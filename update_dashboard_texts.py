import re

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    content = f.read()

# Replace FontWeight.Bold with FontWeight.ExtraBold
# Replace FontWeight.Medium with FontWeight.Bold for better contrast
content = content.replace('FontWeight.Bold', 'FontWeight.ExtraBold')
content = content.replace('FontWeight.Medium', 'FontWeight.Bold')

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
    f.write(content)
