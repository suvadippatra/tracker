package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.model.MeterReading
import com.example.data.model.UtilityBill
import com.example.data.repository.FinancialRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

sealed interface MeterReadingUiState {
    object Loading : MeterReadingUiState
    data class Success(
        val readings: List<MeterReading>,
        val bills: List<UtilityBill>,
        val currentProjectedBill: Double,
        val totalUnitsConsumedInRange: Double,
        val rates: List<com.example.data.model.ElectricityRate> = emptyList()
    ) : MeterReadingUiState
    data class Error(val message: String) : MeterReadingUiState
}

class MeterReadingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinancialRepository

    // Rate settings StateFlows
    val fixedServiceCharge = MutableStateFlow(0.0)

    // Current Date Range state
    var isManualDateRangeSet = false
    val calculationStartDate = MutableStateFlow<Long>(getStartOfMonthMillis())
    val calculationEndDate = MutableStateFlow<Long>(getEndOfDayMillis())

    init {
        val db = AppDatabase.getDatabase(application)
        repository = FinancialRepository(db)

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

    // Reactive computation of units consumed in selected range
    val unitsConsumedInRange: StateFlow<Double> = combine(
        calculationStartDate,
        calculationEndDate,
        repository.allMeterReadingsSorted
    ) { start, end, readings ->
        if (readings.isEmpty()) return@combine 0.0
        
        val startVal = estimateReadingAt(start, readings)
        val endVal = estimateReadingAt(end, readings)
        
        if (startVal != null && endVal != null) {
            (endVal - startVal).coerceAtLeast(0.0)
        } else {
            // Fallback: sum of deltas within range
            readings.filter { it.readingDate in (start + 1)..end }.sumOf { it.unitsConsumed }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    private fun getDayStart(time: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = time
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun estimateReadingAt(time: Long, readings: List<MeterReading>): Double? {
        val timeDay = getDayStart(time)
        return readings.find { getDayStart(it.readingDate) == timeDay }?.totalReading
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

    // Reactive computation of projected bill based on historical rate intervals
    val currentProjectedBill: StateFlow<Double> = combine(
        repository.allMeterReadingsSorted,
        dateRangeFlow,
        ratesFlow
    ) { sortedReadings, dateRange, ratesPair ->
        val (start, end) = dateRange
        val (rates, charge) = ratesPair
        if (start >= end || sortedReadings.isEmpty()) return@combine 0.0

        val startReading = estimateReadingAt(start, sortedReadings)
        val endReading = estimateReadingAt(end, sortedReadings)
        
        if (startReading == null || endReading == null) return@combine 0.0
        
        val totalUnits = (endReading - startReading).coerceAtLeast(0.0)
        
        val startDay = getDayStart(start)
        val endDay = getDayStart(end)
        val totalDays = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(endDay - startDay).coerceAtLeast(1)
        val dailyUnits = totalUnits / totalDays.toDouble()

        val validRates = rates.sortedBy { it.effectiveDate }
        
        val breakpoints = mutableListOf<Long>()
        breakpoints.add(startDay)
        
        validRates.forEach { rate ->
            val rateDay = getDayStart(rate.effectiveDate)
            if (rateDay > startDay && rateDay < endDay) {
                breakpoints.add(rateDay)
            }
        }
        if (!breakpoints.contains(endDay)) {
            breakpoints.add(endDay)
        }
        breakpoints.sort()

        var totalCost = 0.0

        for (i in 0 until breakpoints.size - 1) {
            val periodStart = breakpoints[i]
            val periodEnd = breakpoints[i + 1]
            
            val periodDays = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(periodEnd - periodStart).coerceAtLeast(0)
            val periodUnits = dailyUnits * periodDays
            
            val activeRate = validRates
                .filter { getDayStart(it.effectiveDate) <= periodStart }
                .maxByOrNull { it.effectiveDate }
                ?.ratePerUnit
                ?: 0.0
                
            totalCost += periodUnits * activeRate
        }

        totalCost + charge
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)
    val uiState: StateFlow<MeterReadingUiState> = combine(
        repository.allMeterReadings,
        repository.allUtilityBills,
        currentProjectedBill,
        unitsConsumedInRange,
        allElectricityRates
    ) { readings, bills, projected, units, ratesList ->
        MeterReadingUiState.Success(
            readings = readings,
            bills = bills,
            currentProjectedBill = projected,
            totalUnitsConsumedInRange = units,
            rates = ratesList
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
                
                val reading = MeterReading(
                    readingDate = dateMillis,
                    totalReading = totalReading,
                    unitsConsumed = if (consumed < 0) 0.0 else consumed, // Safety check
                    isPaid = false
                )
                repository.insertReading(reading)
            } catch (e: Exception) {
                // UI state catches or handles via Flow catch block
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
        transactionId: String? = null
    ) {
        viewModelScope.launch {
            try {
                val bill = UtilityBill(
                    startDate = startDate,
                    endDate = endDate,
                    totalUnits = totalUnits,
                    calculatedAmount = amount,
                    dueDate = dueDate,
                    isPaid = isPaid,
                    transactionId = transactionId
                )
                repository.insertUtilityBill(bill)
                if (isPaid) {
                    repository.updateReadingsPaidStatus(startDate, endDate, true)
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
            repository.insertReading(reading)
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
        fixedServiceCharge.value = charge
    }

    fun toggleBillPayment(bill: UtilityBill) {
        viewModelScope.launch {
            val newIsPaid = !bill.isPaid
            repository.insertUtilityBill(bill.copy(isPaid = newIsPaid))
            repository.updateReadingsPaidStatus(bill.startDate, bill.endDate, newIsPaid)
        }
    }

    fun insertElectricityRate(ratePerUnit: Double, dateMillis: Long) {
        viewModelScope.launch {
            repository.insertElectricityRate(
                com.example.data.model.ElectricityRate(
                    effectiveDate = dateMillis,
                    ratePerUnit = ratePerUnit
                )
            )
        }
    }

    fun deleteElectricityRate(rate: com.example.data.model.ElectricityRate) {
        viewModelScope.launch {
            repository.deleteElectricityRate(rate)
        }
    }

    // Accumulation logic calculation function as requested in guidelines
    fun calculateProjectedBill(totalUnits: Double): Double {
        val currentRate = allElectricityRates.value.maxByOrNull { it.effectiveDate }?.ratePerUnit ?: 0.0
        return totalUnits * currentRate + fixedServiceCharge.value
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
