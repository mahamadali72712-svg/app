with open('app/src/main/java/com/example/ui/screens/ReportsScreen.kt', 'r') as f:
    content = f.read()

# Just strip the weird prefix entirely, and put the proper package at top
content = content.replace('import androidx.compose.foundation.shape.RoundedCornerShapepackage com.example.ui.screensimport android.app.DatePickerDialog', 'package com.example.ui.screens\nimport androidx.compose.foundation.shape.RoundedCornerShape\nimport android.app.DatePickerDialog')

with open('app/src/main/java/com/example/ui/screens/ReportsScreen.kt', 'w') as f:
    f.write(content)
