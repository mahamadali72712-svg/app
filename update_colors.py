import re

def update_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # Define new colors
    colors_block = """val ThemeGreen = Color(0xFF2E8B57)
val ThemeRed = Color(0xFFD32F2F)
val SoftGreen = Color(0xFFE9F5EC)
val SoftRedBg = Color(0xFFFFEBEE)
val WhitePure = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF333333)
val TextSecondary = Color(0xFF757575)"""

    # If it's PurchasesScreen.kt, replace the old color definitions
    if 'val LightGold = Color(0xFFFFD700)' in content:
        content = re.sub(
            r'val LightGold = Color\(0xFFFFD700\).*?val TextSecondary = Color\(0xFF757575\)',
            colors_block,
            content,
            flags=re.DOTALL
        )
    elif 'private val LightGold' in content: # If History screen has it
        content = re.sub(
            r'private val LightGold = Color\(0xFFFFD700\).*?private val TextSecondary = Color\(0xFF757575\)',
            colors_block,
            content,
            flags=re.DOTALL
        )
    else:
        # If deleted by sed earlier in History, just insert them after imports
        if 'PurchasesHistoryScreen.kt' in filepath:
            content = content.replace('// Reusing colors from PurchasesScreen', colors_block)

    # Replace variables
    content = content.replace('PurchaseBgGradient', 'PurchaseBgGradient')
    content = content.replace('HistoryBgGradient', 'HistoryBgGradient')
    
    # Update Gradient definition
    content = content.replace('0.0f to SoftPeach', '0.0f to SoftGreen')
    content = content.replace('0.5f to SoftRose', '0.5f to SoftRedBg')
    
    # Replace usages
    content = content.replace('LuxuryGold', 'ThemeGreen')
    content = content.replace('SoftPeach', 'SoftGreen')
    content = content.replace('SoftRose', 'SoftRedBg')
    content = content.replace('LightGold', 'ThemeGreen')
    
    # Let's add some red accents
    # For example, the delete button or cancel button can use ThemeRed
    content = content.replace('Color(0xFFD32F2F)', 'ThemeRed')

    with open(filepath, 'w') as f:
        f.write(content)

update_file('app/src/main/java/com/example/ui/screens/PurchasesScreen.kt')
update_file('app/src/main/java/com/example/ui/screens/PurchasesHistoryScreen.kt')
