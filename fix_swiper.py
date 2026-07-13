import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

old_pager = """                val pagerState = androidx.compose.foundation.pager.rememberPagerState(
                    initialPage = initialPage,
                    pageCount = { hotels.size }
                )
                
                LaunchedEffect(pagerState) {
                    androidx.compose.runtime.snapshotFlow { pagerState.currentPage }.collect { page ->
                        if (page in hotels.indices) {
                            viewModel.activeHotelId.value = hotels[page].id
                        }
                    }
                }"""

new_pager = """                val pagerState = androidx.compose.foundation.pager.rememberPagerState(
                    initialPage = initialPage,
                    pageCount = { hotels.size }
                )
                
                val view = androidx.compose.ui.platform.LocalView.current
                LaunchedEffect(pagerState) {
                    androidx.compose.runtime.snapshotFlow { pagerState.currentPage }.collect { page ->
                        if (page in hotels.indices) {
                            if (viewModel.activeHotelId.value != hotels[page].id) {
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                                viewModel.activeHotelId.value = hotels[page].id
                            }
                        }
                    }
                }"""

content = content.replace(old_pager, new_pager)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
