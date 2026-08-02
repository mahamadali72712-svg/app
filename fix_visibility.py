import re

def update_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # Add private to the top level colors
    content = re.sub(r'^(val ThemeGreen.*)', r'private \1', content, flags=re.MULTILINE)
    content = re.sub(r'^(val ThemeRed.*)', r'private \1', content, flags=re.MULTILINE)
    content = re.sub(r'^(val SoftGreen.*)', r'private \1', content, flags=re.MULTILINE)
    content = re.sub(r'^(val SoftRedBg.*)', r'private \1', content, flags=re.MULTILINE)
    content = re.sub(r'^(val WhitePure.*)', r'private \1', content, flags=re.MULTILINE)
    content = re.sub(r'^(val TextPrimary.*)', r'private \1', content, flags=re.MULTILINE)
    content = re.sub(r'^(val TextSecondary.*)', r'private \1', content, flags=re.MULTILINE)
    content = re.sub(r'^(val PurchaseBgGradient.*)', r'private \1', content, flags=re.MULTILINE)
    content = re.sub(r'^(val HistoryBgGradient.*)', r'private \1', content, flags=re.MULTILINE)

    # In case there are duplicates like `private private`
    content = content.replace('private private', 'private')

    with open(filepath, 'w') as f:
        f.write(content)

update_file('app/src/main/java/com/example/ui/screens/PurchasesScreen.kt')
update_file('app/src/main/java/com/example/ui/screens/PurchasesHistoryScreen.kt')
