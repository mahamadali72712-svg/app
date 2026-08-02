package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    var isProcessing by remember { mutableStateOf(false) }
    
    val syncLogs by viewModel.syncLogs.collectAsStateWithLifecycle()
    
    var previewData by remember { mutableStateOf<SyncPreview?>(null) }
    var previewUri by remember { mutableStateOf<Uri?>(null) }
    
    val exportFullLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) { e.printStackTrace() }
            isProcessing = true
            viewModel.exportData(context, uri, false, {
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
            viewModel.exportData(context, uri, true, {
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
            previewUri = uri
            viewModel.getSyncPreview(uri) { preview ->
                isProcessing = false
                if (preview != null) {
                    previewData = preview
                } else {
                    Toast.makeText(context, "حدث خطأ أثناء قراءة الملف", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الإعدادات والمزامنة") },
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
                .padding(16.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("المزامنة (تصدير/استيراد)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { exportDeltaLauncher.launch("Sync_Delta_${System.currentTimeMillis()}.xlsx") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing
                    ) {
                        Text("تصدير التغييرات فقط (Delta)")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { exportFullLauncher.launch("Sync_Full_${System.currentTimeMillis()}.xlsx") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing
                    ) {
                        Text("تصدير نسخة كاملة (Full)")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { importLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/octet-stream")) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        enabled = !isProcessing
                    ) {
                        Text("استيراد بيانات")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("النسخ الاحتياطي", fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("استعادة قبل آخر استيراد (تراجع)")
                    }
                }
            }
            
            if (isProcessing) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("سجل المزامنة", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            
            if (syncLogs.isEmpty()) {
                Text("لا توجد عمليات مزامنة سابقة", color = Color.Gray)
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(syncLogs) { log ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(log.syncDate))
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(if (log.syncType == "IMPORT") "📥 استيراد" else "📤 تصدير", fontWeight = FontWeight.Bold)
                                    Text(dateStr, color = Color.Gray, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("المصدر/الجهاز: ${log.sourceDevice ?: "غير معروف"}", fontSize = 14.sp)
                                if (log.syncType == "IMPORT") {
                                    Text("أضيف: ${log.recordsAdded} | عُدل: ${log.recordsUpdated} | حُذف: ${log.recordsDeleted} | تخطي: ${log.recordsSkipped}", fontSize = 12.sp, color = Color.DarkGray)
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
                title = { Text("✅ تم الاستيراد بنجاح") },
                text = {
                    Column {
                        Text("أضيف: ${importResultDialog!!.added}")
                        Text("عُدّل: ${importResultDialog!!.updated}")
                        Text("تُخطي: ${importResultDialog!!.skipped}")
                        Text("حُذف: ${importResultDialog!!.deleted}")
                    }
                },
                confirmButton = {
                    Button(onClick = { 
                        importResultDialog = null
                        previewData = null
                        previewUri = null
                    }) {
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
                title = { Text("تأكيد الاستيراد") },
                text = {
                    Column {
                        Text("الملف من: ${previewData!!.sourceDevice}")
                        Text("التاريخ: ${previewData!!.date}")
                        Text("عدد السجلات الإجمالي: ${previewData!!.totalRecords}", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        previewData!!.details.forEach { (table, count) ->
                            Text("- $table: $count", fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("هل تريد متابعة الدمج الذكي؟", color = MaterialTheme.colorScheme.primary)
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.importData(context, previewUri!!)
                    }) {
                        Text("متابعة واستيراد")
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
