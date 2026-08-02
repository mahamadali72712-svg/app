import re

def add_glow(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

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
        elif 'color = ThemeRed' in full_match:
            return full_match.rstrip(') \n\t') + ', style = glowingTextStyle.copy(color = ThemeRed))'
        elif 'color = WhitePure' in full_match:
            return full_match.rstrip(') \n\t') + ', style = glowingTextStyle)'
        return full_match

    content = re.sub(r'Text\([^)]*color\s*=\s*(TextPrimary|ThemeGreen|ThemeRed|WhitePure)[^)]*\)', replacer, content)

    with open(filepath, 'w') as f:
        f.write(content)

add_glow('app/src/main/java/com/example/ui/screens/PurchasesScreen.kt')
add_glow('app/src/main/java/com/example/ui/screens/PurchasesHistoryScreen.kt')
