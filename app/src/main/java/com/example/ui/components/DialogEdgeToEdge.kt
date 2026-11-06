package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import kotlin.math.ceil

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Forces a Compose Dialog to span 100% of the physical screen (including over the status bar
 * and navigation bar) on all Android versions from Android 8.0 (API 26) through Android 15+.
 *
 * Root cause on Android 8–10:
 * 1. Compose's internal `DialogLayout.internalOnMeasure` checks `usePlatformDefaultWidth`. When false,
 *    it clamps its child `AndroidComposeView` height to `configuration.screenHeightDp * density`, which
 *    on Android 8 excludes the status bar (24dp) and navigation bar (48dp) = 72dp shorter than physical
 *    screen height, leaving a 72dp gap at the bottom where the underlying screen leaks through.
 * 2. Android 8's `PhoneWindow` content container (`FitWindowsLinearLayout` / `android.R.id.content`)
 *    consumes window insets as top/bottom padding unless explicitly blocked.
 */
@Composable
fun DialogEdgeToEdge() {
    val view = LocalView.current
    val context = LocalContext.current
    val isLight = isAppLightMode()

    DisposableEffect(view, isLight) {
        val dialogWindowProvider = view.parent as? DialogWindowProvider
        val window = dialogWindowProvider?.window

        if (window != null) {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            fun getTargetDimensions(): Pair<Int, Int> {
                val activity = context.findActivity()
                val actDecor = activity?.window?.decorView
                val metrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                wm.defaultDisplay.getRealMetrics(metrics)

                if (actDecor != null && actDecor.width > 0 && actDecor.height > 0) {
                    val isMultiWindow = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity.isInMultiWindowMode
                    val isLetterboxedOrConstrained = isMultiWindow ||
                        kotlin.math.abs(actDecor.width - metrics.widthPixels) > 4 ||
                        kotlin.math.abs(actDecor.height - metrics.heightPixels) > 180
                    return if (isLetterboxedOrConstrained) {
                        actDecor.width to actDecor.height
                    } else {
                        maxOf(actDecor.width, metrics.widthPixels) to maxOf(actDecor.height, metrics.heightPixels)
                    }
                }
                return metrics.widthPixels to metrics.heightPixels
            }

            // Also flip DialogLayout's internal usePlatformDefaultWidth field to true via reflection
            // after we set exact targetWidth x targetHeight LayoutParams, so DialogLayout delegates
            // directly to super.internalOnMeasure(EXACTLY targetWidth, EXACTLY targetHeight).
            val dialogLayoutView = view.parent as? View
            val usePlatformDefaultWidthField = try {
                dialogLayoutView?.javaClass?.getDeclaredField("usePlatformDefaultWidth")?.apply {
                    isAccessible = true
                }
            } catch (_: Exception) {
                null
            }

            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.setBackgroundDrawableResource(android.R.color.transparent)
            window.decorView.setBackgroundColor(android.graphics.Color.TRANSPARENT)

            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR)

            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = android.graphics.Color.TRANSPARENT

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
                window.isStatusBarContrastEnforced = false
            }

            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.isAppearanceLightStatusBars = isLight
            insetsController.isAppearanceLightNavigationBars = isLight

            fun enforceFullScreenHierarchy() {
                val (targetWidth, targetHeight) = getTargetDimensions()
                if (targetWidth <= 0 || targetHeight <= 0) return

                val density = context.resources.displayMetrics.density.coerceAtLeast(1f)
                val fullWidthDp = ceil(targetWidth / density).toInt()
                val fullHeightDp = ceil(targetHeight / density).toInt()
                val targetFontScale = com.example.ui.theme.ThemePreferences.getFontScale(context)

                try {
                    view.context.resources.configuration.screenWidthDp = fullWidthDp
                    view.context.resources.configuration.screenHeightDp = fullHeightDp
                    view.context.resources.configuration.fontScale = targetFontScale
                    (view.parent as? View)?.context?.resources?.configuration?.let { cfg ->
                        cfg.screenWidthDp = fullWidthDp
                        cfg.screenHeightDp = fullHeightDp
                        cfg.fontScale = targetFontScale
                    }
                    window.context.resources.configuration.screenWidthDp = fullWidthDp
                    window.context.resources.configuration.screenHeightDp = fullHeightDp
                    window.context.resources.configuration.fontScale = targetFontScale
                    usePlatformDefaultWidthField?.setBoolean(dialogLayoutView, true)
                } catch (_: Exception) {}

                val winParams = window.attributes
                if (winParams.width != targetWidth || winParams.height != targetHeight ||
                    winParams.gravity != (Gravity.TOP or Gravity.START) || winParams.dimAmount != 0f
                ) {
                    winParams.width = targetWidth
                    winParams.height = targetHeight
                    winParams.gravity = Gravity.TOP or Gravity.START
                    winParams.x = 0
                    winParams.y = 0
                    winParams.dimAmount = 0f
                    winParams.horizontalMargin = 0f
                    winParams.verticalMargin = 0f
                    window.attributes = winParams
                    window.setLayout(targetWidth, targetHeight)
                }

                var curr: View? = view
                while (curr != null) {
                    if (curr !== view) {
                        if (curr.paddingLeft != 0 || curr.paddingTop != 0 ||
                            curr.paddingRight != 0 || curr.paddingBottom != 0
                        ) {
                            curr.setPadding(0, 0, 0, 0)
                        }
                        curr.fitsSystemWindows = false
                        curr.background = null
                        curr.outlineProvider = null
                        curr.clipToOutline = false
                        if (curr is ViewGroup) {
                            curr.clipChildren = false
                            curr.clipToPadding = false
                        }
                    }
                    val lp = curr.layoutParams
                    if (lp != null) {
                        var changed = false
                        if (lp.width != targetWidth) {
                            lp.width = targetWidth
                            changed = true
                        }
                        if (lp.height != targetHeight) {
                            lp.height = targetHeight
                            changed = true
                        }
                        if (lp is ViewGroup.MarginLayoutParams) {
                            if (lp.leftMargin != 0 || lp.topMargin != 0 ||
                                lp.rightMargin != 0 || lp.bottomMargin != 0
                            ) {
                                lp.setMargins(0, 0, 0, 0)
                                changed = true
                            }
                        }
                        if (changed) {
                            curr.layoutParams = lp
                        }
                    }
                    curr = curr.parent as? View
                }
            }

            // Block ancestor views (like Android 8's FitWindowsLinearLayout / android.R.id.content)
            // from consuming window insets as padding, while letting the Compose view receive them.
            var ancestor: View? = view.parent as? View
            while (ancestor != null) {
                ViewCompat.setOnApplyWindowInsetsListener(ancestor) { v, insets ->
                    if (v.paddingTop != 0 || v.paddingBottom != 0 || v.paddingLeft != 0 || v.paddingRight != 0) {
                        v.setPadding(0, 0, 0, 0)
                    }
                    insets
                }
                ancestor = ancestor.parent as? View
            }

            enforceFullScreenHierarchy()

            val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
                val (tW, tH) = getTargetDimensions()
                val decor = window.decorView
                val parentView = view.parent as? View
                val needsFix = decor.width != tW ||
                    decor.height != tH ||
                    decor.paddingBottom != 0 ||
                    decor.paddingTop != 0 ||
                    (parentView != null && (parentView.paddingBottom != 0 || parentView.paddingTop != 0))
                if (needsFix) {
                    enforceFullScreenHierarchy()
                }
            }
            view.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)

            onDispose {
                if (view.viewTreeObserver.isAlive) {
                    view.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
                }
            }
        } else {
            onDispose { }
        }
    }
}

/**
 * Reliable top & bottom system bar padding for full-screen Dialogs across Android 8–15.
 * On Android 8–10, secondary Dialog windows with FLAG_LAYOUT_NO_LIMITS often receive 0dp
 * from Compose's WindowInsets.systemBars inside the Dialog window, causing headers to overlap
 * the status bar and bottom buttons to sit behind the 3-button navigation bar.
 * This modifier reads the Activity window's real system bar insets (with safe fallbacks).
 */
private fun computeSystemNavBarBottomPx(context: Context): Int {
    val res = context.resources
    val activity = context.findActivity()
    val actDecor = activity?.window?.decorView
    val rootInsets = if (actDecor != null) ViewCompat.getRootWindowInsets(actDecor) else null
    var navBottomPx = rootInsets?.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
    if (navBottomPx <= 0) {
        try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val realMetrics = DisplayMetrics()
            val usableMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(realMetrics)
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getMetrics(usableMetrics)
            val navId = res.getIdentifier("navigation_bar_height", "dimen", "android")
            val navResPx = if (navId > 0) res.getDimensionPixelSize(navId) else 0
            val isMultiWindow = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity?.isInMultiWindowMode == true

            if (isMultiWindow && actDecor != null && actDecor.height > 0) {
                val loc = IntArray(2)
                actDecor.getLocationOnScreen(loc)
                val windowBottomY = loc[1] + actDecor.height
                val systemNavTopY = realMetrics.heightPixels - navResPx
                // If this split-screen pane sits at the bottom of the physical screen or overlaps the system nav bar
                if (navResPx > 0 && windowBottomY > systemNavTopY) {
                    navBottomPx = (windowBottomY - systemNavTopY).coerceIn(0, navResPx)
                } else if (loc[1] > 40 && navResPx > 0 && windowBottomY >= usableMetrics.heightPixels - 4) {
                    // Bottom split-screen pane on Android 8 where decorView reaches usable bottom but system nav bar overlays
                    navBottomPx = navResPx
                }
            } else {
                val isLetterboxed = actDecor != null && actDecor.width > 0 &&
                    (kotlin.math.abs(actDecor.width - realMetrics.widthPixels) > 4 ||
                     kotlin.math.abs(actDecor.height - realMetrics.heightPixels) > 180)
                if (!isLetterboxed) {
                    val diffH = realMetrics.heightPixels - usableMetrics.heightPixels
                    if (diffH in 1..220) {
                        val fallbackNav = if (navResPx > 0) navResPx else diffH
                        navBottomPx = minOf(diffH, fallbackNav.coerceAtLeast(diffH / 2))
                    }
                }
            }
        } catch (_: Exception) {}
    }
    return navBottomPx
}

@Composable
fun rememberSplitScreenSafeBottomPadding(): Dp {
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val composeInsets = WindowInsets.systemBars.asPaddingValues()
    val fallbackBottomDp = androidx.compose.runtime.remember(
        context,
        density,
        configuration.screenHeightDp,
        configuration.screenWidthDp,
        configuration.orientation
    ) {
        val navPx = computeSystemNavBarBottomPx(context)
        with(density) {
            if (navPx > 0) navPx.toDp() else 0.dp
        }
    }
    return maxOf(composeInsets.calculateBottomPadding(), fallbackBottomDp)
}

fun Modifier.dialogSystemBarsPadding(
    extraTop: Dp = 0.dp,
    extraBottom: Dp = 0.dp
): Modifier = composed {
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val composeInsets = WindowInsets.systemBars.asPaddingValues()

    val (fallbackTopDp, fallbackBottomDp) = androidx.compose.runtime.remember(
        context,
        density,
        configuration.screenHeightDp,
        configuration.screenWidthDp,
        configuration.orientation
    ) {
        val res = context.resources
        val statusId = res.getIdentifier("status_bar_height", "dimen", "android")
        val statusPx = if (statusId > 0) res.getDimensionPixelSize(statusId) else 0
        val topDp = with(density) {
            if (statusPx > 0) statusPx.toDp() else 24.dp
        }
        val navBottomPx = computeSystemNavBarBottomPx(context)
        val bottomDp = with(density) {
            if (navBottomPx > 0) navBottomPx.toDp() else 0.dp
        }
        topDp to bottomDp
    }

    val topPadding = maxOf(composeInsets.calculateTopPadding(), fallbackTopDp) + extraTop
    val bottomPadding = maxOf(composeInsets.calculateBottomPadding(), fallbackBottomDp) + extraBottom

    this.padding(
        PaddingValues(
            top = topPadding,
            bottom = bottomPadding
        )
    )
}

fun Modifier.dialogSafeTopPadding(extraTop: Dp = 0.dp): Modifier = composed {
    val context = LocalContext.current
    val density = LocalDensity.current
    val composeInsets = WindowInsets.systemBars.asPaddingValues()

    val fallbackTopDp = androidx.compose.runtime.remember(context, density) {
        val res = context.resources
        val statusId = res.getIdentifier("status_bar_height", "dimen", "android")
        val statusPx = if (statusId > 0) res.getDimensionPixelSize(statusId) else 0
        with(density) {
            if (statusPx > 0) statusPx.toDp() else 24.dp
        }
    }

    val topPadding = maxOf(composeInsets.calculateTopPadding(), fallbackTopDp) + extraTop
    this.padding(top = topPadding)
}

fun Modifier.dialogSafeBottomPadding(extraBottom: Dp = 0.dp): Modifier = composed {
    val bottomPadding = rememberSplitScreenSafeBottomPadding() + extraBottom
    this.padding(bottom = bottomPadding)
}

