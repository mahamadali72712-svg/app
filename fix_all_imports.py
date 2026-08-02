import re
with open('app/src/main/java/com/example/ui/screens/ReportsScreen.kt', 'r') as f:
    content = f.read()

# Replace all occurrences of 'import ' that immediately follow a non-whitespace character with '\nimport '
content = re.sub(r'(?<=\S)import ', '\nimport ', content)
content = re.sub(r'(?<=\S)package ', '\npackage ', content)

with open('app/src/main/java/com/example/ui/screens/ReportsScreen.kt', 'w') as f:
    f.write(content)
