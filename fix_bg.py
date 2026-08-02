with open('app/src/main/java/com/example/ui/navigation/Navigation.kt', 'r') as f:
    nav_content = f.read()

import re

nav_content = re.sub(
    r'val MainBgGradient = Brush\.verticalGradient\([\s\S]*?Color\(0xFF131131\)\n\)',
    '''val MainBgGradient = Brush.verticalGradient(
    0.0f to Color(0xFF1E3C72),
    0.4f to Color(0xFF2A5298),
    0.7f to Color(0xFF2980B9),
    1.0f to Color(0xFF1E3C72)
)''',
    nav_content
)

# And make the radial gradients a bit lighter and cyan-ish for that "light wavy" effect
nav_content = nav_content.replace('Color(0x775D35D3)', 'Color(0x6600C9FF)')
nav_content = nav_content.replace('Color(0x553070E0)', 'Color(0x5592FE9D)')

# Adjust bottom bar color slightly to match the new light blue background
# It was Color(0xFF22245C). Let's make it more matching like Color(0xFF1B3B6D)
nav_content = nav_content.replace('Color(0xFF22245C)', 'Color(0xFF1A365D)')

with open('app/src/main/java/com/example/ui/navigation/Navigation.kt', 'w') as f:
    f.write(nav_content)

