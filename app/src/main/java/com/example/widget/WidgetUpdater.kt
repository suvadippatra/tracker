package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

object WidgetUpdater {
    fun updateAllWidgets(context: Context) {
        val providers = listOf(
            HotelWidgetProvider::class.java,
            ExpensesWidgetProvider::class.java,
            OverallWidgetProvider::class.java,
            DailyMonthlySpendWidgetProvider::class.java
        )

        for (provider in providers) {
            val intent = Intent(context, provider)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                ComponentName(context, provider)
            )
            if (ids.isNotEmpty()) {
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                context.sendBroadcast(intent)
            }
        }
    }
}
