import re

def add_imports(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    imports = [
        'import androidx.compose.foundation.clickable',
        'import androidx.compose.foundation.verticalScroll',
        'import androidx.compose.foundation.rememberScrollState',
        'import com.example.data.local.Product'
    ]

    for imp in imports:
        if imp not in content:
            content = content.replace('import androidx.compose.material3.*', 'import androidx.compose.material3.*\n' + imp)

    with open(filepath, 'w') as f:
        f.write(content)

add_imports('app/src/main/java/com/example/ui/screens/SalesScreen.kt')
add_imports('app/src/main/java/com/example/ui/screens/PurchasesScreen.kt')
