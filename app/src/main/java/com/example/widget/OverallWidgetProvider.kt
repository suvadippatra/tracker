package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class OverallWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val scope = CoroutineScope(Dispatchers.IO)
        
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_overall)
            views.setOnClickPendingIntent(R.id.overall_net, pendingIntent)

            scope.launch {
                val db = AppDatabase.getDatabase(context)
                val user = db.userProfileDao().getUserProfileSync()
                val hotelId = user?.defaultHotelId ?: 1
                
                val cal = Calendar.getInstance()
                // Set start/end of month
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis
                
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                val end = cal.timeInMillis

                val txs = db.transactionDao().getTransactionsForHotelAndDateRange(hotelId, start, end)
                val totalHotel = txs.sumOf { it.totalAmount }

                val expenses = db.extraExpenseDao().getExpensesForDateRange(start, end)
                val totalExpenses = expenses.sumOf { it.amount }
                
                val totalNet = totalHotel + totalExpenses

                withContext(Dispatchers.Main) {
                    views.setTextViewText(R.id.overall_net, "Net: ₹ $totalNet")
                    views.setTextViewText(R.id.overall_breakdown, "Hotel: ₹ $totalHotel\nExpenses: ₹ $totalExpenses")
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
