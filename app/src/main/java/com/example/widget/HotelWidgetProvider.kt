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

class HotelWidgetProvider : AppWidgetProvider() {

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
                val allHotels = db.hotelDao().getAllHotelsSync()
                val targetHotelId = user?.defaultHotelId ?: allHotels.firstOrNull()?.id ?: 1
                val hotel = db.hotelDao().getHotelByIdSync(targetHotelId) ?: allHotels.firstOrNull()
                val menus = if (hotel != null) db.menuDao().getMenusForHotelSync(hotel.id) else emptyList()

                val isConfigured = allHotels.isNotEmpty() && hotel != null && menus.isNotEmpty()
                val targetAction = if (isConfigured) "ADD_MEAL_LOG" else "ADD_HOTEL"
                val buttonLabel = if (isConfigured) "+ Add Meal" else "+ Add Hotel"

                val actionIntent = Intent(context, MainActivity::class.java).apply {
                    action = targetAction
                    putExtra("action", targetAction)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingActionIntent = PendingIntent.getActivity(
                    context,
                    1,
                    actionIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val hotelName = if (isConfigured) (hotel?.name ?: "Hotel") else "No Hotel Set"
                
                val now = Calendar.getInstance()
                val currentMonth = now.get(Calendar.MONTH)
                val currentYear = now.get(Calendar.YEAR)

                val allTxs = if (hotel != null) db.transactionDao().getTransactionsForHotelSync(hotel.id) else emptyList()
                val monthTxs = allTxs.filter { tx ->
                    val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                    txCal.get(Calendar.MONTH) == currentMonth && txCal.get(Calendar.YEAR) == currentYear
                }
                val total = monthTxs.sumOf { it.totalAmount }
                
                withContext(Dispatchers.Main) {
                    for (appWidgetId in appWidgetIds) {
                        val views = RemoteViews(context.packageName, R.layout.widget_hotel)
                        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                        views.setOnClickPendingIntent(R.id.btn_add_meal, pendingActionIntent)
                        views.setTextViewText(R.id.btn_add_meal, buttonLabel)
                        views.setTextViewText(R.id.hotel_name, "🏨 $hotelName")
                        views.setTextViewText(R.id.hotel_total, "₹ $total")
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            } catch (e: Exception) {
                Log.e("HotelWidget", "Error updating widget", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
