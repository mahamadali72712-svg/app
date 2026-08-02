with open('app/src/main/java/com/example/ui/screens/ReportsScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('import androidx.compose.foundation.shape.RoundedCornerShapepackage com.example.ui.screens', 'package com.example.ui.screens\nimport androidx.compose.foundation.shape.RoundedCornerShape\n')
content = content.replace('package com.example.ui.screensimport', 'package com.example.ui.screens\nimport')

with open('app/src/main/java/com/example/ui/screens/ReportsScreen.kt', 'w') as f:
    f.write(content)
