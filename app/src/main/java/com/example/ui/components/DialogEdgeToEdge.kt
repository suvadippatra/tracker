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
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

@Composable
fun DialogEdgeToEdge() {
    val view = LocalView.current
    val isLight = isAppLightMode()

    fun applyEdgeToEdge(win: Window) {
        WindowCompat.setDecorFitsSystemWindows(win, false)
        
        val insetsController = WindowCompat.getInsetsController(win, win.decorView)
        insetsController.isAppearanceLightStatusBars = isLight
        insetsController.isAppearanceLightNavigationBars = isLight

        val params = win.attributes
        params.x = 0
        params.y = 0
        params.width = ViewGroup.LayoutParams.MATCH_PARENT
        params.height = ViewGroup.LayoutParams.MATCH_PARENT
        params.gravity = Gravity.FILL
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        win.attributes = params
        win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        win.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        win.navigationBarColor = Color.TRANSPARENT
        win.statusBarColor = Color.TRANSPARENT
        
        win.decorView.setPadding(0, 0, 0, 0)
        (win.decorView as? ViewGroup)?.clipToPadding = false
        win.decorView.fitsSystemWindows = false

        val contentView = win.decorView.findViewById<View>(android.R.id.content)
        if (contentView != null) {
            contentView.setPadding(0, 0, 0, 0)
            (contentView as? ViewGroup)?.clipToPadding = false
            contentView.fitsSystemWindows = false
            
            val lp = contentView.layoutParams ?: ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT
            contentView.layoutParams = lp
        }

        win.addFlags(
            WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        win.clearFlags(
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION or
            WindowManager.LayoutParams.FLAG_DIM_BEHIND
        )
        win.setDimAmount(0f)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            win.setDecorFitsSystemWindows(false)
        }
        
        // Traverse upwards from Compose view to force MATCH_PARENT and remove padding
        var parent: ViewParent? = view.parent
        while (parent != null) {
            if (parent is View) {
                parent.fitsSystemWindows = false
                parent.setPadding(0, 0, 0, 0)
                val plp = parent.layoutParams ?: ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                plp.width = ViewGroup.LayoutParams.MATCH_PARENT
                plp.height = ViewGroup.LayoutParams.MATCH_PARENT
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

    DisposableEffect(view, isLight) {
        findWindow(view)?.let { applyEdgeToEdge(it) }
        val attachListener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                findWindow(v)?.let { applyEdgeToEdge(it) }
            }
            override fun onViewDetachedFromWindow(v: View) {}
        }
        view.addOnAttachStateChangeListener(attachListener)
        onDispose {
            view.removeOnAttachStateChangeListener(attachListener)
        }
    }

    SideEffect {
        findWindow(view)?.let { applyEdgeToEdge(it) }
    }
}
