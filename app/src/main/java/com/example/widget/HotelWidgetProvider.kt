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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HotelWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_PREV_MONTH = "com.example.widget.HOTEL_PREV_MONTH"
        const val ACTION_NEXT_MONTH = "com.example.widget.HOTEL_NEXT_MONTH"
        private const val PREFS_NAME = "hotel_widget_prefs"
        private const val KEY_OFFSET_PREFIX = "month_offset_"

        fun getMonthOffset(context: Context, appWidgetId: Int): Int {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_OFFSET_PREFIX + appWidgetId, 0)
        }

        fun setMonthOffset(context: Context, appWidgetId: Int, offset: Int) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_OFFSET_PREFIX + appWidgetId, offset)
                .apply()
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_PREV_MONTH -> {
                val appWidgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    changeMonthOffset(context, appWidgetId, -1)
                }
            }
            ACTION_NEXT_MONTH -> {
                val appWidgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    changeMonthOffset(context, appWidgetId, 1)
                }
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val offset = getMonthOffset(context, appWidgetId)
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                renderWidget(context, appWidgetManager, appWidgetId, offset)
            } catch (e: Exception) {
                Log.e("HotelWidget", "Error updating widget on options changed", e)
            }
        }
    }

    private fun changeMonthOffset(context: Context, appWidgetId: Int, delta: Int) {
        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                val db = AppDatabase.getDatabase(context.applicationContext)
                val user = db.userProfileDao().getUserProfileSync()
                val allHotels = db.hotelDao().getAllHotelsSync()
                val targetHotelId = user?.defaultHotelId ?: allHotels.firstOrNull()?.id ?: 1
                val hotel = db.hotelDao().getHotelByIdSync(targetHotelId) ?: allHotels.firstOrNull()
                val allTxs = if (hotel != null) db.transactionDao().getTransactionsForHotelSync(hotel.id) else emptyList()

                val earliestTs = allTxs.minOfOrNull { it.timestamp } ?: System.currentTimeMillis()
                val eCal = Calendar.getInstance().apply { timeInMillis = earliestTs }
                val nowCal = Calendar.getInstance()
                val diffYears = eCal.get(Calendar.YEAR) - nowCal.get(Calendar.YEAR)
                val diffMonths = eCal.get(Calendar.MONTH) - nowCal.get(Calendar.MONTH)
                val minOffset = (diffYears * 12 + diffMonths).coerceAtMost(0)

                val currentOffset = getMonthOffset(context, appWidgetId)
                val newOffset = (currentOffset + delta).coerceIn(minOffset, 0)

                setMonthOffset(context, appWidgetId, newOffset)

                val appWidgetManager = AppWidgetManager.getInstance(context)
                renderWidget(context, appWidgetManager, appWidgetId, newOffset)
            } catch (e: Exception) {
                Log.e("HotelWidget", "Error changing month offset", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                for (appWidgetId in appWidgetIds) {
                    val offset = getMonthOffset(context, appWidgetId)
                    renderWidget(context, appWidgetManager, appWidgetId, offset)
                }
            } catch (e: Exception) {
                Log.e("HotelWidget", "Error updating widget", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun renderWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        monthOffset: Int
    ) {
        val db = AppDatabase.getDatabase(context.applicationContext)
        val user = db.userProfileDao().getUserProfileSync()
        val allHotels = db.hotelDao().getAllHotelsSync()
        val targetHotelId = user?.defaultHotelId ?: allHotels.firstOrNull()?.id ?: 1
        val hotel = db.hotelDao().getHotelByIdSync(targetHotelId) ?: allHotels.firstOrNull()
        val menus = if (hotel != null) db.menuDao().getMenusForHotelSync(hotel.id) else emptyList()

        val isConfigured = allHotels.isNotEmpty() && hotel != null && menus.isNotEmpty()
        val isBengali = user?.language == "bn"

        val allTxs = if (hotel != null) db.transactionDao().getTransactionsForHotelSync(hotel.id) else emptyList()
        val earliestTs = allTxs.minOfOrNull { it.timestamp } ?: System.currentTimeMillis()
        val eCal = Calendar.getInstance().apply { timeInMillis = earliestTs }
        val nowCal = Calendar.getInstance()
        val diffYears = eCal.get(Calendar.YEAR) - nowCal.get(Calendar.YEAR)
        val diffMonths = eCal.get(Calendar.MONTH) - nowCal.get(Calendar.MONTH)
        val minOffset = (diffYears * 12 + diffMonths).coerceAtMost(0)

        // Clamp offset within boundaries
        val effectiveOffset = monthOffset.coerceIn(minOffset, 0)
        if (effectiveOffset != monthOffset) {
            setMonthOffset(context, appWidgetId, effectiveOffset)
        }

        val targetCal = Calendar.getInstance().apply { add(Calendar.MONTH, effectiveOffset) }
        val targetMonth = targetCal.get(Calendar.MONTH)
        val targetYear = targetCal.get(Calendar.YEAR)

        val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val monthLabel = monthFormat.format(targetCal.time)

        val monthTxs = allTxs.filter { tx ->
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            txCal.get(Calendar.MONTH) == targetMonth && txCal.get(Calendar.YEAR) == targetYear
        }
        val total = monthTxs.sumOf { it.totalAmount }

        val allPayments = db.hotelPaymentDao().getAllPaymentsSync()
        val payment = if (hotel != null) {
            allPayments.find { it.hotelId == hotel.id && it.year == targetYear && it.month == targetMonth && !it.isDeleted }
        } else null

        val isPaid = payment != null && payment.amountPaid > 0

        val hotelName = if (isConfigured) (hotel?.name ?: "Hotel") else {
            if (isBengali) "হোটেল সেট নেই" else "No Hotel Set"
        }

        val rootIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingRootIntent = PendingIntent.getActivity(
            context,
            0,
            rootIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Bottom Action Button
        val targetAction = if (isConfigured) {
            if (effectiveOffset == 0) "ADD_MEAL_LOG" else "OPEN_HISTORY"
        } else "ADD_HOTEL"

        val buttonLabel = if (isConfigured) {
            if (effectiveOffset == 0) {
                if (isBengali) "+ খাবার যোগ" else "+ Add Meal"
            } else {
                if (isBengali) "লেজার দেখুন" else "View Ledger"
            }
        } else {
            if (isBengali) "+ হোটেল যোগ" else "+ Add Hotel"
        }

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

        // Navigation Intents
        val prevIntent = Intent(context, HotelWidgetProvider::class.java).apply {
            action = ACTION_PREV_MONTH
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val pendingPrevIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId * 100 + 1,
            prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val nextIntent = Intent(context, HotelWidgetProvider::class.java).apply {
            action = ACTION_NEXT_MONTH
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val pendingNextIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId * 100 + 2,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val canGoPrev = effectiveOffset > minOffset
        val canGoNext = effectiveOffset < 0

        fun populateViews(layoutRes: Int): RemoteViews {
            val views = RemoteViews(context.packageName, layoutRes)
            views.setOnClickPendingIntent(R.id.widget_root, pendingRootIntent)

            views.setTextViewText(R.id.hotel_name, "🏨 $hotelName")
            views.setTextViewText(R.id.hotel_month, monthLabel)

            views.setOnClickPendingIntent(R.id.btn_prev_month, pendingPrevIntent)
            views.setOnClickPendingIntent(R.id.btn_next_month, pendingNextIntent)

            views.setTextColor(
                R.id.btn_prev_month,
                if (canGoPrev) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#55FFFFFF")
            )
            views.setTextColor(
                R.id.btn_next_month,
                if (canGoNext) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#55FFFFFF")
            )

            if (isConfigured) {
                views.setViewVisibility(R.id.btn_add_meal, View.VISIBLE)
                views.setViewVisibility(R.id.hotel_total, View.VISIBLE)
                views.setViewVisibility(R.id.hotel_status, View.VISIBLE)

                views.setOnClickPendingIntent(R.id.btn_add_meal, pendingActionIntent)
                views.setTextViewText(R.id.btn_add_meal, buttonLabel)
                views.setTextViewText(R.id.hotel_total, "₹ ${total.toInt()}")

                if (isPaid) {
                    val payDateStr = payment?.let {
                        SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(it.paymentDate))
                    } ?: ""
                    views.setTextViewText(
                        R.id.hotel_status,
                        if (isBengali) "• পরিশোধিত ($payDateStr)" else "• Paid ($payDateStr)"
                    )
                    views.setTextColor(R.id.hotel_status, android.graphics.Color.parseColor("#00E676"))
                } else {
                    views.setTextViewText(
                        R.id.hotel_status,
                        if (isBengali) "• অপরিশোধিত" else "• Unpaid / Pending"
                    )
                    views.setTextColor(R.id.hotel_status, android.graphics.Color.parseColor("#FFB74D"))
                }
            } else {
                views.setViewVisibility(R.id.btn_add_meal, View.VISIBLE)
                views.setOnClickPendingIntent(R.id.btn_add_meal, pendingActionIntent)
                views.setTextViewText(R.id.btn_add_meal, buttonLabel)
                views.setViewVisibility(R.id.hotel_total, View.GONE)
                views.setViewVisibility(R.id.hotel_status, View.VISIBLE)
                views.setTextViewText(
                    R.id.hotel_status,
                    if (isBengali) "• সেটআপ করতে ট্যাপ করুন" else "• Tap to Setup Hotel"
                )
                views.setTextColor(R.id.hotel_status, android.graphics.Color.parseColor("#80D8FF"))
            }
            return views
        }

        withContext(Dispatchers.Main) {
            val compactViews = populateViews(R.layout.widget_hotel_compact)
            val expandedViews = populateViews(R.layout.widget_hotel)

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
    }
}
