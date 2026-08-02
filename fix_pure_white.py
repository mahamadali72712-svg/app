import re

def fix(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # Revert WhitePure to actual White
    content = content.replace('Color(0xFF212121)', 'Color(0xFFFFFFFF)')
    
    # TextPrimary to a very dark green or black
    content = re.sub(r'private val TextPrimary = Color\([^)]+\)', 'private val TextPrimary = Color(0xFF111111)', content)
    
    # SoftGreen/Red
    content = re.sub(r'private val SoftGreen = Color\([^)]+\)', 'private val SoftGreen = Color(0xFFE9F5EC)', content)
    content = re.sub(r'private val SoftRedBg = Color\([^)]+\)', 'private val SoftRedBg = Color(0xFFFFEBEE)', content)
    
    # ThemeGreen
    content = re.sub(r'private val ThemeGreen = Color\([^)]+\)', 'private val ThemeGreen = Color(0xFF2E8B57)', content)

    with open(filepath, 'w') as f:
        f.write(content)

fix('app/src/main/java/com/example/ui/screens/PurchasesScreen.kt')
fix('app/src/main/java/com/example/ui/screens/PurchasesHistoryScreen.kt')
