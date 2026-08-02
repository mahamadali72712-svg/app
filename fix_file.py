import re
with open('app/src/main/java/com/example/ui/screens/ReportsScreen.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    line = line.strip()
    if not line:
        continue
    # clean out any weird characters? No, let's just make sure it's regular imports
    if line.startswith('import '):
        new_lines.append(line + '\n')
    elif line.startswith('package '):
        new_lines.append(line + '\n\n')
    elif line.startswith('@'):
        new_lines.append(line + '\n')
    else:
        # just append
        new_lines.append(line + '\n')

with open('app/src/main/java/com/example/ui/screens/ReportsScreen.kt', 'w') as f:
    f.writelines(new_lines)

