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
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class DailyMonthlySpendWidgetProvider : AppWidgetProvider() {

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
        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                val db = AppDatabase.getDatabase(context.applicationContext)
                val user = db.userProfileDao().getUserProfileSync()
                val isBengali = user?.language == "bn"
                val allAccounts = db.bankAccountDao().getAllBankAccountsSync().filter { !it.isDeleted }
                val targetAccount = allAccounts.find { it.id == user?.defaultBankAccountId } ?: allAccounts.firstOrNull()

                val isConfigured = targetAccount != null

                // Compute Today and Month Boundaries
                val now = Calendar.getInstance()
                val todayStart = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val monthStart = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val allExpenses = db.extraExpenseDao().getAllExpensesSync().filter { !it.isDeleted }
                val accountExpenses = if (targetAccount != null) {
                    allExpenses.filter { it.bankAccountId == targetAccount.id || it.bankAccountId == 0 }
                } else {
                    allExpenses
                }

                val dailySpent = accountExpenses.filter { it.timestamp >= todayStart }.sumOf { it.amount }
                val monthlySpent = accountExpenses.filter { it.timestamp >= monthStart }.sumOf { it.amount }

                val targetAction = if (isConfigured) "ADD_EXPENSE" else "ADD_ACCOUNT"
                val buttonLabel = if (isConfigured) {
                    if (isBengali) "+ খরচ যোগ" else "+ Add Expense"
                } else {
                    if (isBengali) "+ অ্যাকাউন্ট যোগ" else "+ Add Account"
                }

                val accountLabel = if (isConfigured) {
                    "💳 ${targetAccount?.name}"
                } else {
                    if (isBengali) "💳 কোনো অ্যাকাউন্ট নেই" else "💳 No Account"
                }

                val balanceLabel = if (isConfigured) {
                    if (isBengali) "ব্যালেন্স: ₹ ${targetAccount?.balance?.toInt()}" else "Balance: ₹ ${targetAccount?.balance?.toInt()}"
                } else {
                    if (isBengali) "কনফিগার করতে ট্যাপ করুন" else "Tap to configure"
                }

                val rootIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingRootIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId * 10 + 1,
                    rootIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val actionIntent = Intent(context, MainActivity::class.java).apply {
                    action = targetAction
                    putExtra("action", targetAction)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingActionIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId * 10 + 2,
                    actionIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                fun populateViews(layoutRes: Int): RemoteViews {
                    val views = RemoteViews(context.packageName, layoutRes)
                    views.setOnClickPendingIntent(R.id.widget_root, pendingRootIntent)

                    views.setTextViewText(R.id.spend_account_name, accountLabel)
                    views.setTextViewText(
                        R.id.spend_today_label,
                        if (isBengali) "আজ" else "Today"
                    )
                    views.setTextViewText(
                        R.id.spend_month_label,
                        if (isBengali) "এই মাস" else "This Month"
                    )
                    views.setTextViewText(R.id.spend_today_amount, "₹ ${dailySpent.toInt()}")
                    views.setTextViewText(R.id.spend_month_amount, "₹ ${monthlySpent.toInt()}")
                    views.setTextViewText(R.id.spend_balance, balanceLabel)

                    views.setOnClickPendingIntent(R.id.btn_add_spend_expense, pendingActionIntent)
                    views.setTextViewText(R.id.btn_add_spend_expense, buttonLabel)
                    return views
                }

                withContext(Dispatchers.Main) {
                    val compactViews = populateViews(R.layout.widget_spend_compact)
                    val expandedViews = populateViews(R.layout.widget_spend)

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
                Log.e("SpendWidget", "Error updating spend widget", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

