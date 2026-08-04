package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodels.StoreViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.data.sync.SyncPreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: StoreViewModel, navController: androidx.navigation.NavController) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("store_settings", android.content.Context.MODE_PRIVATE) }

    var syncMode by remember { mutableStateOf(prefs.getString("sync_mode", "SMART_MERGE") ?: "SMART_MERGE") }
    var showPreview by remember { mutableStateOf(prefs.getBoolean("show_preview", true)) }
    var autoBackup by remember { mutableStateOf(prefs.getBoolean("auto_backup", true)) }
    var storeDomain by remember { mutableStateOf(prefs.getString("store_domain", "عام / متجر مخصص") ?: "عام / متجر مخصص") }

    var exportPeriod by remember { mutableStateOf("ALL") } // "ALL", "TODAY", "WEEK", "MONTH", "CUSTOM"
    var customFromDate by remember { mutableStateOf<Long?>(null) }
    var customToDate by remember { mutableStateOf<Long?>(null) }

    var isProcessing by remember { mutableStateOf(false) }
    var showStoreDomainDialog by remember { mutableStateOf(false) }

    val syncLogs by viewModel.syncLogs.collectAsStateWithLifecycle()

    var previewData by remember { mutableStateOf<SyncPreview?>(null) }
    var previewUri by remember { mutableStateOf<Uri?>(null) }

    fun calculateExportTimeRange(
        period: String,
        customFrom: Long?,
        customTo: Long?
    ): Pair<Long?, Long?> {
        val cal = java.util.Calendar.getInstance()
        val now = System.currentTimeMillis()
        return when (period) {
            "TODAY" -> {
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                Pair(cal.timeInMillis, now)
            }
            "WEEK" -> {
                cal.add(java.util.Calendar.DAY_OF_YEAR, -7)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                Pair(cal.timeInMillis, now)
            }
            "MONTH" -> {
                cal.add(java.util.Calendar.DAY_OF_YEAR, -30)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                Pair(cal.timeInMillis, now)
            }
            "CUSTOM" -> {
                if (customFrom != null && customTo != null) {
                    val fromCal = java.util.Calendar.getInstance().apply {
                        timeInMillis = customFrom
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    val toCal = java.util.Calendar.getInstance().apply {
                        timeInMillis = customTo
                        set(java.util.Calendar.HOUR_OF_DAY, 23)
                        set(java.util.Calendar.MINUTE, 59)
                        set(java.util.Calendar.SECOND, 59)
                        set(java.util.Calendar.MILLISECOND, 999)
                    }
                    Pair(fromCal.timeInMillis, toCal.timeInMillis)
                } else {
                    Pair(null, null)
                }
            }
            else -> Pair(null, null)
        }
    }

    val exportFullLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) { e.printStackTrace() }
            isProcessing = true
            val (fromTime, toTime) = calculateExportTimeRange(exportPeriod, customFromDate, customToDate)
            viewModel.exportData(context, uri, false, fromTime, toTime, {
                isProcessing = false
                Toast.makeText(context, "تم تصدير نسخة كاملة بنجاح", Toast.LENGTH_SHORT).show()
            }, {
                isProcessing = false
                Toast.makeText(context, "حدث خطأ أثناء التصدير", Toast.LENGTH_SHORT).show()
            })
        }
    }

    val exportDeltaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) { e.printStackTrace() }
            isProcessing = true
            val (fromTime, toTime) = calculateExportTimeRange(exportPeriod, customFromDate, customToDate)
            viewModel.exportData(context, uri, true, fromTime, toTime, {
                isProcessing = false
                Toast.makeText(context, "تم تصدير التغييرات بنجاح", Toast.LENGTH_SHORT).show()
            }, {
                isProcessing = false
                Toast.makeText(context, "حدث خطأ أثناء التصدير", Toast.LENGTH_SHORT).show()
            })
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) { e.printStackTrace() }
            isProcessing = true
            if (showPreview) {
                previewUri = uri
                viewModel.getSyncPreview(uri) { preview ->
                    isProcessing = false
                    if (preview != null) {
                        previewData = preview
                    } else {
                        Toast.makeText(context, "حدث خطأ أثناء قراءة الملف", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                viewModel.importData(context, uri)
                isProcessing = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إعدادات التحكم والمزامنة", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card 1: Sync Strategy Option (Radio Buttons)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.AltRoute, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("استراتيجية دمج البيانات عند الاستيراد", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Option 1: Smart Merge
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                syncMode = "SMART_MERGE"
                                prefs.edit().putString("sync_mode", "SMART_MERGE").apply()
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = syncMode == "SMART_MERGE",
                            onClick = {
                                syncMode = "SMART_MERGE"
                                prefs.edit().putString("sync_mode", "SMART_MERGE").apply()
                            }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text("دمج ذكي (تحديث الأحدث تلقائياً)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("يتم دمج البيانات وإبقاء التعديلات الأحدث في المنتجات والمخزون والحسابات.", fontSize = 12.sp, color = Color.Gray)
                        }
                    }

                    // Option 2: Skip Existing
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                syncMode = "SKIP_EXISTING"
                                prefs.edit().putString("sync_mode", "SKIP_EXISTING").apply()
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = syncMode == "SKIP_EXISTING",
                            onClick = {
                                syncMode = "SKIP_EXISTING"
                                prefs.edit().putString("sync_mode", "SKIP_EXISTING").apply()
                            }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text("تخطي السجلات الموجودة مسبقاً", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("إضافة المنتجات والعملاء الجدد فقط، والتغاضي عن السجلات الموجودة حالياً.", fontSize = 12.sp, color = Color.Gray)
                        }
                    }

                    // Option 3: Force Overwrite
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                syncMode = "FORCE_OVERWRITE"
                                prefs.edit().putString("sync_mode", "FORCE_OVERWRITE").apply()
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = syncMode == "FORCE_OVERWRITE",
                            onClick = {
                                syncMode = "FORCE_OVERWRITE"
                                prefs.edit().putString("sync_mode", "FORCE_OVERWRITE").apply()
                            }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text("استبدال وتحديث كامل بالملف المستورد", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("استبدال كل السجلات بالبيانات القادمة من الملف فوراً.", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }

            // Card 2: Control Preferences (Switches)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("ميزات التحكم في الاستيراد", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Switch 1: Preview Data
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("معاينة مراجعة البيانات قبل الدمج", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("إظهار نافذة تحتوي على تفاصيل أعداد السجلات بكل جدول قبل الاعتماد.", fontSize = 12.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = showPreview,
                            onCheckedChange = {
                                showPreview = it
                                prefs.edit().putBoolean("show_preview", it).apply()
                            }
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    // Switch 2: Auto Backup
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("نسخ احتياطي تلقائي قبل كل استيراد", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("إنشاء نقطة استعادة تلقائياً قبل أي عملية دمج لتفادي الأخطاء.", fontSize = 12.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = autoBackup,
                            onCheckedChange = {
                                autoBackup = it
                                prefs.edit().putBoolean("auto_backup", it).apply()
                            }
                        )
                    }
                }
            }

            // Card 3: Store Domain Customization
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("تخصيص النشاط التجاري والتصنيفات", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("النشاط التجاري الحالي: $storeDomain", fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { showStoreDomainDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.Category, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تغيير نوع النشاط والتصنيفات السريعة", fontSize = 13.sp)
                    }
                }
            }

            // Card 4: Actions (Export / Import / Rollback)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("المزامنة (تصدير / استيراد)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("تحديد الفترة الزمنية للتصدير:", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val periods = listOf(
                            "ALL" to "الكل",
                            "TODAY" to "يوم",
                            "WEEK" to "أسبوع",
                            "MONTH" to "شهر",
                            "CUSTOM" to "مخصص"
                        )
                        periods.forEach { (key, label) ->
                            FilterChip(
                                selected = exportPeriod == key,
                                onClick = { exportPeriod = key },
                                label = { Text(label, fontSize = 11.5.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (exportPeriod == "CUSTOM") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                            OutlinedButton(
                                onClick = {
                                    val cal = java.util.Calendar.getInstance()
                                    if (customFromDate != null) cal.timeInMillis = customFromDate!!
                                    android.app.DatePickerDialog(
                                        context,
                                        { _, year, month, day ->
                                            val selected = java.util.Calendar.getInstance().apply { set(year, month, day) }
                                            customFromDate = selected.timeInMillis
                                        },
                                        cal.get(java.util.Calendar.YEAR),
                                        cal.get(java.util.Calendar.MONTH),
                                        cal.get(java.util.Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (customFromDate != null) "من: ${dateFormat.format(Date(customFromDate!!))}" else "من تاريخ",
                                    fontSize = 12.sp
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    val cal = java.util.Calendar.getInstance()
                                    if (customToDate != null) cal.timeInMillis = customToDate!!
                                    android.app.DatePickerDialog(
                                        context,
                                        { _, year, month, day ->
                                            val selected = java.util.Calendar.getInstance().apply { set(year, month, day) }
                                            customToDate = selected.timeInMillis
                                        },
                                        cal.get(java.util.Calendar.YEAR),
                                        cal.get(java.util.Calendar.MONTH),
                                        cal.get(java.util.Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (customToDate != null) "إلى: ${dateFormat.format(Date(customToDate!!))}" else "إلى تاريخ",
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val suffix = when (exportPeriod) {
                                "TODAY" -> "Today"
                                "WEEK" -> "Week"
                                "MONTH" -> "Month"
                                "CUSTOM" -> "Custom"
                                else -> "All"
                            }
                            exportDeltaLauncher.launch("Sync_Delta_${suffix}_${System.currentTimeMillis()}.xlsx")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تصدير التغييرات فقط (Delta)")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val suffix = when (exportPeriod) {
                                "TODAY" -> "Today"
                                "WEEK" -> "Week"
                                "MONTH" -> "Month"
                                "CUSTOM" -> "Custom"
                                else -> "All"
                            }
                            exportFullLauncher.launch("Sync_Full_${suffix}_${System.currentTimeMillis()}.xlsx")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تصدير نسخة كاملة (Full)")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { importLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/octet-stream")) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        enabled = !isProcessing,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("استيراد بيانات من ملف Excel")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("الاستعادة والتراجع", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            isProcessing = true
                            viewModel.restoreBackup(context) { success ->
                                isProcessing = false
                                if (!success) {
                                    Toast.makeText(context, "فشل استعادة النسخة الاحتياطية", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("استعادة قبل آخر استيراد (تراجع فوري)")
                    }
                }
            }

            if (isProcessing) {
                Spacer(modifier = Modifier.height(8.dp))
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            // Card 5: Sync Audit History
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("سجل وتدقيق المزامنة", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (syncLogs.isEmpty()) {
                    Text("لا توجد عمليات مزامنة سابقة حتى الآن", color = Color.Gray, fontSize = 13.sp)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        syncLogs.take(10).forEach { log ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(log.syncDate))
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            if (log.syncType == "IMPORT") "📥 استيراد بيانات" else "📤 تصدير بيانات",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        )
                                        Text(dateStr, color = Color.Gray, fontSize = 11.5.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("المصدر / الجهاز: ${log.sourceDevice ?: "غير محدد"}", fontSize = 12.5.sp)
                                    if (log.syncType == "IMPORT") {
                                        Text(
                                            "أضيف: ${log.recordsAdded} | عُدل: ${log.recordsUpdated} | حُذف: ${log.recordsDeleted} | تخطي: ${log.recordsSkipped}",
                                            fontSize = 12.sp,
                                            color = Color.DarkGray
                                        )
                                    }
                                    if (!log.isSuccess) {
                                        Text("فشل: ${log.errorMessage}", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showStoreDomainDialog) {
        com.example.ui.screens.StoreDomainSelectionDialog(
            onDismiss = { showStoreDomainDialog = false },
            onApplyPreset = { domainTitle, sampleCategories ->
                viewModel.applyDomainCategories(sampleCategories) {
                    storeDomain = domainTitle
                    prefs.edit().putString("store_domain", domainTitle).apply()
                    showStoreDomainDialog = false
                    Toast.makeText(context, "تم تطبيق تصنيفات نشاط: $domainTitle", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (previewData != null && previewUri != null) {
        val importState by viewModel.importState.collectAsStateWithLifecycle()
        var importResultDialog by remember { mutableStateOf<com.example.data.sync.SyncResult?>(null) }

        when (val state = importState) {
            is com.example.data.sync.ImportState.Loading -> {
                ImportProgressDialog(state = state)
            }
            is com.example.data.sync.ImportState.Success -> {
                importResultDialog = state.result
                viewModel.resetImportState()
                previewData = null
                previewUri = null
            }
            is com.example.data.sync.ImportState.Error -> {
                Toast.makeText(context, "خطأ: ${state.message}", Toast.LENGTH_SHORT).show()
                viewModel.resetImportState()
                previewData = null
                previewUri = null
            }
            else -> {}
        }

        if (importResultDialog != null) {
            AlertDialog(
                onDismissRequest = {
                    importResultDialog = null
                    previewData = null
                    previewUri = null
                },
                title = { Text("✅ تم الاستيراد والدمج بنجاح") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("السجلات المضافة: ${importResultDialog!!.added}")
                        Text("السجلات المحدثة: ${importResultDialog!!.updated}")
                        Text("السجلات المتخطاة: ${importResultDialog!!.skipped}")
                        Text("السجلات المحذوفة: ${importResultDialog!!.deleted}")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            importResultDialog = null
                            previewData = null
                            previewUri = null
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("حسناً")
                    }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = {
                    previewData = null
                    previewUri = null
                },
                title = { Text("مراجعة ومعاينة البيانات قبل الدمج") },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("مصدر الملف: ${previewData!!.sourceDevice}", fontSize = 13.sp)
                        Text("التاريخ والوقت: ${previewData!!.date}", fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("إجمالي السجلات بالملف: ${previewData!!.totalRecords}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        previewData!!.details.forEach { (table, count) ->
                            val arabicTableName = when (table.lowercase(Locale.ROOT)) {
                                "product_categories" -> "تصنيفات المنتجات"
                                "products" -> "المنتجات والمخزون"
                                "customers" -> "العملاء والحسابات"
                                "suppliers" -> "الموردين والحسابات"
                                "sales_invoices" -> "فواتير المبيعات"
                                "sales_invoice_items" -> "تفاصيل فواتير المبيعات"
                                "purchase_invoices" -> "فواتير المشتريات"
                                "purchase_invoice_items" -> "تفاصيل فواتير المشتريات"
                                "expenses" -> "المصاريف والنفقات"
                                "supplier_payments" -> "سداد الموردين"
                                "customer_payments" -> "تحصيل العملاء"
                                "cash_movements" -> "حركات الصندوق الخزينة"
                                else -> table
                            }
                            Text("• $arabicTableName: $count سجل", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val modeText = when (syncMode) {
                                "SKIP_EXISTING" -> "سيتم إضافة السجلات الجديدة فقط وتخطي أي سجل موجود مسبقاً."
                                "FORCE_OVERWRITE" -> "سيتم استبدال وتحديث كل السجلات بالبيانات القادمة من الملف فوراً."
                                else -> "سيتم إجراء دمج ذكي مع تحديث الكميات والأسعار والتصنيفات تلقائياً إلى أحدث حالة مستوردة."
                            }
                            Text(
                                modeText,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.importData(context, previewUri!!)
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("تأكيد ومتابعة الاستيراد")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        previewData = null
                        previewUri = null
                    }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}


@Composable
fun ImportProgressDialog(
    state: com.example.data.sync.ImportState.Loading
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = { /* non-dismissible */ },
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier.padding(24.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "جاري استيراد الملف...",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(16.dp))

                LinearProgressIndicator(
                    progress = { state.percent / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                Text("${state.percent}%")
                Spacer(Modifier.height(12.dp))

                Text(
                    state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))

                Text(
                    "الرجاء عدم إغلاق التطبيق",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
