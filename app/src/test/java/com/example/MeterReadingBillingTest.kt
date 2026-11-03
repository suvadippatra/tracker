package com.example

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ElectricityRate
import com.example.data.model.MeterReading
import com.example.data.model.UtilityBill
import com.example.ui.viewmodel.MeterReadingViewModel
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], instrumentedPackages = ["androidx.loader.content"])
class MeterReadingBillingTest {

    private lateinit var viewModel: MeterReadingViewModel

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        viewModel = MeterReadingViewModel(app, SavedStateHandle())
    }

    private fun dateMillis(year: Int, monthOneBased: Int, day: Int, hour: Int = 12): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthOneBased - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun `test multi-reading exact date range calculation`() {
        val readings = listOf(
            MeterReading(id = "r1", readingDate = dateMillis(2026, 10, 1), totalReading = 1000.0, unitsConsumed = 0.0, isPaid = true),
            MeterReading(id = "r2", readingDate = dateMillis(2026, 10, 5), totalReading = 1020.0, unitsConsumed = 20.0, isPaid = false),
            MeterReading(id = "r3", readingDate = dateMillis(2026, 10, 10), totalReading = 1050.0, unitsConsumed = 30.0, isPaid = false),
            MeterReading(id = "r4", readingDate = dateMillis(2026, 10, 15), totalReading = 1090.0, unitsConsumed = 40.0, isPaid = false)
        )
        val rates = listOf(
            ElectricityRate(id = "rate1", effectiveDate = dateMillis(2026, 10, 1), ratePerUnit = 8.0)
        )

        // Selecting Oct 1 to Oct 15 -> 90 units * 8 = 720
        val res = viewModel.calculateDetailedBill(
            start = dateMillis(2026, 10, 1),
            end = dateMillis(2026, 10, 15),
            sortedReadings = readings,
            rates = rates,
            fixedCharge = 50.0
        )
        assertEquals(90.0, res.totalUnits, 0.01)
        assertEquals(770.0, res.totalCost, 0.01)
    }

    @Test
    fun `test unlisted start and end dates linear interpolation`() {
        val readings = listOf(
            MeterReading(id = "r1", readingDate = dateMillis(2026, 10, 1), totalReading = 1000.0, unitsConsumed = 0.0, isPaid = false),
            MeterReading(id = "r2", readingDate = dateMillis(2026, 10, 5), totalReading = 1020.0, unitsConsumed = 20.0, isPaid = false),
            MeterReading(id = "r3", readingDate = dateMillis(2026, 10, 10), totalReading = 1050.0, unitsConsumed = 30.0, isPaid = false),
            MeterReading(id = "r4", readingDate = dateMillis(2026, 10, 15), totalReading = 1090.0, unitsConsumed = 40.0, isPaid = false)
        )
        val rates = listOf(
            ElectricityRate(id = "rate1", effectiveDate = dateMillis(2026, 10, 1), ratePerUnit = 10.0)
        )

        // Oct 3 (halfway between Oct 1 [1000] and Oct 5 [1020] = 1010)
        // to Oct 12 (2/5 between Oct 10 [1050] and Oct 15 [1090] = 1066)
        // Expected units = 1066 - 1010 = 56.0 kWh; Cost = 56 * 10 = 560.0
        val res = viewModel.calculateDetailedBill(
            start = dateMillis(2026, 10, 3),
            end = dateMillis(2026, 10, 12),
            sortedReadings = readings,
            rates = rates,
            fixedCharge = 0.0
        )
        assertEquals(56.0, res.totalUnits, 0.01)
        assertEquals(560.0, res.totalCost, 0.01)
    }

    @Test
    fun `test inactive meter readings carries over previous unpaid balance plus zero for extra days`() {
        val readings = listOf(
            MeterReading(id = "r1", readingDate = dateMillis(2026, 10, 1), totalReading = 500.0, unitsConsumed = 0.0, isPaid = true),
            MeterReading(id = "r2", readingDate = dateMillis(2026, 10, 5), totalReading = 520.0, unitsConsumed = 20.0, isPaid = true),
            MeterReading(id = "r3", readingDate = dateMillis(2026, 10, 10), totalReading = 550.0, unitsConsumed = 30.0, isPaid = false),
            MeterReading(id = "r4", readingDate = dateMillis(2026, 10, 15), totalReading = 590.0, unitsConsumed = 40.0, isPaid = false)
        )
        val rates = listOf(
            ElectricityRate(id = "rate1", effectiveDate = dateMillis(2026, 10, 1), ratePerUnit = 8.0)
        )

        // User stops updating readings after Oct 15.
        // Selecting from last unpaid reading date (Oct 15) to Oct 25 should carry over unpaid (Oct 5 [520] -> Oct 15 [590] = 70 units = ₹560) + ₹0
        val res = viewModel.calculateDetailedBill(
            start = dateMillis(2026, 10, 15),
            end = dateMillis(2026, 10, 25),
            sortedReadings = readings,
            rates = rates,
            fixedCharge = 0.0
        )
        assertEquals(70.0, res.totalUnits, 0.01)
        assertEquals(560.0, res.totalCost, 0.01)
    }

    @Test
    fun `test unlisted start date with end date beyond last reading interpolates and adds zero after last reading`() {
        val readings = listOf(
            MeterReading(id = "r1", readingDate = dateMillis(2026, 10, 1), totalReading = 100.0, unitsConsumed = 0.0, isPaid = false),
            MeterReading(id = "r2", readingDate = dateMillis(2026, 10, 11), totalReading = 200.0, unitsConsumed = 100.0, isPaid = false)
        )
        val rates = listOf(
            ElectricityRate(id = "rate1", effectiveDate = dateMillis(2026, 10, 1), ratePerUnit = 7.5)
        )
        val unpaidBills = listOf(
            UtilityBill(
                id = "b1",
                startDate = dateMillis(2026, 9, 1),
                endDate = dateMillis(2026, 9, 30),
                totalUnits = 40.0,
                calculatedAmount = 300.0,
                dueDate = dateMillis(2026, 10, 10),
                isPaid = false
            )
        )

        // Start = Oct 6 (interpolated 150.0), End = Oct 20 (clamped at Oct 11 = 200.0 -> 50 units @ 7.5 = 375.0)
        // Plus prior unpaid bill (40 units, ₹300.0) -> Total = 90 units, ₹675.0
        val res = viewModel.calculateDetailedBill(
            start = dateMillis(2026, 10, 6),
            end = dateMillis(2026, 10, 20),
            sortedReadings = readings,
            rates = rates,
            fixedCharge = 0.0,
            bills = unpaidBills
        )
        assertEquals(90.0, res.totalUnits, 0.01)
        assertEquals(675.0, res.totalCost, 0.01)
    }

    @Test
    fun `test pro-rata rate splitting across rate change on unlisted date`() {
        // 10 units per day from Oct 1 (100) to Oct 11 (200)
        val readings = listOf(
            MeterReading(id = "r1", readingDate = dateMillis(2026, 10, 1), totalReading = 100.0, unitsConsumed = 0.0, isPaid = false),
            MeterReading(id = "r2", readingDate = dateMillis(2026, 10, 11), totalReading = 200.0, unitsConsumed = 100.0, isPaid = false)
        )
        val rates = listOf(
            ElectricityRate(id = "rate1", effectiveDate = dateMillis(2026, 10, 1), ratePerUnit = 6.0),
            ElectricityRate(id = "rate2", effectiveDate = dateMillis(2026, 10, 6), ratePerUnit = 10.0)
        )

        // Oct 3 (120) to Oct 9 (180):
        // Oct 3 -> Oct 6: 30 units @ ₹6 = ₹180
        // Oct 6 -> Oct 9: 30 units @ ₹10 = ₹300
        // Total = 60 units, ₹480
        val res = viewModel.calculateDetailedBill(
            start = dateMillis(2026, 10, 3),
            end = dateMillis(2026, 10, 9),
            sortedReadings = readings,
            rates = rates,
            fixedCharge = 0.0
        )
        assertEquals(60.0, res.totalUnits, 0.01)
        assertEquals(480.0, res.totalCost, 0.01)
        assertEquals(2, res.breakdown.size)
    }
}
