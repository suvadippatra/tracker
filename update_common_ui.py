import re
with open('app/src/main/java/com/example/ui/components/CommonUI.kt', 'r') as f:
    content = f.read()

old_content = """@Composable
fun Modifier.slideInOnScroll(): Modifier {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }
    val offsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 100f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "slideIn"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "fadeIn"
    )
    return this.graphicsLayer {
        translationY = offsetY
        this.alpha = alpha
    }
}"""

new_content = """@Composable
fun Modifier.slideInOnScroll(): Modifier {
    return this
}"""

if old_content in content:
    content = content.replace(old_content, new_content)
    with open('app/src/main/java/com/example/ui/components/CommonUI.kt', 'w') as f:
        f.write(content)
    print("Replaced!")
else:
    print("Not found!")

