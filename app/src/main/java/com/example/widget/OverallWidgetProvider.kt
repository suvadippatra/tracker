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
import java.util.Calendar

class OverallWidgetProvider : AppWidgetProvider() {

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
                val allHotels = db.hotelDao().getAllHotelsSync()
                val targetHotelId = user?.defaultHotelId ?: allHotels.firstOrNull()?.id ?: 1
                val hotel = db.hotelDao().getHotelByIdSync(targetHotelId) ?: allHotels.firstOrNull()
                val menus = if (hotel != null) db.menuDao().getMenusForHotelSync(hotel.id) else emptyList()

                val isHotelConfigured = allHotels.isNotEmpty() && hotel != null && menus.isNotEmpty()
                val hotelAction = if (isHotelConfigured) "ADD_MEAL_LOG" else "ADD_HOTEL"
                val hotelBtnLabel = if (isHotelConfigured) "+ Meal" else "+ Hotel"

                val hotelActionIntent = Intent(context, MainActivity::class.java).apply {
                    action = hotelAction
                    putExtra("action", hotelAction)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingHotelIntent = PendingIntent.getActivity(
                    context,
                    1,
                    hotelActionIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val accounts = db.bankAccountDao().getAllBankAccountsSync().filter { !it.isDeleted }
                val isAccountConfigured = accounts.isNotEmpty()
                val expenseAction = if (isAccountConfigured) "ADD_EXPENSE" else "ADD_ACCOUNT"
                val expenseBtnLabel = if (isAccountConfigured) "+ Expense" else "+ Account"

                val expenseActionIntent = Intent(context, MainActivity::class.java).apply {
                    action = expenseAction
                    putExtra("action", expenseAction)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingExpenseIntent = PendingIntent.getActivity(
                    context,
                    2,
                    expenseActionIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val hotelName = if (isHotelConfigured) (hotel?.name ?: "Hotels") else "No Hotel"

                val now = Calendar.getInstance()
                val currentMonth = now.get(Calendar.MONTH)
                val currentYear = now.get(Calendar.YEAR)

                val allTxs = if (hotel != null) db.transactionDao().getTransactionsForHotelSync(hotel.id) else emptyList()
                val monthTxs = allTxs.filter { tx ->
                    val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                    txCal.get(Calendar.MONTH) == currentMonth && txCal.get(Calendar.YEAR) == currentYear
                }
                val totalHotel = monthTxs.sumOf { it.totalAmount }

                val allExpenses = db.extraExpenseDao().getAllExpensesSync()
                val monthExpenses = allExpenses.filter { ex ->
                    val exCal = Calendar.getInstance().apply { timeInMillis = ex.timestamp }
                    exCal.get(Calendar.MONTH) == currentMonth && exCal.get(Calendar.YEAR) == currentYear
                }
                val totalExpenses = monthExpenses.sumOf { it.amount }

                val totalNet = totalHotel + totalExpenses

                val isBengali = user?.language == "bn"
                val titleLabel = if (isBengali) "📊 সামগ্রিক ($hotelName)" else "📊 Overall ($hotelName)"
                val netLabel = if (isBengali) "মোট: ₹ $totalNet" else "Net: ₹ $totalNet"
                val breakdownLabel = if (isBengali) "হোটেল: ₹ $totalHotel | অন্যান্য: ₹ $totalExpenses" else "Hotel: ₹ $totalHotel | Exp: ₹ $totalExpenses"

                fun populateViews(layoutRes: Int): RemoteViews {
                    val views = RemoteViews(context.packageName, layoutRes)
                    views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                    views.setTextViewText(R.id.overall_title, titleLabel)

                    if (isHotelConfigured || isAccountConfigured) {
                        views.setViewVisibility(R.id.overall_net, android.view.View.VISIBLE)
                        views.setTextViewText(R.id.overall_net, netLabel)
                    } else {
                        views.setViewVisibility(R.id.overall_net, android.view.View.GONE)
                    }

                    views.setTextViewText(R.id.overall_breakdown, breakdownLabel)

                    if (isHotelConfigured) {
                        views.setViewVisibility(R.id.btn_add_meal, android.view.View.VISIBLE)
                        views.setOnClickPendingIntent(R.id.btn_add_meal, pendingHotelIntent)
                        views.setTextViewText(R.id.btn_add_meal, hotelBtnLabel)
                    } else {
                        views.setViewVisibility(R.id.btn_add_meal, android.view.View.GONE)
                    }

                    if (isAccountConfigured) {
                        views.setViewVisibility(R.id.btn_add_expense, android.view.View.VISIBLE)
                        views.setOnClickPendingIntent(R.id.btn_add_expense, pendingExpenseIntent)
                        views.setTextViewText(R.id.btn_add_expense, expenseBtnLabel)
                    } else {
                        views.setViewVisibility(R.id.btn_add_expense, android.view.View.GONE)
                    }
                    return views
                }

                withContext(Dispatchers.Main) {
                    val compactViews = populateViews(R.layout.widget_overall_compact)
                    val expandedViews = populateViews(R.layout.widget_overall)

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
                Log.e("OverallWidget", "Error updating widget", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

