import os

with open("app/src/main/java/com/example/ui/screens/ProductsScreen.kt", "r") as f:
    content = f.read()

# Fix the background modifier
old_modifier = """        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(4.dp)
                .clip(CircleShape)
                .background(if (isSelected) Brush.horizontalGradient(listOf(GlowBlue, GlowPurple, GlowPink)) else Color.Transparent)
        )"""

new_modifier = """        val bgMod = if (isSelected) Modifier.background(Brush.horizontalGradient(listOf(GlowBlue, GlowPurple, GlowPink))) else Modifier.background(Color.Transparent)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(4.dp)
                .clip(CircleShape)
                .then(bgMod)
        )"""

content = content.replace(old_modifier, new_modifier)

# Add BadgeText at the end
badge_text = """

@Composable
fun BadgeText(text: String, containerColor: Color) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
"""

content += badge_text

with open("app/src/main/java/com/example/ui/screens/ProductsScreen.kt", "w") as f:
    f.write(content)

