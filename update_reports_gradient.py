with open('app/src/main/java/com/example/ui/screens/ReportsScreen.kt', 'r') as f:
    content = f.read()

import re

new_bg = """val MainBgGradient = Brush.verticalGradient(
    0.0f to Color(0xFFFFFFFF), // White
    0.3f to Color(0xFFFFF3E0), // Light Orange
    0.7f to Color(0xFFE8F5E9), // Light Green
    1.0f to Color(0xFFA5D6A7)  // Darker Green
)"""

content = re.sub(
    r'val MainBgGradient = Brush\.verticalGradient\([\s\S]*?\)',
    new_bg,
    content
)

with open('app/src/main/java/com/example/ui/screens/ReportsScreen.kt', 'w') as f:
    f.write(content)
