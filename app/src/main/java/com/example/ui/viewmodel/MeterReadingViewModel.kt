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
    val periodEnd: Long,
    val startReading: Double = 0.0,
    val endReading: Double = 0.0
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
    var isManualDateRangeSet: Boolean = savedStateHandle.get<Boolean>("KEY_MANUAL_RANGE_SET") ?: false
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

    private data class NormalizedReading(
        val id: String,
        val readingDate: Long,
        val dayStart: Long,
        val rawTotal: Double,
        val rawUnitsConsumed: Double,
        val cumulativeReading: Double,
        val isPaid: Boolean
    )

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

    private fun normalizeReadings(readings: List<MeterReading>): List<NormalizedReading> {
        val sorted = readings.sortedBy { it.readingDate }
        if (sorted.isEmpty()) return emptyList()

        val result = ArrayList<NormalizedReading>(sorted.size)
        var currentCumulative = sorted[0].totalReading.coerceAtLeast(0.0)
        result.add(
            NormalizedReading(
                id = sorted[0].id,
                readingDate = sorted[0].readingDate,
                dayStart = getDayStart(sorted[0].readingDate),
                rawTotal = sorted[0].totalReading,
                rawUnitsConsumed = sorted[0].unitsConsumed,
                cumulativeReading = currentCumulative,
                isPaid = sorted[0].isPaid
            )
        )

        for (i in 1 until sorted.size) {
            val r = sorted[i]
            val prev = sorted[i - 1]
            val rawDiff = r.totalReading - prev.totalReading
            val step = when {
                rawDiff > 0.0 -> rawDiff
                r.unitsConsumed > 0.0 -> r.unitsConsumed
                else -> 0.0
            }
            currentCumulative += step
            result.add(
                NormalizedReading(
                    id = r.id,
                    readingDate = r.readingDate,
                    dayStart = getDayStart(r.readingDate),
                    rawTotal = r.totalReading,
                    rawUnitsConsumed = r.unitsConsumed,
                    cumulativeReading = currentCumulative,
                    isPaid = r.isPaid
                )
            )
        }
        return result
    }

    private fun estimateNormalizedReadingAt(
        time: Long,
        normalized: List<NormalizedReading>,
        isEndBoundary: Boolean = true
    ): Double? {
        if (normalized.isEmpty()) return null

        val timeDay = getDayStart(time)
        val sameDayReadings = normalized.filter { it.dayStart == timeDay }
        if (sameDayReadings.isNotEmpty()) {
            return if (isEndBoundary) {
                sameDayReadings.maxOf { it.cumulativeReading }
            } else {
                sameDayReadings.minOf { it.cumulativeReading }
            }
        }

        val preceding = normalized.filter { it.dayStart < timeDay }
            .maxByOrNull { it.readingDate }
        val succeeding = normalized.filter { it.dayStart > timeDay }
            .minByOrNull { it.readingDate }

        if (preceding != null && succeeding != null) {
            val t1 = preceding.dayStart
            val t2 = succeeding.dayStart
            val r1 = normalized.filter { it.dayStart == t1 }.maxOf { it.cumulativeReading }
            val r2 = normalized.filter { it.dayStart == t2 }.minOf { it.cumulativeReading }

            if (t2 <= t1) return r1

            val fraction = (timeDay - t1).toDouble() / (t2 - t1).toDouble()
            return r1 + fraction * (r2 - r1)
        }

        // If query is after all readings, return latest (so days after stopping readings add 0 units / 0 rupees);
        // if before all readings, return earliest.
        if (preceding != null && succeeding == null) {
            val lastDay = preceding.dayStart
            return normalized.filter { it.dayStart == lastDay }.maxOf { it.cumulativeReading }
        }
        if (preceding == null && succeeding != null) {
            val firstDay = succeeding.dayStart
            return normalized.filter { it.dayStart == firstDay }.minOf { it.cumulativeReading }
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

        val normalized = normalizeReadings(sortedReadings)

        // Helper for unpaid bills carry-over prior to or overlapping an inactive window
        fun getUnpaidBillsPriorTo(cutoffDay: Long): List<UtilityBill> {
            return bills.filter { !it.isPaid && it.calculatedAmount > 0.0 && getDayStart(it.startDate) <= cutoffDay }
        }

        if (normalized.isEmpty()) {
            val defaultRate = getActiveRateAt(endDay)
            val unpaidBills = getUnpaidBillsPriorTo(endDay)
            val carriedUnits = unpaidBills.sumOf { it.totalUnits }
            val carriedCost = unpaidBills.sumOf { it.calculatedAmount }
            val breakdown = unpaidBills.map { bill ->
                val effRate = if (bill.totalUnits > 0.0) Math.round((bill.calculatedAmount / bill.totalUnits) * 100.0) / 100.0 else defaultRate
                RateBreakdownItem(
                    ratePerUnit = effRate,
                    units = bill.totalUnits,
                    cost = bill.calculatedAmount,
                    periodStart = getDayStart(bill.startDate),
                    periodEnd = getDayStart(bill.endDate)
                )
            }
            return DetailedBillResult(
                totalUnits = Math.round(carriedUnits * 100.0) / 100.0,
                totalCost = Math.round((carriedCost + fixedCharge) * 100.0) / 100.0,
                breakdown = breakdown,
                activeRates = if (defaultRate > 0.0) listOf(defaultRate) else emptyList()
            )
        }

        val lastReadingDay = normalized.last().dayStart

        if (startDay == endDay) {
            val dayReadings = normalized.filter { it.dayStart == startDay }
            var dayUnits = if (dayReadings.isNotEmpty()) {
                val sumUnits = dayReadings.sumOf { it.rawUnitsConsumed }
                if (sumUnits > 0.0) sumUnits else (dayReadings.maxOf { it.cumulativeReading } - dayReadings.minOf { it.cumulativeReading }).coerceAtLeast(0.0)
            } else 0.0

            // If user selected a single day on or after lastReadingDay when readings stopped updating, carry over unpaid readings/bills
            if (dayUnits <= 0.0 && startDay >= lastReadingDay) {
                val unpaidReadings = normalized.filter { !it.isPaid && it.dayStart <= endDay }
                if (unpaidReadings.isNotEmpty()) {
                    val firstUnpaid = unpaidReadings.first()
                    val firstIdx = normalized.indexOfFirst { it.id == firstUnpaid.id }
                    val baseVal = if (firstIdx > 0) normalized[firstIdx - 1].cumulativeReading else (firstUnpaid.cumulativeReading - firstUnpaid.rawUnitsConsumed).coerceAtLeast(0.0)
                    dayUnits = (normalized.last().cumulativeReading - baseVal).coerceAtLeast(0.0)
                }
            }

            val singleStartReading = if (dayReadings.isNotEmpty()) {
                (dayReadings.maxOf { it.cumulativeReading } - dayUnits).coerceAtLeast(0.0)
            } else {
                (normalized.last().cumulativeReading - dayUnits).coerceAtLeast(0.0)
            }
            val singleEndReading = singleStartReading + dayUnits
            val rate = getActiveRateAt(startDay)
            val cost = Math.round((dayUnits * rate + fixedCharge) * 100.0) / 100.0
            return DetailedBillResult(
                totalUnits = Math.round(dayUnits * 100.0) / 100.0,
                totalCost = cost,
                breakdown = if (dayUnits > 0) listOf(
                    RateBreakdownItem(
                        ratePerUnit = rate,
                        units = dayUnits,
                        cost = dayUnits * rate,
                        periodStart = startDay,
                        periodEnd = endDay,
                        startReading = singleStartReading,
                        endReading = singleEndReading
                    )
                ) else emptyList(),
                activeRates = if (rate > 0.0) listOf(rate) else emptyList()
            )
        }

        var effectiveStartDay = startDay
        var rawStartReading = estimateNormalizedReadingAt(minTime, normalized, isEndBoundary = false)
        val rawEndReading = estimateNormalizedReadingAt(maxTime, normalized, isEndBoundary = true)

        if (rawStartReading == null || rawEndReading == null) {
            val defaultRate = getActiveRateAt(endDay)
            return DetailedBillResult(
                totalUnits = 0.0,
                totalCost = fixedCharge,
                breakdown = emptyList(),
                activeRates = if (defaultRate > 0.0) listOf(defaultRate) else emptyList()
            )
        }

        // Case 1: Meter readings are inactive in the selected window (user stopped updating readings after lastReadingDay
        // and selected from the last unpaid reading date or a later date up to endDay, or no readings exist after startDay).
        // Carry over previous unpaid reading intervals + unpaid bills + 0 rupees for the inactive duration after lastReadingDay.
        val readingsAfterStart = normalized.filter { it.dayStart > startDay }
        if (readingsAfterStart.isEmpty() && (rawEndReading - rawStartReading) <= 0.0 && startDay >= lastReadingDay) {
            val unpaidReadings = normalized.filter { !it.isPaid && it.dayStart <= endDay }
            if (unpaidReadings.isNotEmpty()) {
                // Find the first unpaid reading after the most recent paid reading (or the very first reading if none are paid)
                val lastPaidIdx = normalized.indexOfLast { it.isPaid && it.dayStart <= endDay }
                val firstUnpaidIdx = if (lastPaidIdx >= 0 && lastPaidIdx + 1 < normalized.size) {
                    lastPaidIdx + 1
                } else {
                    normalized.indexOfFirst { !it.isPaid }
                }
                if (firstUnpaidIdx > 0) {
                    val baselineReading = normalized[firstUnpaidIdx - 1]
                    rawStartReading = baselineReading.cumulativeReading
                    effectiveStartDay = baselineReading.dayStart
                } else if (firstUnpaidIdx == 0) {
                    val firstReading = normalized[0]
                    rawStartReading = (firstReading.cumulativeReading - firstReading.rawUnitsConsumed).coerceAtLeast(0.0)
                    effectiveStartDay = firstReading.dayStart
                }
            }
        }

        val startReading = rawStartReading!!
        val endReading = rawEndReading

        var totalUnits = (endReading - startReading).coerceAtLeast(0.0)

        // Fallback if cumulative difference is 0 but readings within [effectiveStartDay..endDay] recorded positive unitsConsumed
        if (totalUnits <= 0.0) {
            val readingsInWindow = normalized.filter {
                it.dayStart in effectiveStartDay..endDay && !it.isPaid
            }
            val summedUnits = readingsInWindow.sumOf { it.rawUnitsConsumed }
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

        // Include any explicit unpaid UtilityBill records strictly prior to effectiveStartDay when readings are inactive (endDay > lastReadingDay)
        if (endDay >= lastReadingDay) {
            val priorUnpaidBills = bills.filter {
                !it.isPaid && it.calculatedAmount > 0.0 && getDayEnd(it.endDate) < effectiveStartDay
            }
            for (unpaidBill in priorUnpaidBills) {
                val effRate = if (unpaidBill.totalUnits > 0.0) {
                    Math.round((unpaidBill.calculatedAmount / unpaidBill.totalUnits) * 100.0) / 100.0
                } else {
                    getActiveRateAt(unpaidBill.startDate)
                }
                totalUnits += unpaidBill.totalUnits
                totalCost += unpaidBill.calculatedAmount
                rawBreakdownItems.add(
                    RateBreakdownItem(
                        ratePerUnit = effRate,
                        units = unpaidBill.totalUnits,
                        cost = unpaidBill.calculatedAmount,
                        periodStart = getDayStart(unpaidBill.startDate),
                        periodEnd = getDayStart(unpaidBill.endDate)
                    )
                )
            }
        }

        val windowUnits = (endReading - startReading).coerceAtLeast(0.0).let { diff ->
            if (diff > 0.0) diff else normalized.filter { it.dayStart in effectiveStartDay..endDay && !it.isPaid }.sumOf { it.rawUnitsConsumed }.coerceAtLeast(0.0)
        }

        if (regionBoundaries.size <= 2) {
            val activeRate = getActiveRateAt(effectiveStartDay)
            val cost = windowUnits * activeRate
            totalCost += cost
            if (windowUnits > 0.0 || rawBreakdownItems.isEmpty()) {
                rawBreakdownItems.add(
                    RateBreakdownItem(
                        ratePerUnit = activeRate,
                        units = windowUnits,
                        cost = cost,
                        periodStart = startDay,
                        periodEnd = endDay,
                        startReading = startReading,
                        endReading = startReading + windowUnits
                    )
                )
            }
        } else {
            for (i in 0 until regionBoundaries.size - 1) {
                val rStart = regionBoundaries[i]
                val rEnd = regionBoundaries[i + 1]

                val startValue = if (i == 0) startReading else (estimateNormalizedReadingAt(rStart, normalized, isEndBoundary = false) ?: startReading)
                val endValue = if (i == regionBoundaries.size - 2) endReading else (estimateNormalizedReadingAt(rEnd, normalized, isEndBoundary = true) ?: endReading)

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
                        periodEnd = rEnd,
                        startReading = startValue,
                        endReading = endValue
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
                        periodEnd = item.periodEnd,
                        startReading = if (last.startReading > 0.0) last.startReading else item.startReading,
                        endReading = if (item.endReading > 0.0) item.endReading else (last.startReading + mergedUnits)
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
            repository.softDeleteUtilityBill(bill.id)
        }
    }

    fun setCalculationDateRange(start: Long, end: Long) {
        isManualDateRangeSet = true
        savedStateHandle["KEY_MANUAL_RANGE_SET"] = true
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
            repository.softDeleteElectricityRate(rate.id)
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
