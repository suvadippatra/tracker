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

    // Rate settings StateFlows
    val fixedServiceCharge = MutableStateFlow(0.0)

    // Current Date Range state
    var isManualDateRangeSet = false
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
                if (lastPaidBill != null) {
                    lastPaidBill.endDate
                } else {
                    sortedReadings.firstOrNull()?.readingDate ?: getStartOfMonthMillis()
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

    private fun estimateReadingAt(time: Long, readings: List<MeterReading>): Double? {
        val timeDay = getDayStart(time)
        val exact = readings.find { getDayStart(it.readingDate) == timeDay }
        if (exact != null) {
            return exact.totalReading
        }
        
        val sorted = readings.sortedBy { it.readingDate }
        if (sorted.isEmpty()) return null
        
        val preceding = sorted.lastOrNull { getDayStart(it.readingDate) < timeDay }
        val succeeding = sorted.firstOrNull { getDayStart(it.readingDate) > timeDay }
        
        if (preceding != null && succeeding != null) {
            val t1 = getDayStart(preceding.readingDate)
            val t2 = getDayStart(succeeding.readingDate)
            val r1 = preceding.totalReading
            val r2 = succeeding.totalReading
            
            if (t2 == t1) return r1
            
            val fraction = (timeDay - t1).toDouble() / (t2 - t1).toDouble()
            return r1 + fraction * (r2 - r1)
        }
        
        // If query is after all readings, return latest; if before all, return earliest
        if (preceding != null && succeeding == null) return preceding.totalReading
        if (preceding == null && succeeding != null) return succeeding.totalReading
        
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

    private val ratesFlow = combine(allElectricityRates, fixedServiceCharge) { rates, charge ->
        Pair(rates, charge)
    }

    fun calculateDetailedBill(
        start: Long,
        end: Long,
        sortedReadings: List<MeterReading>,
        rates: List<com.example.data.model.ElectricityRate>,
        fixedCharge: Double
    ): DetailedBillResult {
        val minTime = Math.min(start, end)
        val maxTime = Math.max(start, end)
        val startDay = getDayStart(minTime)
        val endDay = getDayStart(maxTime)

        val validRates = rates.sortedBy { it.effectiveDate }
        fun getActiveRateAt(time: Long): Double {
            if (validRates.isEmpty()) return 0.0
            val tDay = getDayStart(time)
            return validRates.filter { getDayStart(it.effectiveDate) <= tDay }
                .maxByOrNull { it.effectiveDate }
                ?.ratePerUnit
                ?: validRates.first().ratePerUnit
        }

        if (sortedReadings.isEmpty()) {
            val defaultRate = getActiveRateAt(endDay)
            return DetailedBillResult(
                totalUnits = 0.0,
                totalCost = fixedCharge,
                breakdown = emptyList(),
                activeRates = if (defaultRate > 0.0) listOf(defaultRate) else emptyList()
            )
        }

        if (startDay == endDay) {
            val dayReadings = sortedReadings.filter { getDayStart(it.readingDate) == startDay }
            val dayUnits = dayReadings.sumOf { it.unitsConsumed }
            val rate = getActiveRateAt(startDay)
            val cost = Math.round((dayUnits * rate + fixedCharge) * 100.0) / 100.0
            return DetailedBillResult(
                totalUnits = dayUnits,
                totalCost = cost,
                breakdown = if (dayUnits > 0) listOf(RateBreakdownItem(rate, dayUnits, dayUnits * rate, startDay, endDay)) else emptyList(),
                activeRates = if (rate > 0.0) listOf(rate) else emptyList()
            )
        }

        val startReading = estimateReadingAt(minTime, sortedReadings)
        val endReading = estimateReadingAt(maxTime, sortedReadings)

        if (startReading == null || endReading == null) {
            val defaultRate = getActiveRateAt(endDay)
            return DetailedBillResult(
                totalUnits = 0.0,
                totalCost = fixedCharge,
                breakdown = emptyList(),
                activeRates = if (defaultRate > 0.0) listOf(defaultRate) else emptyList()
            )
        }

        val totalUnits = (endReading - startReading).coerceAtLeast(0.0)

        // Identify rate cutoff dates strictly between startDay and endDay
        val rateCutoffs = validRates
            .map { getDayStart(it.effectiveDate) }
            .filter { it > startDay && it < endDay }
            .distinct()
            .sorted()

        val regionBoundaries = (listOf(startDay) + rateCutoffs + listOf(endDay)).distinct()

        val rawBreakdownItems = mutableListOf<RateBreakdownItem>()
        var totalCost = 0.0

        for (i in 0 until regionBoundaries.size - 1) {
            val rStart = regionBoundaries[i]
            val rEnd = regionBoundaries[i + 1]

            val startValue = estimateReadingAt(rStart, sortedReadings) ?: startReading
            val endValue = estimateReadingAt(rEnd, sortedReadings) ?: endReading

            val intervalUnits = (endValue - startValue).coerceAtLeast(0.0)
            val activeRate = getActiveRateAt(rStart)
            val cost = intervalUnits * activeRate
            totalCost += cost

            rawBreakdownItems.add(
                RateBreakdownItem(
                    ratePerUnit = activeRate,
                    units = intervalUnits,
                    cost = cost,
                    periodStart = rStart,
                    periodEnd = rEnd
                )
            )
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
    ) { sortedReadings, dateRange, ratesPair ->
        val (start, end) = dateRange
        val (rates, charge) = ratesPair
        calculateDetailedBill(start, end, sortedReadings, rates, charge)
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
                val lastReading = latestMeterReading.value
                val consumed = if (lastReading == null) 0.0 else totalReading - lastReading.totalReading
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
                val roundedAmount = Math.round(amount * 100.0) / 100.0
                val roundedUnits = Math.round(totalUnits * 100.0) / 100.0
                val bill = UtilityBill(
                    startDate = startDate,
                    endDate = endDate,
                    totalUnits = roundedUnits,
                    calculatedAmount = roundedAmount,
                    dueDate = dueDate,
                    isPaid = isPaid,
                    transactionId = transactionId,
                    note = note.trim()
                )
                repository.insertUtilityBill(bill)
                if (isPaid) {
                    val startDay = getDayStart(startDate)
                    val endDay = getDayEnd(endDate)
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

    fun updateCalculationParameters(charge: Double) {
        fixedServiceCharge.value = Math.round(charge * 100.0) / 100.0
    }

    fun updateBillPaymentStatus(bill: UtilityBill, newIsPaid: Boolean, note: String = bill.note) {
        viewModelScope.launch {
            repository.insertUtilityBill(bill.copy(isPaid = newIsPaid, note = note.trim()))
            val startDay = getDayStart(bill.startDate)
            val endDay = getDayEnd(bill.endDate)
            repository.updateReadingsPaidStatus(startDay, endDay, newIsPaid)
        }
    }

    fun toggleBillPayment(bill: UtilityBill, note: String? = null) {
        viewModelScope.launch {
            val newIsPaid = !bill.isPaid
            repository.insertUtilityBill(bill.copy(isPaid = newIsPaid, note = note?.trim() ?: bill.note))
            val startDay = getDayStart(bill.startDate)
            val endDay = getDayEnd(bill.endDate)
            repository.updateReadingsPaidStatus(startDay, endDay, newIsPaid)
        }
    }

    fun insertElectricityRate(ratePerUnit: Double, dateMillis: Long) {
        viewModelScope.launch {
            val roundedRate = Math.round(ratePerUnit * 100.0) / 100.0
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
        val currentRate = allElectricityRates.value.maxByOrNull { it.effectiveDate }?.ratePerUnit ?: 0.0
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
