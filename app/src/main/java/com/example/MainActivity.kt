package com.example

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.TrakerApp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FinancialViewModel

class MainActivity : FragmentActivity() {
    private var pendingQuickAction by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            MyApplicationTheme {
                val viewModel: FinancialViewModel = viewModel()
                LaunchedEffect(pendingQuickAction) {
                    pendingQuickAction?.let { action ->
                        viewModel.triggerQuickAction(action)
                        pendingQuickAction = null
                    }
                }
                TrakerApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val action = intent?.action ?: intent?.getStringExtra("action")
        if (action == "ADD_EXPENSE" || action == "ADD_MEAL_LOG" || action == "ADD_HOTEL_TX" || action == "ADD_HOTEL" || action == "ADD_ACCOUNT") {
            pendingQuickAction = action
        }
    }
}
