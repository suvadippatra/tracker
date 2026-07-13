import re

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

# Add graphicsLayer to GlassCard inside HorizontalPager
old_pager = """                androidx.compose.foundation.pager.HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                    pageSpacing = 16.dp,
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) { page ->
                    val hotel = hotels[page]
                    Box {
                        GlassCard("""

new_pager = """                androidx.compose.foundation.pager.HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                    pageSpacing = 16.dp,
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) { page ->
                    val hotel = hotels[page]
                    val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                    val scale = 1f - (0.05f * kotlin.math.abs(pageOffset).coerceAtMost(1f))
                    val alpha = 1f - (0.3f * kotlin.math.abs(pageOffset).coerceAtMost(1f))
                    Box(modifier = Modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }) {
                        GlassCard("""

content = content.replace(old_pager, new_pager)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
