with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    content = f.read()

import re

# Update QuickActionButton colors
content = content.replace('Color(0xFF22245C)', 'Color(0xFF28548A)')
content = content.replace('Color(0xFF45498A)', 'Color(0xFF3B71B5)')

# Make the QuickActionButtons slightly taller just to fit the text perfectly, 
# 56.dp is good, but maybe increase slightly to 60.dp? The user wanted 
# the top cards smaller to fit everything. Let's keep 56.dp.

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
    f.write(content)
