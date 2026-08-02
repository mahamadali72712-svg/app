import re

def update_dashboard():
    with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
        content = f.read()
    
    # Increase glow and sharpness
    new_glow = """val glowingWhite = Color(0xFFFFFFFF)
val glowingShadow = Shadow(color = Color(0xDDFFFFFF), blurRadius = 10f)
val glowingTextStyle = TextStyle(shadow = glowingShadow, color = Color.White, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)"""
    
    content = re.sub(
        r'val glowingWhite = Color\.White\s*val glowingShadow = Shadow\(color = Color\(0xAAFFFFFF\), blurRadius = 12f\)\s*val glowingTextStyle = TextStyle\(shadow = glowingShadow\)',
        new_glow,
        content
    )
    
    with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
        f.write(content)

update_dashboard()
