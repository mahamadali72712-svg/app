import re

def update_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # Add private to glowingShadow and glowingTextStyle
    content = re.sub(r'^(val glowingShadow.*)', r'private \1', content, flags=re.MULTILINE)
    content = re.sub(r'^(val glowingTextStyle.*)', r'private \1', content, flags=re.MULTILINE)
    content = re.sub(r'^(val glowingWhite.*)', r'private \1', content, flags=re.MULTILINE)
    
    # fix double private if exists
    content = content.replace('private private', 'private')
    
    # fix Unresolved reference 'copy'. This is because glowingTextStyle is a TextStyle, but in PurchasesScreen it says style = glowingTextStyle.copy(...)
    # Wait, TextStyle has copy(). Why did it say unresolved reference? Because glowingTextStyle was an Ambiguity error, so type inference failed. Once we fix ambiguity, copy() should resolve.

    with open(filepath, 'w') as f:
        f.write(content)

update_file('app/src/main/java/com/example/ui/screens/DashboardScreen.kt')
update_file('app/src/main/java/com/example/ui/screens/PurchasesScreen.kt')
update_file('app/src/main/java/com/example/ui/screens/PurchasesHistoryScreen.kt')
