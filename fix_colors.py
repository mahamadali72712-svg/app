import re

def fix(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # Revert to light theme colors
    content = content.replace('Color(0xFF152A20)', 'Color(0xFFE9F5EC)') # SoftGreen
    content = content.replace('Color(0xFF2A1515)', 'Color(0xFFFFEBEE)') # SoftRedBg
    content = content.replace('Color(0xFF161F28)', 'Color(0xFFFFFFFF)') # WhitePure
    content = content.replace('Color(0xFFFFFFFF)', 'Color(0xFF212121)') # TextPrimary (Will fix glowingTextStyle's Color.White separately)
    content = content.replace('Color(0xFFB0B0B0)', 'Color(0xFF757575)') # TextSecondary

    # But we want the glowingWhite / glowingTextStyle to still use White if it's white.
    # So let's refine the TextPrimary replace:
    content = re.sub(r'private val TextPrimary = Color\([^)]+\)', 'private val TextPrimary = Color(0xFF1A1A1A)', content)
    content = re.sub(r'private val TextSecondary = Color\([^)]+\)', 'private val TextSecondary = Color(0xFF757575)', content)
    
    # Fix the glowingTextStyle and glowingShadow
    glow_def = """private val glowingShadow = Shadow(color = Color(0x442E8B57), blurRadius = 8f)
private val glowingTextStyle = TextStyle(shadow = glowingShadow, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)"""
    
    content = re.sub(
        r'private val glowingShadow = .*?letterSpacing = 0\.5\.sp\)',
        glow_def,
        content,
        flags=re.DOTALL
    )

    with open(filepath, 'w') as f:
        f.write(content)

fix('app/src/main/java/com/example/ui/screens/PurchasesScreen.kt')
fix('app/src/main/java/com/example/ui/screens/PurchasesHistoryScreen.kt')
