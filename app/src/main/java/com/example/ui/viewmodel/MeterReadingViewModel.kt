package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.model.MeterReading
import com.example.data.model.UtilityBill
import com.example.data.repository.FinancialRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class RateBreakdownItem(
    val ratePerUnit: Double,
    val units: Double,
    val cost: Double,
    val periodStart: Long,
    val periodEnd: Long
)

data class DetailedBillResult(
    val totalUnits: Double,
    val totalCost: Double,
    val breakdown: List<RateBreakdownItem>,
    val activeRates: List<Double>
)

sealed interface MeterReadingUiState {
    object Loading : MeterReadingUiState
    data class Success(
        val readings: List<MeterReading>,
        val bills: List<UtilityBill>,
        val currentProjectedBill: Double,
        val totalUnitsConsumedInRange: Double,
        val rates: List<com.example.data.model.ElectricityRate> = emptyList(),
        val rateBreakdown: List<RateBreakdownItem> = emptyList(),
        val activeRatesInRange: List<Double> = emptyList()
    ) : MeterReadingUiState
    data class Error(val message: String) : MeterReadingUiState
}

class MeterReadingViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val repository: FinancialRepository
    private val prefs = application.getSharedPreferences("traker_utility_prefs", android.content.Context.MODE_PRIVATE)

    // Rate settings StateFlows
    val fixedServiceCharge = MutableStateFlow(prefs.getFloat("fixed_service_charge", 0.0f).toDouble())
    val defaultElectricityRate = MutableStateFlow(prefs.getFloat("default_electricity_rate", 8.0f).toDouble())

    // Current Date Range state
    var isManualDateRangeSet = savedStateHandle.contains("KEY_CALC_START_DATE") || savedStateHandle.contains("KEY_CALC_END_DATE")
    val calculationStartDate = MutableStateFlow<Long>(
        savedStateHandle.get<Long>("KEY_CALC_START_DATE") ?: getStartOfMonthMillis()
    )
    val calculationEndDate = MutableStateFlow<Long>(
        savedStateHandle.get<Long>("KEY_CALC_END_DATE") ?: getEndOfDayMillis()
    )

    init {
        val db = AppDatabase.getDatabase(application)
        repository = FinancialRepository(db)

        viewModelScope.launch {
            calculationStartDate.collect { date ->
                savedStateHandle["KEY_CALC_START_DATE"] = date
            }
        }
        viewModelScope.launch {
            calculationEndDate.collect { date ->
                savedStateHandle["KEY_CALC_END_DATE"] = date
            }
        }

        viewModelScope.launch {
            combine(
                repository.allUtilityBills,
                repository.allMeterReadingsSorted
            ) { bills, sortedReadings ->
                val lastPaidBill = bills.filter { it.isPaid }.maxByOrNull { it.endDate }
                val lastPaidReading = sortedReadings.filter { it.isPaid }.maxByOrNull { it.readingDate }
                when {
                    lastPaidBill != null -> lastPaidBill.endDate
                    lastPaidReading != null -> lastPaidReading.readingDate
                    else -> sortedReadings.firstOrNull()?.readingDate ?: getStartOfMonthMillis()
                }
            }.collect { autoStart ->
                if (!isManualDateRangeSet) {
                    calculationStartDate.value = autoStart
                }
            }
        }
    }

    private fun getDayStart(time: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = time
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getDayEnd(time: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = time
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    private fun estimateReadingAt(
        time: Long,
        readings: List<MeterReading>,
        isEndBoundary: Boolean = true
    ): Double? {
        val sorted = readings.sortedBy { it.readingDate }
        if (sorted.isEmpty()) return null

        val timeDay = getDayStart(time)
        val sameDayReadings = sorted.filter { getDayStart(it.readingDate) == timeDay }
        if (sameDayReadings.isNotEmpty()) {
            return if (isEndBoundary) {
                sameDayReadings.maxOf { it.totalReading }
            } else {
                sameDayReadings.minOf { it.totalReading }
            }
        }

        val preceding = sorted.filter { getDayStart(it.readingDate) < timeDay }
            .maxByOrNull { it.readingDate }
        val succeeding = sorted.filter { getDayStart(it.readingDate) > timeDay }
            .minByOrNull { it.readingDate }

        if (preceding != null && succeeding != null) {
            val t1 = getDayStart(preceding.readingDate)
            val t2 = getDayStart(succeeding.readingDate)
            val r1 = sorted.filter { getDayStart(it.readingDate) == t1 }.maxOf { it.totalReading }
            val r2 = sorted.filter { getDayStart(it.readingDate) == t2 }.minOf { it.totalReading }

            if (t2 <= t1) return r1

            val fraction = (timeDay - t1).toDouble() / (t2 - t1).toDouble()
            return r1 + fraction * (r2 - r1)
        }

        // If query is after all readings, return latest (so days after stopping readings add 0 units / 0 rupees);
        // if before all readings, return earliest.
        if (preceding != null && succeeding == null) {
            val lastDay = getDayStart(preceding.readingDate)
            return sorted.filter { getDayStart(it.readingDate) == lastDay }.maxOf { it.totalReading }
        }
        if (preceding == null && succeeding != null) {
            val firstDay = getDayStart(succeeding.readingDate)
            return sorted.filter { getDayStart(it.readingDate) == firstDay }.minOf { it.totalReading }
        }

        return null
    }

    // Expose rates
    val allElectricityRates = repository.allElectricityRates
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val latestMeterReading = repository.latestReading
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val dateRangeFlow = combine(calculationStartDate, calculationEndDate) { start, end ->
        start to end
    }

    private val ratesFlow = combine(
        allElectricityRates,
        fixedServiceCharge,
        defaultElectricityRate,
        repository.allUtilityBills
    ) { rates, charge, defaultRate, bills ->
        Triple(rates, charge to defaultRate, bills)
    }

    fun calculateDetailedBill(
        start: Long,
        end: Long,
        sortedReadings: List<MeterReading>,
        rates: List<com.example.data.model.ElectricityRate>,
        fixedCharge: Double,
        bills: List<UtilityBill> = emptyList()
    ): DetailedBillResult {
        val minTime = Math.min(start, end)
        val maxTime = Math.max(start, end)
        val startDay = getDayStart(minTime)
        val endDay = getDayStart(maxTime)

        val validRates = rates.filter { it.ratePerUnit > 0.0 }.sortedBy { it.effectiveDate }
        val historicalBillRate = bills
            .filter { it.totalUnits > 0.0 && it.calculatedAmount > 0.0 }
            .maxByOrNull { it.endDate }
            ?.let { Math.round((it.calculatedAmount / it.totalUnits) * 100.0) / 100.0 }
        val fallbackRate = historicalBillRate
            ?: defaultElectricityRate.value.takeIf { it > 0.0 }
            ?: 8.0

        fun getActiveRateAt(time: Long): Double {
            if (validRates.isEmpty()) return fallbackRate
            val tDay = getDayStart(time)
            return validRates.filter { getDayStart(it.effectiveDate) <= tDay }
                .maxByOrNull { it.effectiveDate }
                ?.ratePerUnit
                ?: validRates.first().ratePerUnit
        }

        val chronologicallySorted = sortedReadings.sortedBy { it.readingDate }
        if (chronologicallySorted.isEmpty()) {
            val defaultRate = getActiveRateAt(endDay)
            return DetailedBillResult(
                totalUnits = 0.0,
                totalCost = fixedCharge,
                breakdown = emptyList(),
                activeRates = if (defaultRate > 0.0) listOf(defaultRate) else emptyList()
            )
        }

        if (startDay == endDay) {
            val dayReadings = chronologicallySorted.filter { getDayStart(it.readingDate) == startDay }
            val dayUnits = if (dayReadings.isNotEmpty()) {
                val sumUnits = dayReadings.sumOf { it.unitsConsumed }
                if (sumUnits > 0.0) sumUnits else (dayReadings.maxOf { it.totalReading } - dayReadings.minOf { it.totalReading }).coerceAtLeast(0.0)
            } else 0.0
            val rate = getActiveRateAt(startDay)
            val cost = Math.round((dayUnits * rate + fixedCharge) * 100.0) / 100.0
            return DetailedBillResult(
                totalUnits = Math.round(dayUnits * 100.0) / 100.0,
                totalCost = cost,
                breakdown = if (dayUnits > 0) listOf(RateBreakdownItem(rate, dayUnits, dayUnits * rate, startDay, endDay)) else emptyList(),
                activeRates = if (rate > 0.0) listOf(rate) else emptyList()
            )
        }

        val lastReadingDay = getDayStart(chronologicallySorted.last().readingDate)

        // Determine effective startDay and startReading baseline:
        // If the user stopped updating readings after lastReadingDay and selects from the last unpaid reading date
        // to today/a later date (startDay >= lastReadingDay && endDay >= lastReadingDay), or if startDay lands on
        // the first unpaid reading after paid sessions, use the pre-unpaid baseline so the previous unpaid bill + 0 rupees is preserved.
        var effectiveStartDay = startDay
        var rawStartReading = estimateReadingAt(minTime, chronologicallySorted, isEndBoundary = false)
        val rawEndReading = estimateReadingAt(maxTime, chronologicallySorted, isEndBoundary = true)

        if (rawStartReading == null || rawEndReading == null) {
            val defaultRate = getActiveRateAt(endDay)
            return DetailedBillResult(
                totalUnits = 0.0,
                totalCost = fixedCharge,
                breakdown = emptyList(),
                activeRates = if (defaultRate > 0.0) listOf(defaultRate) else emptyList()
            )
        }

        // Case 1: User stopped updating readings and selected [lastReadingDay..endDay] where no readings exist after startDay
        val readingsAfterStart = chronologicallySorted.filter { getDayStart(it.readingDate) > startDay }
        if (readingsAfterStart.isEmpty() && (rawEndReading - rawStartReading) <= 0.0) {
            val unpaidReadings = chronologicallySorted.filter { !it.isPaid && getDayStart(it.readingDate) <= endDay }
            if (unpaidReadings.isNotEmpty()) {
                val firstUnpaid = unpaidReadings.first()
                val firstUnpaidIndex = chronologicallySorted.indexOfFirst { it.id == firstUnpaid.id }
                if (firstUnpaidIndex > 0) {
                    val baselineReading = chronologicallySorted[firstUnpaidIndex - 1]
                    rawStartReading = baselineReading.totalReading
                    effectiveStartDay = getDayStart(baselineReading.readingDate)
                } else {
                    rawStartReading = (firstUnpaid.totalReading - firstUnpaid.unitsConsumed).coerceAtLeast(0.0)
                    effectiveStartDay = getDayStart(firstUnpaid.readingDate)
                }
            }
        } else {
            // Case 2: startDay lands directly on an unpaid reading whose own unitsConsumed should be part of this period
            // (e.g., first unpaid reading after paid readings, or only 1 reading day in range)
            val startDayReadings = chronologicallySorted.filter { getDayStart(it.readingDate) == startDay }
            val firstOnStartDay = startDayReadings.firstOrNull()
            if (firstOnStartDay != null && !firstOnStartDay.isPaid && firstOnStartDay.unitsConsumed > 0.0) {
                val idx = chronologicallySorted.indexOfFirst { it.id == firstOnStartDay.id }
                val allPriorPaid = idx > 0 && chronologicallySorted.subList(0, idx).all { it.isPaid }
                if (allPriorPaid) {
                    rawStartReading = chronologicallySorted[idx - 1].totalReading
                }
            }
        }

        val startReading = rawStartReading!!
        val endReading = rawEndReading

        var totalUnits = (endReading - startReading).coerceAtLeast(0.0)

        // Fallback if totalReading difference is 0 but readings within [effectiveStartDay..endDay] recorded positive unitsConsumed
        if (totalUnits <= 0.0) {
            val readingsInWindow = chronologicallySorted.filter {
                val d = getDayStart(it.readingDate)
                d in effectiveStartDay..endDay && !it.isPaid
            }
            val summedUnits = readingsInWindow.sumOf { it.unitsConsumed }
            if (summedUnits > 0.0) {
                totalUnits = summedUnits
            }
        }

        // Identify rate cutoff dates strictly between effectiveStartDay and endDay
        val rateCutoffs = validRates
            .map { getDayStart(it.effectiveDate) }
            .filter { it > effectiveStartDay && it < endDay }
            .distinct()
            .sorted()

        val regionBoundaries = (listOf(effectiveStartDay) + rateCutoffs + listOf(endDay)).distinct()

        val rawBreakdownItems = mutableListOf<RateBreakdownItem>()
        var totalCost = 0.0

        if (regionBoundaries.size <= 2) {
            val activeRate = getActiveRateAt(effectiveStartDay)
            val cost = totalUnits * activeRate
            totalCost = cost
            rawBreakdownItems.add(
                RateBreakdownItem(
                    ratePerUnit = activeRate,
                    units = totalUnits,
                    cost = cost,
                    periodStart = startDay,
                    periodEnd = endDay
                )
            )
        } else {
            for (i in 0 until regionBoundaries.size - 1) {
                val rStart = regionBoundaries[i]
                val rEnd = regionBoundaries[i + 1]

                val startValue = if (i == 0) startReading else (estimateReadingAt(rStart, chronologicallySorted, isEndBoundary = false) ?: startReading)
                val endValue = if (i == regionBoundaries.size - 2) endReading else (estimateReadingAt(rEnd, chronologicallySorted, isEndBoundary = true) ?: endReading)

                val intervalUnits = (endValue - startValue).coerceAtLeast(0.0)
                val activeRate = getActiveRateAt(rStart)
                val cost = intervalUnits * activeRate
                totalCost += cost

                rawBreakdownItems.add(
                    RateBreakdownItem(
                        ratePerUnit = activeRate,
                        units = intervalUnits,
                        cost = cost,
                        periodStart = if (i == 0) startDay else rStart,
                        periodEnd = rEnd
                    )
                )
            }
        }

        // Merge adjacent breakdown items if they have the same ratePerUnit
        val breakdownItems = mutableListOf<RateBreakdownItem>()
        for (item in rawBreakdownItems) {
            if (breakdownItems.isEmpty()) {
                breakdownItems.add(item)
            } else {
                val last = breakdownItems.last()
                if (last.ratePerUnit == item.ratePerUnit) {
                    val mergedUnits = last.units + item.units
                    val mergedCost = last.cost + item.cost
                    breakdownItems[breakdownItems.size - 1] = RateBreakdownItem(
                        ratePerUnit = last.ratePerUnit,
                        units = mergedUnits,
                        cost = mergedCost,
                        periodStart = last.periodStart,
                        periodEnd = item.periodEnd
                    )
                } else {
                    breakdownItems.add(item)
                }
            }
        }

        val activeRates = breakdownItems.map { it.ratePerUnit }.distinct()

        val finalCost = Math.round((totalCost + fixedCharge) * 100.0) / 100.0
        val finalUnits = Math.round(totalUnits * 100.0) / 100.0

        return DetailedBillResult(
            totalUnits = finalUnits,
            totalCost = finalCost,
            breakdown = breakdownItems,
            activeRates = if (activeRates.isNotEmpty()) activeRates else listOf(getActiveRateAt(endDay))
        )
    }

    val billCalculationResult: StateFlow<DetailedBillResult> = combine(
        repository.allMeterReadingsSorted,
        dateRangeFlow,
        ratesFlow
    ) { sortedReadings, dateRange, ratesTriple ->
        val (start, end) = dateRange
        val (rates, chargeAndDefault, bills) = ratesTriple
        val (charge, _) = chargeAndDefault
        calculateDetailedBill(start, end, sortedReadings, rates, charge, bills)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        DetailedBillResult(0.0, 0.0, emptyList(), emptyList())
    )

    val unitsConsumedInRange: StateFlow<Double> = billCalculationResult
        .map { it.totalUnits }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val currentProjectedBill: StateFlow<Double> = billCalculationResult
        .map { it.totalCost }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val uiState: StateFlow<MeterReadingUiState> = combine(
        repository.allMeterReadings,
        repository.allUtilityBills,
        billCalculationResult,
        allElectricityRates
    ) { readings, bills, calcResult, ratesList ->
        MeterReadingUiState.Success(
            readings = readings,
            bills = bills,
            currentProjectedBill = calcResult.totalCost,
            totalUnitsConsumedInRange = calcResult.totalUnits,
            rates = ratesList,
            rateBreakdown = calcResult.breakdown,
            activeRatesInRange = calcResult.activeRates
        ) as MeterReadingUiState
    }.catch { e ->
        emit(MeterReadingUiState.Error(e.localizedMessage ?: "Unknown Database Error") as MeterReadingUiState)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MeterReadingUiState.Loading)

    // Insertion & Deletion Methods
    fun insertReading(totalReading: Double, dateMillis: Long) {
        viewModelScope.launch {
            try {
                val allSorted = repository.allMeterReadingsSorted.firstOrNull().orEmpty().sortedBy { it.readingDate }
                val prevReading = allSorted.lastOrNull { it.readingDate <= dateMillis } ?: latestMeterReading.value
                val consumed = if (prevReading == null) 0.0 else totalReading - prevReading.totalReading
                val roundedReading = Math.round(totalReading * 100.0) / 100.0
                val roundedConsumed = Math.round((if (consumed < 0) 0.0 else consumed) * 100.0) / 100.0

                val reading = MeterReading(
                    readingDate = dateMillis,
                    totalReading = roundedReading,
                    unitsConsumed = roundedConsumed,
                    isPaid = false
                )
                repository.insertReading(reading)
            } catch (e: Exception) {
                // Handled
            }
        }
    }

    fun insertUtilityBill(
        startDate: Long,
        endDate: Long,
        totalUnits: Double,
        amount: Double,
        dueDate: Long,
        isPaid: Boolean,
        transactionId: String? = null,
        note: String = ""
    ) {
        viewModelScope.launch {
            try {
                val normStart = minOf(startDate, endDate)
                val normEnd = maxOf(startDate, endDate)
                val roundedAmount = Math.round(amount * 100.0) / 100.0
                val roundedUnits = Math.round(totalUnits * 100.0) / 100.0
                val bill = UtilityBill(
                    startDate = normStart,
                    endDate = normEnd,
                    totalUnits = roundedUnits,
                    calculatedAmount = roundedAmount,
                    dueDate = dueDate,
                    isPaid = isPaid,
                    transactionId = transactionId,
                    note = note.trim()
                )
                repository.insertUtilityBill(bill)
                if (isPaid) {
                    val startDay = getDayStart(normStart)
                    val endDay = getDayEnd(normEnd)
                    repository.updateReadingsPaidStatus(startDay, endDay, true)
                }
            } catch (e: Exception) {
                // Handled
            }
        }
    }

    fun deleteReading(reading: MeterReading) {
        viewModelScope.launch {
            repository.softDeleteReading(reading.id)
        }
    }

    fun updateReading(reading: MeterReading) {
        viewModelScope.launch {
            val roundedReading = Math.round(reading.totalReading * 100.0) / 100.0
            val roundedConsumed = Math.round(reading.unitsConsumed * 100.0) / 100.0
            repository.insertReading(reading.copy(totalReading = roundedReading, unitsConsumed = roundedConsumed))
        }
    }

    fun deleteUtilityBill(bill: UtilityBill) {
        viewModelScope.launch {
            repository.deleteUtilityBill(bill)
        }
    }

    fun setCalculationDateRange(start: Long, end: Long) {
        isManualDateRangeSet = true
        calculationStartDate.value = start
        calculationEndDate.value = end
    }

    fun updateCalculationParameters(charge: Double, defaultRate: Double = defaultElectricityRate.value) {
        val roundedCharge = Math.round(charge * 100.0) / 100.0
        val roundedDefaultRate = Math.round(defaultRate * 100.0) / 100.0
        fixedServiceCharge.value = roundedCharge
        if (roundedDefaultRate > 0.0) {
            defaultElectricityRate.value = roundedDefaultRate
        }
        prefs.edit()
            .putFloat("fixed_service_charge", roundedCharge.toFloat())
            .putFloat("default_electricity_rate", defaultElectricityRate.value.toFloat())
            .apply()
    }

    fun updateBillPaymentStatus(bill: UtilityBill, newIsPaid: Boolean, note: String = bill.note) {
        viewModelScope.launch {
            val normStart = minOf(bill.startDate, bill.endDate)
            val normEnd = maxOf(bill.startDate, bill.endDate)
            repository.insertUtilityBill(bill.copy(startDate = normStart, endDate = normEnd, isPaid = newIsPaid, note = note.trim()))
            val startDay = getDayStart(normStart)
            val endDay = getDayEnd(normEnd)
            repository.updateReadingsPaidStatus(startDay, endDay, newIsPaid)
        }
    }

    fun toggleBillPayment(bill: UtilityBill, note: String? = null) {
        viewModelScope.launch {
            val newIsPaid = !bill.isPaid
            val normStart = minOf(bill.startDate, bill.endDate)
            val normEnd = maxOf(bill.startDate, bill.endDate)
            repository.insertUtilityBill(bill.copy(startDate = normStart, endDate = normEnd, isPaid = newIsPaid, note = note?.trim() ?: bill.note))
            val startDay = getDayStart(normStart)
            val endDay = getDayEnd(normEnd)
            repository.updateReadingsPaidStatus(startDay, endDay, newIsPaid)
        }
    }

    fun insertElectricityRate(ratePerUnit: Double, dateMillis: Long) {
        viewModelScope.launch {
            val roundedRate = Math.round(ratePerUnit * 100.0) / 100.0
            if (roundedRate > 0.0) {
                defaultElectricityRate.value = roundedRate
                prefs.edit().putFloat("default_electricity_rate", roundedRate.toFloat()).apply()
            }
            repository.insertElectricityRate(
                com.example.data.model.ElectricityRate(
                    effectiveDate = dateMillis,
                    ratePerUnit = roundedRate
                )
            )
        }
    }

    fun deleteElectricityRate(rate: com.example.data.model.ElectricityRate) {
        viewModelScope.launch {
            repository.deleteElectricityRate(rate)
        }
    }

    fun calculateProjectedBill(totalUnits: Double): Double {
        val currentRate = allElectricityRates.value.filter { it.ratePerUnit > 0.0 }.maxByOrNull { it.effectiveDate }?.ratePerUnit
            ?: defaultElectricityRate.value.takeIf { it > 0.0 }
            ?: 8.0
        val cost = totalUnits * currentRate + fixedServiceCharge.value
        return Math.round(cost * 100.0) / 100.0
    }

    // Helper functions for calendar millisecond calculation
    private fun getStartOfMonthMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getEndOfDayMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }
}
