package com.example.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance cached formatters for zero-allocation rendering in LazyLists and Canvas draws.
 */
object Formatters {
    private val sdfCache = ConcurrentHashMap<String, ThreadLocal<SimpleDateFormat>>()

    fun getSimpleDateFormat(pattern: String, locale: Locale = Locale.getDefault()): SimpleDateFormat {
        val key = "${pattern}_${locale.language}_${locale.country}"
        val threadLocal = sdfCache.computeIfAbsent(key) {
            ThreadLocal.withInitial { SimpleDateFormat(pattern, locale) }
        }
        return threadLocal.get() ?: SimpleDateFormat(pattern, locale)
    }

    fun formatDateTime(timestamp: Long, pattern: String = "dd MMM yyyy, hh:mm a"): String {
        if (timestamp <= 0L) return ""
        return getSimpleDateFormat(pattern).format(Date(timestamp))
    }

    fun formatDate(timestamp: Long, pattern: String = "dd MMM yyyy"): String {
        if (timestamp <= 0L) return ""
        return getSimpleDateFormat(pattern).format(Date(timestamp))
    }

    fun formatDayMonthYear(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        return getSimpleDateFormat("dd MMM, yyyy").format(Date(timestamp))
    }

    fun formatShortDate(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        return getSimpleDateFormat("dd MMM, hh:mm a").format(Date(timestamp))
    }

    fun formatDayMonth(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        return getSimpleDateFormat("dd MMM").format(Date(timestamp))
    }

    fun formatDayDotMonthYear(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        return getSimpleDateFormat("dd.MM.yyyy").format(Date(timestamp))
    }

    fun formatDateFull(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        return getSimpleDateFormat("dd MMMM yyyy").format(Date(timestamp))
    }

    fun formatMonthYear(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        return getSimpleDateFormat("MMMM yyyy").format(Date(timestamp))
    }

    private val currencyFormatThreadLocal = ThreadLocal.withInitial {
        val symbols = DecimalFormatSymbols(Locale.getDefault())
        DecimalFormat("#,##0.00", symbols)
    }

    private val intFormatThreadLocal = ThreadLocal.withInitial {
        val symbols = DecimalFormatSymbols(Locale.getDefault())
        DecimalFormat("#,##0", symbols)
    }

    fun formatCurrency(amount: Double): String {
        val df = currencyFormatThreadLocal.get() ?: DecimalFormat("#,##0.00")
        return "₹${df.format(amount)}"
    }

    fun formatCurrencyNoSymbol(amount: Double): String {
        val df = currencyFormatThreadLocal.get() ?: DecimalFormat("#,##0.00")
        return df.format(amount)
    }

    fun formatUnits(units: Double): String {
        val df = currencyFormatThreadLocal.get() ?: DecimalFormat("#,##0.00")
        return "${df.format(units)} Units"
    }
}
