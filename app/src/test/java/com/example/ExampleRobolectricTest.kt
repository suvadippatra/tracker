package com.example
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.example.ui.screens.SettingsDialog
import com.example.ui.viewmodel.FinancialViewModel
import androidx.compose.runtime.Composable

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], instrumentedPackages = ["androidx.loader.content"])
class ExampleRobolectricTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testSettingsDialog() {
        val viewModel = FinancialViewModel(ApplicationProvider.getApplicationContext())
        composeTestRule.setContent {
            SettingsDialog(
                viewModel = viewModel,
                onDismiss = {},
                onOpenBin = {},
                onOpenOwnerProfile = {},
                onOpenUtilityConfig = {}
            )
        }
    }
}
