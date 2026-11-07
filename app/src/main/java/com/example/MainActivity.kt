package com.example

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.TrakerApp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FinancialViewModel

class MainActivity : FragmentActivity() {
    private var sharedViewModel: FinancialViewModel? = null
    private var isInitialIntentProcessed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val isInitialLight = com.example.ui.theme.ThemePreferences.isLightMode(this)
        val initialBgColor = if (isInitialLight) {
            android.graphics.Color.parseColor("#FCFCFD")
        } else {
            android.graphics.Color.parseColor("#0A1317")
        }
        window.setFormat(android.graphics.PixelFormat.RGBA_8888)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            window.colorMode = android.content.pm.ActivityInfo.COLOR_MODE_DEFAULT
        }
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(initialBgColor))
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.decorView.setBackgroundColor(initialBgColor)
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = isInitialLight
            isAppearanceLightNavigationBars = isInitialLight
        }
        com.example.service.MonthlyReminderManager.checkDueRemindersNow(this)

        val initialAction = intent?.action ?: intent?.getStringExtra("action")

        setContent {
            val viewModel: FinancialViewModel = viewModel()

            SideEffect {
                sharedViewModel = viewModel
            }

            if (!isInitialIntentProcessed) {
                isInitialIntentProcessed = true
                if (initialAction in listOf("ADD_EXPENSE", "ADD_MEAL_LOG", "ADD_HOTEL_TX", "ADD_HOTEL", "ADD_ACCOUNT")) {
                    if (viewModel.quickActionTrigger.value == null) {
                        viewModel.triggerQuickAction(initialAction!!)
                    }
                }
            }

            TrakerApp(viewModel = viewModel)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val action = intent.action ?: intent.getStringExtra("action")
        if (action in listOf("ADD_EXPENSE", "ADD_MEAL_LOG", "ADD_HOTEL_TX", "ADD_HOTEL", "ADD_ACCOUNT")) {
            sharedViewModel?.triggerQuickAction(action!!)
        }
    }
}
