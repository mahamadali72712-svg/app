with open('app/src/main/java/com/example/ui/navigation/Navigation.kt', 'r') as f:
    nav_content = f.read()

import re

nav_content = re.sub(
    r'val MainBgGradient = Brush\.verticalGradient\([\s\S]*?1\.0f to Color\(0xFF14143D\)\n\)',
    '''val MainBgGradient = Brush.verticalGradient(
    0.0f to Color(0xFF0F0C29),
    0.4f to Color(0xFF1F1C45),
    0.7f to Color(0xFF242055),
    1.0f to Color(0xFF131131)
)''',
    nav_content
)

# Wait, let's just replace from `val MainBgGradient` to `@Composable`
nav_content = re.sub(
    r'val MainBgGradient = .*?@Composable\nfun AppNavigation',
    '''val MainBgGradient = Brush.verticalGradient(
    0.0f to Color(0xFF0F0C29),
    0.4f to Color(0xFF1F1C45),
    0.7f to Color(0xFF242055),
    1.0f to Color(0xFF131131)
)

@Composable
fun AppNavigation''',
    nav_content,
    flags=re.DOTALL
)

with open('app/src/main/java/com/example/ui/navigation/Navigation.kt', 'w') as f:
    f.write(nav_content)
