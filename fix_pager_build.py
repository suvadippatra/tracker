with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "r") as f:
    content = f.read()

old_pager = """                androidx.compose.foundation.pager.HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                    pageSpacing = 16.dp,
                    contentPadding = PaddingValues(horizontal = 0.dp),
                    beyondBoundsPageCount = 1
                ) {"""
new_pager = """                androidx.compose.foundation.pager.HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                    pageSpacing = 16.dp,
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) {"""
content = content.replace(old_pager, new_pager)

with open("app/src/main/java/com/example/ui/screens/SmritiPocketScreen.kt", "w") as f:
    f.write(content)
