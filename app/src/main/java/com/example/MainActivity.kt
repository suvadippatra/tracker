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
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val initialAction = intent?.action ?: intent?.getStringExtra("action")
        
        setContent {
            MyApplicationTheme {
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
