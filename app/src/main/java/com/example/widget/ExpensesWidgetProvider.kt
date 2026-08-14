package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class ExpensesWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                val db = AppDatabase.getDatabase(context.applicationContext)
                val user = db.userProfileDao().getUserProfileSync()
                val isBengali = user?.language == "bn"
                val accounts = db.bankAccountDao().getAllBankAccountsSync()
                val activeAccounts = accounts.filter { !it.isDeleted }
                val totalBalance = activeAccounts.sumOf { it.balance }

                val isConfigured = activeAccounts.isNotEmpty()
                val targetAction = if (isConfigured) "ADD_EXPENSE" else "ADD_ACCOUNT"
                val buttonLabel = if (isConfigured) {
                    if (isBengali) "+ খরচ যোগ" else "+ Expense"
                } else {
                    if (isBengali) "+ অ্যাকাউন্ট যোগ" else "+ Add Account"
                }

                val actionIntent = Intent(context, MainActivity::class.java).apply {
                    action = targetAction
                    putExtra("action", targetAction)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingActionIntent = PendingIntent.getActivity(
                    context,
                    2,
                    actionIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val accountLabel = when (activeAccounts.size) {
                    0 -> if (isBengali) "কোনো অ্যাকাউন্ট নেই" else "No Accounts"
                    1 -> activeAccounts[0].name
                    else -> if (isBengali) "অ্যাকাউন্টসমূহ (${activeAccounts.size})" else "Accounts (${activeAccounts.size})"
                }

                val subtitleLabel = if (isConfigured) {
                    if (isBengali) "সর্বমোট ওয়ালেট ব্যালেন্স" else "Total Wallet Balance"
                } else {
                    if (isBengali) "কনফিগার করতে এখানে চাপুন" else "Tap to configure"
                }
                
                withContext(Dispatchers.Main) {
                    for (appWidgetId in appWidgetIds) {
                        val views = RemoteViews(context.packageName, R.layout.widget_expenses)
                        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                        
                        views.setTextViewText(R.id.expenses_title, "💳 $accountLabel")
                        
                        if (isConfigured) {
                            views.setViewVisibility(R.id.btn_add_expense, android.view.View.VISIBLE)
                            views.setViewVisibility(R.id.expenses_balance, android.view.View.VISIBLE)
                            views.setOnClickPendingIntent(R.id.btn_add_expense, pendingActionIntent)
                            views.setTextViewText(R.id.btn_add_expense, buttonLabel)
                            views.setTextViewText(R.id.expenses_balance, "₹ $totalBalance")
                        } else {
                            views.setViewVisibility(R.id.btn_add_expense, android.view.View.GONE)
                            views.setViewVisibility(R.id.expenses_balance, android.view.View.GONE)
                        }
                        
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            } catch (e: Exception) {
                Log.e("ExpensesWidget", "Error updating widget", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
