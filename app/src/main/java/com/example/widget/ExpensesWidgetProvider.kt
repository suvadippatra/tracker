package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.SizeF
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExpensesWidgetProvider : AppWidgetProvider() {

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        renderSingleWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            renderSingleWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun renderSingleWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
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

                fun populateViews(layoutRes: Int): RemoteViews {
                    val views = RemoteViews(context.packageName, layoutRes)
                    views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                    views.setTextViewText(R.id.expenses_title, "💳 $accountLabel")

                    if (isConfigured) {
                        views.setViewVisibility(R.id.btn_add_expense, android.view.View.VISIBLE)
                        views.setViewVisibility(R.id.expenses_balance, android.view.View.VISIBLE)
                        views.setOnClickPendingIntent(R.id.btn_add_expense, pendingActionIntent)
                        views.setTextViewText(R.id.btn_add_expense, buttonLabel)
                        views.setTextViewText(R.id.expenses_balance, "₹ $totalBalance")
                    } else {
                        views.setViewVisibility(R.id.btn_add_expense, android.view.View.VISIBLE)
                        views.setOnClickPendingIntent(R.id.btn_add_expense, pendingActionIntent)
                        views.setTextViewText(R.id.btn_add_expense, buttonLabel)
                        views.setViewVisibility(R.id.expenses_balance, android.view.View.VISIBLE)
                        views.setTextViewText(R.id.expenses_balance, subtitleLabel)
                        views.setTextColor(R.id.expenses_balance, android.graphics.Color.parseColor("#80D8FF"))
                    }
                    return views
                }

                withContext(Dispatchers.Main) {
                    val compactViews = populateViews(R.layout.widget_expenses_compact)
                    val expandedViews = populateViews(R.layout.widget_expenses)

                    val finalViews = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val viewMapping = mapOf(
                            SizeF(100f, 40f) to compactViews,
                            SizeF(100f, 120f) to expandedViews
                        )
                        RemoteViews(viewMapping)
                    } else {
                        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
                        if (minHeight in 1..119) compactViews else expandedViews
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, finalViews)
                }
            } catch (e: Exception) {
                Log.e("ExpensesWidget", "Error updating widget", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

