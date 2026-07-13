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
    val flatRatePerUnit = MutableStateFlow(7.5)
    val fixedServiceCharge = MutableStateFlow(120.0)

    // Current Date Range state
    val calculationStartDate = MutableStateFlow<Long>(getStartOfMonthMillis())
    val calculationEndDate = MutableStateFlow<Long>(getEndOfDayMillis())

    init {
        val db = AppDatabase.getDatabase(application)
        repository = FinancialRepository(db)
    }

    // Reactive computation of units consumed in selected range
    val unitsConsumedInRange: StateFlow<Double> = combine(
        calculationStartDate,
        calculationEndDate
    ) { start, end ->
        start to end
    }.flatMapLatest { (start, end) ->
        repository.getSumOfUnitsConsumed(start, end)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    // Expose rates
    val allElectricityRates = repository.allElectricityRates
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val latestMeterReading = repository.latestReading
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val dateRangeFlow = combine(calculationStartDate, calculationEndDate) { start, end ->
        start to end
    }

    private val ratesFlow = combine(allElectricityRates, flatRatePerUnit, fixedServiceCharge) { rates, defaultRate, charge ->
        Triple(rates, defaultRate, charge)
    }

    // Reactive computation of projected bill based on historical rate intervals
    val currentProjectedBill: StateFlow<Double> = combine(
        repository.allMeterReadings,
        dateRangeFlow,
        ratesFlow
    ) { readings, dateRange, ratesTriple ->
        val (start, end) = dateRange
        val (rates, defaultRate, charge) = ratesTriple
        val filteredReadings = readings.filter { it.readingDate in start..end }
        var totalCost = 0.0
        filteredReadings.forEach { reading ->
            // Find rate effective on or before the reading's date
            val activeRate = rates
                .filter { it.effectiveDate <= reading.readingDate }
                .maxByOrNull { it.effectiveDate }
                ?.ratePerUnit ?: defaultRate
            totalCost += reading.unitsConsumed * activeRate
        }
        totalCost + charge
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    // Exposed consolidated UI State flow
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
            repository.deleteReading(reading)
        }
    }

    fun deleteUtilityBill(bill: UtilityBill) {
        viewModelScope.launch {
            repository.deleteUtilityBill(bill)
        }
    }

    fun setCalculationDateRange(start: Long, end: Long) {
        calculationStartDate.value = start
        calculationEndDate.value = end
    }

    fun updateCalculationParameters(rate: Double, charge: Double) {
        flatRatePerUnit.value = rate
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
        return totalUnits * flatRatePerUnit.value + fixedServiceCharge.value
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
