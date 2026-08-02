with open('app/src/main/java/com/example/ui/navigation/Navigation.kt', 'r') as f:
    nav_content = f.read()

import re

# Replace MainBgGradient
nav_content = re.sub(
    r'val MainBgGradient = Brush\.verticalGradient\(.*?\)',
    '''val MainBgGradient = Brush.verticalGradient(
    0.0f to Color(0xFF0F0C29),
    0.4f to Color(0xFF1F1C45),
    0.7f to Color(0xFF242055),
    1.0f to Color(0xFF131131)
)''',
    nav_content,
    flags=re.DOTALL
)

# Replace Box(modifier = Modifier.fillMaxSize().background(MainBgGradient)) block's glows
glow_pattern = r'Box\(modifier = Modifier\.fillMaxSize\(\)\.background\(MainBgGradient\)\) \{.*?Scaffold\('
new_glows = '''Box(modifier = Modifier.fillMaxSize().background(MainBgGradient)) {
        // Glow at top left
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .align(Alignment.TopStart)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x775D35D3), Color.Transparent),
                        radius = 900f,
                        center = androidx.compose.ui.geometry.Offset(0f, 200f)
                    )
                )
        )
        
        // Glow at center right
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .align(Alignment.CenterEnd)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x553070E0), Color.Transparent),
                        radius = 800f,
                        center = androidx.compose.ui.geometry.Offset(1000f, 500f)
                    )
                )
        )
                
        Scaffold('''

nav_content = re.sub(glow_pattern, new_glows, nav_content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/navigation/Navigation.kt', 'w') as f:
    f.write(nav_content)

print("Navigation.kt background updated")
