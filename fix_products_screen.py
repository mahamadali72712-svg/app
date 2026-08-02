import re

with open('app/src/main/java/com/example/ui/screens/ProductsScreen.kt', 'r') as f:
    content = f.read()

# 1. Update CosmicBgBrush to light wavy colors
content = re.sub(
    r'val CosmicBgBrush = Brush\.verticalGradient\([\s\S]*?endY = 1800f\n\)',
    '''val CosmicBgBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFF0F4FF), // Very Light Blue
        Color(0xFFE3F2FD), // Light Blue
        Color(0xFFE8F5E9), // Light Greenish
        Color(0xFFFFFFFF)  // White bottom
    ),
    startY = 0f,
    endY = 1800f
)''',
    content
)

# 2. Reduce empty space (spacers)
content = content.replace('Spacer(modifier = Modifier.height(24.dp))', 'Spacer(modifier = Modifier.height(12.dp))')
content = content.replace('Spacer(modifier = Modifier.height(16.dp))', 'Spacer(modifier = Modifier.height(10.dp))')

# Update Top bar text color because background is now light
content = content.replace('Color.White', 'Color(0xFF111122)')
# Oh wait, Color.White is used in many places. I should be more specific. Let's not do replace all!
