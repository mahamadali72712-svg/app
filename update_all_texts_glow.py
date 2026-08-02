import re

def add_glow(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # Find all Text(...) that have color = TextPrimary or color = ThemeGreen, and don't already have glowingTextStyle
    # It's easier to use a function
    
    def replacer(match):
        full_match = match.group(0)
        if 'glowingTextStyle' in full_match:
            return full_match
            
        # Upgrade Bold to ExtraBold
        if 'FontWeight.Bold' in full_match:
            full_match = full_match.replace('FontWeight.Bold', 'FontWeight.ExtraBold')
        elif 'fontWeight' not in full_match:
            # Insert fontWeight
            full_match = full_match.replace('color =', 'fontWeight = FontWeight.ExtraBold, color =')
            
        if 'color = TextPrimary' in full_match:
            return full_match.rstrip(') \n\t') + ', style = glowingTextStyle)'
        elif 'color = ThemeGreen' in full_match:
            return full_match.rstrip(') \n\t') + ', style = glowingTextStyle.copy(color = ThemeGreen))'
        return full_match

    # Simple regex to match Text(...) calls over multiple lines
    content = re.sub(r'Text\([^)]*color\s*=\s*(TextPrimary|ThemeGreen)[^)]*\)', replacer, content)

    with open(filepath, 'w') as f:
        f.write(content)

add_glow('app/src/main/java/com/example/ui/screens/PurchasesScreen.kt')
add_glow('app/src/main/java/com/example/ui/screens/PurchasesHistoryScreen.kt')
