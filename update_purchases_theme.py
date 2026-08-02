import re

def update_purchases(filepath):
    with open(filepath, 'r') as f:
        content = f.read()
    
    # 1. Add glowing text definitions right after imports
    glow_defs = """
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle

val glowingShadow = Shadow(color = Color(0xCCFFFFFF), blurRadius = 8f)
val glowingTextStyle = TextStyle(shadow = glowingShadow, color = Color.White, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
"""
    
    if "val glowingShadow" not in content:
        content = content.replace("import java.util.UUID\n", "import java.util.UUID\n" + glow_defs)

    # 2. Update Colors to Dark Theme
    content = re.sub(r'private val SoftGreen = Color\(0xFFE9F5EC\)', 'private val SoftGreen = Color(0xFF152A20)', content)
    content = re.sub(r'private val SoftRedBg = Color\(0xFFFFEBEE\)', 'private val SoftRedBg = Color(0xFF2A1515)', content)
    content = re.sub(r'private val WhitePure = Color\(0xFFFFFFFF\)', 'private val WhitePure = Color(0xFF161F28)', content) # Dark card bg
    content = re.sub(r'private val TextPrimary = Color\(0xFF333333\)', 'private val TextPrimary = Color(0xFFFFFFFF)', content)
    content = re.sub(r'private val TextSecondary = Color\(0xFF757575\)', 'private val TextSecondary = Color(0xFFB0B0B0)', content)
    
    # 3. Add glowingTextStyle to prominent text elements (like TextPrimary)
    # This regex looks for Text(..., color = TextPrimary, ...) and adds style = glowingTextStyle
    
    # Instead of complex regex, let's just make TextPrimary white and add a wrapper or regex substitution
    # For now, changing the TextPrimary to White and applying style to all titles and important values.
    
    # Add style = glowingTextStyle to text elements that use TextPrimary or ThemeGreen as color
    content = re.sub(
        r'Text\(\s*"([^"]+)"\s*,\s*fontSize\s*=\s*([0-9]+)\.sp\s*,\s*fontWeight\s*=\s*FontWeight\.(Bold|ExtraBold)\s*,\s*color\s*=\s*TextPrimary\s*\)',
        r'Text("\1", fontSize = \2.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary, style = glowingTextStyle)',
        content
    )
    
    # For dynamic text with variables
    content = re.sub(
        r'Text\(\s*([a-zA-Z0-9_\.\(\)]+)\s*,\s*fontSize\s*=\s*([0-9]+)\.sp\s*,\s*fontWeight\s*=\s*FontWeight\.(Bold|ExtraBold)\s*,\s*color\s*=\s*TextPrimary\s*\)',
        r'Text(\1, fontSize = \2.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary, style = glowingTextStyle)',
        content
    )

    # Also apply to ThemeGreen text if it's bold
    content = re.sub(
        r'Text\(\s*([a-zA-Z0-9_\.\(\)]+)\s*,\s*fontSize\s*=\s*([0-9]+)\.sp\s*,\s*fontWeight\s*=\s*FontWeight\.(Bold|ExtraBold)\s*,\s*color\s*=\s*ThemeGreen\s*\)',
        r'Text(\1, fontSize = \2.sp, fontWeight = FontWeight.ExtraBold, color = ThemeGreen, style = glowingTextStyle.copy(color = ThemeGreen))',
        content
    )
    
    with open(filepath, 'w') as f:
        f.write(content)

update_purchases('app/src/main/java/com/example/ui/screens/PurchasesScreen.kt')
update_purchases('app/src/main/java/com/example/ui/screens/PurchasesHistoryScreen.kt')
