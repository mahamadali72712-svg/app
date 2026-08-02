with open('app/src/main/java/com/example/ui/screens/ReportsScreen.kt', 'r') as f:
    content = f.read()

# find the first 'import android.content.Context' and take everything from there
idx = content.find('import android.content.Context')
if idx != -1:
    new_content = 'package com.example.ui.screens\nimport androidx.compose.foundation.shape.RoundedCornerShape\nimport android.app.DatePickerDialog\n' + content[idx:]
    with open('app/src/main/java/com/example/ui/screens/ReportsScreen.kt', 'w') as f:
        f.write(new_content)
