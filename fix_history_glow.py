import re

def fix():
    filepath = 'app/src/main/java/com/example/ui/screens/PurchasesHistoryScreen.kt'
    with open(filepath, 'r') as f:
        content = f.read()

    glow_defs = """
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle

private val glowingShadow = Shadow(color = Color(0xCCFFFFFF), blurRadius = 8f)
private val glowingTextStyle = TextStyle(shadow = glowingShadow, color = Color.White, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
"""
    
    if "glowingShadow" not in content:
        content = content.replace("import java.util.Locale\n", "import java.util.Locale\n" + glow_defs)

    with open(filepath, 'w') as f:
        f.write(content)

fix()
