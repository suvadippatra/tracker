package com.example.ui.components

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

@Suppress("DEPRECATION")
@Composable
fun DialogEdgeToEdge(transparentBackground: Boolean = false) {
    val view = LocalView.current
    val isLight = isAppLightMode()

    fun applyEdgeToEdge(win: Window) {
        WindowCompat.setDecorFitsSystemWindows(win, false)

        val insetsController = WindowCompat.getInsetsController(win, win.decorView)
        insetsController.isAppearanceLightStatusBars = isLight
        insetsController.isAppearanceLightNavigationBars = isLight

        val wm = win.context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val metrics = DisplayMetrics()
        wm?.defaultDisplay?.getRealMetrics(metrics)
        val realWidth = if (metrics.widthPixels > 0) metrics.widthPixels else ViewGroup.LayoutParams.MATCH_PARENT
        val realHeight = if (metrics.heightPixels > 0) metrics.heightPixels else ViewGroup.LayoutParams.MATCH_PARENT

        win.clearFlags(
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION or
            WindowManager.LayoutParams.FLAG_DIM_BEHIND
        )

        var windowFlags = WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            // Crucial on Android 8–10 (API 26–29) so secondary Dialog windows extend across status & navigation bars without top/bottom cropping
            windowFlags = windowFlags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        }
        win.addFlags(windowFlags)

        val params = win.attributes
        params.x = 0
        params.y = 0
        params.width = realWidth
        params.height = realHeight
        params.gravity = Gravity.TOP or Gravity.START
        params.horizontalMargin = 0f
        params.verticalMargin = 0f
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        win.attributes = params
        win.setLayout(realWidth, realHeight)

        val bgColor = if (transparentBackground) {
            Color.TRANSPARENT
        } else {
            if (isLight) Color.parseColor("#FCFCFD") else Color.parseColor("#0A1317")
        }
        win.setBackgroundDrawable(ColorDrawable(bgColor))
        win.navigationBarColor = Color.TRANSPARENT
        win.statusBarColor = Color.TRANSPARENT
        win.setDimAmount(0f)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            win.setDecorFitsSystemWindows(false)
        }

        win.decorView.setPadding(0, 0, 0, 0)
        (win.decorView as? ViewGroup)?.clipToPadding = false
        (win.decorView as? ViewGroup)?.clipChildren = false
        win.decorView.fitsSystemWindows = false
        win.decorView.layoutParams = win.decorView.layoutParams?.apply {
            width = realWidth
            height = realHeight
        }

        val contentView = win.decorView.findViewById<View>(android.R.id.content)
        if (contentView != null) {
            contentView.setPadding(0, 0, 0, 0)
            (contentView as? ViewGroup)?.clipToPadding = false
            (contentView as? ViewGroup)?.clipChildren = false
            contentView.fitsSystemWindows = false

            val lp = contentView.layoutParams ?: ViewGroup.LayoutParams(realWidth, realHeight)
            lp.width = realWidth
            lp.height = realHeight
            contentView.layoutParams = lp
        }

        var parent: ViewParent? = view.parent
        while (parent != null) {
            if (parent is View) {
                parent.fitsSystemWindows = false
                parent.setPadding(0, 0, 0, 0)
                (parent as? ViewGroup)?.clipToPadding = false
                (parent as? ViewGroup)?.clipChildren = false
                val plp = parent.layoutParams ?: ViewGroup.LayoutParams(realWidth, realHeight)
                plp.width = realWidth
                plp.height = realHeight
                parent.layoutParams = plp
            }
            parent = parent.parent
        }
    }

    fun findWindow(v: View): Window? {
        var parent: ViewParent? = v.parent
        while (parent != null) {
            if (parent is DialogWindowProvider) {
                return parent.window
            }
            parent = parent.parent
        }
        return (v.parent as? DialogWindowProvider)?.window
    }

    DisposableEffect(view, isLight, transparentBackground) {
        findWindow(view)?.let { applyEdgeToEdge(it) }
        val globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            findWindow(view)?.let { win ->
                val wm = win.context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                val metrics = DisplayMetrics()
                wm?.defaultDisplay?.getRealMetrics(metrics)
                if (metrics.heightPixels > 0 && (win.decorView.height < metrics.heightPixels || win.decorView.paddingTop != 0 || win.decorView.paddingBottom != 0)) {
                    applyEdgeToEdge(win)
                }
            }
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
        val attachListener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                findWindow(v)?.let { applyEdgeToEdge(it) }
            }
            override fun onViewDetachedFromWindow(v: View) {}
        }
        view.addOnAttachStateChangeListener(attachListener)
        onDispose {
            if (view.viewTreeObserver.isAlive) {
                view.viewTreeObserver.removeOnGlobalLayoutListener(globalLayoutListener)
            }
            view.removeOnAttachStateChangeListener(attachListener)
        }
    }

    SideEffect {
        findWindow(view)?.let { applyEdgeToEdge(it) }
    }
}

/**
 * Reliable top status bar + extra margin padding for full-screen Dialogs on Android 8–14+.
 * On Android 8–10 (API 26–29), secondary Dialog windows with FLAG_LAYOUT_NO_LIMITS may report
 * 0dp for WindowInsets.statusBars, causing top headers to appear lifted or cropped. This modifier
 * falls back to the physical status_bar_height dimen resource so headers always have proper top margin.
 */
fun Modifier.dialogSafeTopPadding(extraTop: Dp = 8.dp): Modifier = composed {
    val context = LocalContext.current
    val density = LocalDensity.current
    val resourceStatusBarHeight = remember(context, density) {
        val resId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resId > 0) {
            with(density) { context.resources.getDimensionPixelSize(resId).toDp() }
        } else {
            24.dp
        }
    }
    val insetTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val effectiveTop = maxOf(insetTop, resourceStatusBarHeight) + extraTop
    this.padding(top = effectiveTop)
}

/**
 * Reliable bottom navigation bar padding for full-screen Dialogs on Android 8–14+.
 */
fun Modifier.dialogSafeBottomPadding(extraBottom: Dp = 0.dp): Modifier = composed {
    val context = LocalContext.current
    val density = LocalDensity.current
    val resourceNavBarHeight = remember(context, density) {
        val resId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        val hasNavId = context.resources.getIdentifier("config_showNavigationBar", "bool", "android")
        val hasNav = if (hasNavId > 0) context.resources.getBoolean(hasNavId) else true
        if (resId > 0 && hasNav) {
            with(density) { context.resources.getDimensionPixelSize(resId).toDp() }
        } else {
            0.dp
        }
    }
    val insetBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val effectiveBottom = maxOf(insetBottom, resourceNavBarHeight) + extraBottom
    this.padding(bottom = effectiveBottom)
}

fun Modifier.dialogSystemBarsPadding(extraTop: Dp = 8.dp, extraBottom: Dp = 0.dp): Modifier =
    this.dialogSafeTopPadding(extraTop = extraTop).dialogSafeBottomPadding(extraBottom = extraBottom)

