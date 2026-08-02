with open('app/src/main/java/com/example/ui/screens/ReportsScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('package com.example.ui.screensimport', 'package com.example.ui.screens\nimport')
content = content.replace('import androidx.compose.foundation.shape.RoundedCornerShapeimport', 'import androidx.compose.foundation.shape.RoundedCornerShape\nimport')
content = content.replace('import android.app.DatePickerDialogimport', 'import android.app.DatePickerDialog\nimport')
content = content.replace('import android.content.Contextimport', 'import android.content.Context\nimport')
content = content.replace('import android.widget.Toastimport', 'import android.widget.Toast\nimport')
content = content.replace('import androidx.activity.compose.rememberLauncherForActivityResultimport', 'import androidx.activity.compose.rememberLauncherForActivityResult\nimport')
content = content.replace('import androidx.activity.result.contract.ActivityResultContractsimport', 'import androidx.activity.result.contract.ActivityResultContracts\nimport')
content = content.replace('import androidx.compose.animation.animateContentSizeimport', 'import androidx.compose.animation.animateContentSize\nimport')
content = content.replace('import androidx.compose.animation.core.Springimport', 'import androidx.compose.animation.core.Spring\nimport')

with open('app/src/main/java/com/example/ui/screens/ReportsScreen.kt', 'w') as f:
    f.write(content)
