package com.example.data.sync

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Settings
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.AppDatabase
import com.example.data.local.SyncLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dhatim.fastexcel.Workbook
import org.dhatim.fastexcel.reader.ReadableWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.stream.Collectors

data class SyncPreview(
    val sourceDevice: String,
    val date: String,
    val totalRecords: Int,
    val details: Map<String, Int>
)

data class SyncResult(
    val added: Int,
    val updated: Int,
    val skipped: Int,
    val deleted: Int
)

sealed class ImportState {
    object Idle : ImportState()
    data class Loading(val percent: Int, val message: String) : ImportState()
    data class Success(val result: SyncResult) : ImportState()
    data class Error(val message: String) : ImportState()
}

class SyncEngine(private val context: Context, private val db: AppDatabase) {

    private val deviceId: String by lazy {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN_DEVICE"
    }

    private val tablesToSync = listOf(
        "product_categories",
        "products",
        "customers",
        "suppliers",
        "sales_invoices",
        "sales_invoice_items",
        "purchase_invoices",
        "purchase_invoice_items",
        "expenses",
        "supplier_payments",
        "customer_payments",
        "cash_movements"
    )

    suspend fun exportData(uri: Uri, isDelta: Boolean = false, fromTime: Long? = null, toTime: Long? = null) {
        withContext(Dispatchers.IO) {
            val tempFile = File(context.cacheDir, "export_temp_${System.currentTimeMillis()}.xlsx")
            try {
                FileOutputStream(tempFile).use { os ->
                    val wb = Workbook(os, "StoreApp", "1.0")

                    // Meta sheet
                    val metaSheet = wb.newWorksheet("__META")
                    metaSheet.value(0, 0, "export_id")
                    metaSheet.value(0, 1, UUID.randomUUID().toString())
                    metaSheet.value(1, 0, "date")
                    metaSheet.value(1, 1, SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                    metaSheet.value(2, 0, "source_device")
                    metaSheet.value(2, 1, deviceId)
                    metaSheet.value(3, 0, "format_version")
                    metaSheet.value(3, 1, "2.0")
                    if (fromTime != null && toTime != null) {
                        metaSheet.value(4, 0, "filter_from")
                        metaSheet.value(4, 1, fromTime.toString())
                        metaSheet.value(5, 0, "filter_to")
                        metaSheet.value(5, 1, toTime.toString())
                    }

                    val writableDb = db.openHelper.writableDatabase

                    tablesToSync.forEach { tableName ->
                        val sheet = wb.newWorksheet(tableName)

                        val colMetaMap = getTableColumnMeta(writableDb, tableName)
                        val hasUpdatedAt = colMetaMap.containsKey("updatedat")
                        val hasDate = colMetaMap.containsKey("date")

                        var queryStr = if (isDelta) {
                            "SELECT * FROM $tableName WHERE (syncStatus = 0 OR syncStatus = 2)"
                        } else {
                            "SELECT * FROM $tableName WHERE 1=1"
                        }

                        if (fromTime != null && toTime != null) {
                            if (hasUpdatedAt) {
                                queryStr += " AND (updatedAt >= $fromTime AND updatedAt <= $toTime)"
                            } else if (hasDate) {
                                queryStr += " AND (date >= $fromTime AND date <= $toTime)"
                            }
                        }

                        writableDb.query(queryStr).use { cursor ->
                            val colNames = cursor.columnNames
                            // Write headers
                            for (i in colNames.indices) {
                                sheet.value(0, i, colNames[i])
                            }

                            var row = 1
                            while (cursor.moveToNext()) {
                                for (i in colNames.indices) {
                                    val type = cursor.getType(i)
                                    when (type) {
                                        Cursor.FIELD_TYPE_INTEGER -> sheet.value(row, i, cursor.getLong(i))
                                        Cursor.FIELD_TYPE_FLOAT -> sheet.value(row, i, cursor.getDouble(i))
                                        Cursor.FIELD_TYPE_STRING -> sheet.value(row, i, cursor.getString(i) ?: "")
                                        Cursor.FIELD_TYPE_NULL -> sheet.value(row, i, "")
                                        else -> sheet.value(row, i, cursor.getString(i) ?: "")
                                    }
                                }
                                row++
                            }
                        }
                    }

                    wb.finish()
                    os.flush()
                }

                context.contentResolver.openOutputStream(uri, "rwt")?.use { targetOs ->
                    tempFile.inputStream().use { input ->
                        input.copyTo(targetOs)
                    }
                    targetOs.flush()
                }

                if (isDelta) {
                    val writableDb = db.openHelper.writableDatabase
                    tablesToSync.forEach { tableName ->
                        writableDb.execSQL("UPDATE $tableName SET syncStatus = 1 WHERE syncStatus = 0 OR syncStatus = 2")
                    }
                }

                db.syncDao().insertSyncLog(
                    SyncLog(
                        syncType = "EXPORT",
                        sourceDevice = deviceId,
                        isSuccess = true
                    )
                )
            } catch (e: Exception) {
                android.util.Log.e("Export", "Error exporting data", e)
                db.syncDao().insertSyncLog(
                    SyncLog(
                        syncType = "EXPORT",
                        sourceDevice = deviceId,
                        isSuccess = false,
                        errorMessage = e.message
                    )
                )
                throw e
            } finally {
                tempFile.delete()
            }
        }
    }

    suspend fun getPreview(uri: Uri): SyncPreview {
        return withContext(Dispatchers.IO) {
            val tempFile = File(context.cacheDir, "preview_temp_${System.currentTimeMillis()}.xlsx")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                if (!tempFile.exists() || tempFile.length() == 0L) {
                    return@withContext SyncPreview("خطأ", "ملف فارغ", 0, emptyMap())
                }

                ReadableWorkbook(tempFile).use { wb ->
                    val sheetsList = wb.sheets.collect(Collectors.toList())
                    var sourceDevice = "غير معروف"
                    var date = "غير معروف"

                    val metaSheet = sheetsList.find { it.name.equals("__META", ignoreCase = true) }
                    if (metaSheet != null) {
                        try {
                            metaSheet.openStream().use { stream ->
                                stream.forEach { row ->
                                    val cell0 = row.getCell(0)
                                    val cell1 = row.getCell(1)
                                    val key = safeCellText(cell0).trim().lowercase(Locale.ROOT)
                                    val value = safeCellText(cell1).trim()
                                    if (key == "source_device" || key == "sourcedevice" || key.contains("source_device")) {
                                        sourceDevice = value
                                    }
                                    if (key == "date" || key.contains("date")) {
                                        date = value
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("SyncPreview", "Error reading __META", e)
                        }
                    }

                    var totalRecords = 0
                    val details = mutableMapOf<String, Int>()

                    tablesToSync.forEach { tableName ->
                        val sheet = sheetsList.find { it.name.equals(tableName, ignoreCase = true) }
                        if (sheet != null) {
                            try {
                                sheet.openStream().use { stream ->
                                    val count = (stream.count() - 1).toInt()
                                    if (count > 0) {
                                        details[tableName] = count
                                        totalRecords += count
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("SyncPreview", "Error counting sheet $tableName", e)
                            }
                        }
                    }

                    SyncPreview(sourceDevice, date, totalRecords, details)
                }
            } catch (e: Exception) {
                android.util.Log.e("SyncPreview", "Error reading preview", e)
                SyncPreview("خطأ", "فشل قراءة الملف", 0, emptyMap())
            } finally {
                tempFile.delete()
            }
        }
    }

    suspend fun restoreBackup(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val dbFile = context.getDatabasePath("furniture_store_db")
                val backupFile = File(context.filesDir, "furniture_store_db_backup.db")
                if (backupFile.exists()) {
                    db.close()
                    backupFile.copyTo(dbFile, overwrite = true)

                    val walBackup = File(context.filesDir, "furniture_store_db_backup.db-wal")
                    val shmBackup = File(context.filesDir, "furniture_store_db_backup.db-shm")
                    val walDb = context.getDatabasePath("furniture_store_db-wal")
                    val shmDb = context.getDatabasePath("furniture_store_db-shm")

                    if (walBackup.exists()) walBackup.copyTo(walDb, overwrite = true) else walDb.delete()
                    if (shmBackup.exists()) shmBackup.copyTo(shmDb, overwrite = true) else shmDb.delete()

                    try {
                        db.invalidationTracker.refreshVersionsAsync()
                    } catch (e: Exception) {
                        android.util.Log.e("Restore", "Error invalidating tables", e)
                    }

                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    private data class ColumnMeta(
        val name: String,
        val type: String,
        val isNotNull: Boolean
    )

    private fun getTableColumnMeta(writableDb: SupportSQLiteDatabase, tableName: String): Map<String, ColumnMeta> {
        val map = mutableMapOf<String, ColumnMeta>()
        try {
            writableDb.query("PRAGMA table_info($tableName)").use { cursor ->
                val nameIdx = cursor.getColumnIndex("name")
                val typeIdx = cursor.getColumnIndex("type")
                val notNullIdx = cursor.getColumnIndex("notnull")
                while (cursor.moveToNext()) {
                    if (nameIdx >= 0 && typeIdx >= 0) {
                        val colName = cursor.getString(nameIdx)
                        val colType = cursor.getString(typeIdx).uppercase(Locale.ROOT)
                        val isNotNull = if (notNullIdx >= 0) cursor.getInt(notNullIdx) == 1 else false
                        map[colName.lowercase(Locale.ROOT).trim()] = ColumnMeta(colName, colType, isNotNull)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("Import", "Error reading table_info for $tableName", e)
        }
        return map
    }

    private fun safeCellText(cell: org.dhatim.fastexcel.reader.Cell?): String {
        if (cell == null) return ""
        return try {
            when (cell.type) {
                org.dhatim.fastexcel.reader.CellType.NUMBER -> {
                    val num = cell.asNumber()
                    if (num != null) {
                        val plain = num.toPlainString()
                        if (plain.endsWith(".0")) plain.substring(0, plain.length - 2) else plain
                    } else {
                        cell.text ?: cell.rawValue ?: ""
                    }
                }
                org.dhatim.fastexcel.reader.CellType.BOOLEAN -> cell.asBoolean()?.toString() ?: ""
                org.dhatim.fastexcel.reader.CellType.STRING -> cell.text ?: cell.rawValue ?: ""
                org.dhatim.fastexcel.reader.CellType.FORMULA -> cell.text ?: cell.rawValue ?: ""
                else -> cell.text ?: cell.rawValue ?: ""
            }
        } catch (e: Exception) {
            try {
                cell.text ?: cell.rawValue ?: ""
            } catch (e2: Exception) {
                ""
            }
        }
    }

    private fun processSheetRows(
        sheetName: String,
        rows: List<Map<String, String>>,
        onProgress: (Int, String) -> Unit,
        counts: IntArray
    ) {
        if (rows.isEmpty()) return
        val actualTableName = tablesToSync.find { it.equals(sheetName, ignoreCase = true) } ?: return

        val writableDb = db.openHelper.writableDatabase
        val colMetaMap = getTableColumnMeta(writableDb, actualTableName)

        val prefs = context.getSharedPreferences("store_settings", android.content.Context.MODE_PRIVATE)
        val syncMode = prefs.getString("sync_mode", "SMART_MERGE") ?: "SMART_MERGE"

        writableDb.beginTransaction()
        try {
            android.util.Log.d("Import", "Insert batch for $actualTableName: ${rows.size} rows (syncMode=$syncMode)")
            for (row in rows) {
                val rowId = row.entries.find { it.key.equals("id", ignoreCase = true) }?.value?.trim()
                if (rowId.isNullOrBlank()) continue

                val fileVersion = row.entries.find { it.key.equals("version", ignoreCase = true) }?.value?.toLongOrNull()?.toInt() ?: 1
                val fileUpdatedStr = row.entries.find { it.key.equals("updatedAt", ignoreCase = true) }?.value?.trim() ?: ""
                val fileUpdated = fileUpdatedStr.toLongOrNull()
                    ?: fileUpdatedStr.toDoubleOrNull()?.toLong()
                    ?: try {
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(fileUpdatedStr)?.time
                    } catch (e: Exception) { null }
                    ?: 0L

                val fileDeletedStr = row.entries.find { it.key.equals("isDeleted", ignoreCase = true) }?.value?.trim() ?: ""
                val fileDeleted = when (fileDeletedStr.lowercase(Locale.ROOT)) {
                    "true", "1" -> 1
                    else -> 0
                }

                var localVersion = -1
                var localUpdated = -1L

                writableDb.query("SELECT version, updatedAt FROM $actualTableName WHERE id = ?", arrayOf(rowId)).use { cursor ->
                    if (cursor.moveToFirst()) {
                        localVersion = cursor.getInt(0)
                        localUpdated = cursor.getLong(1)
                    }
                }

                val isInsert = localVersion == -1
                val isUpdate = when (syncMode) {
                    "SKIP_EXISTING" -> false
                    "FORCE_OVERWRITE" -> !isInsert
                    else -> !isInsert && (fileVersion >= localVersion || fileUpdated >= localUpdated || fileUpdated == 0L)
                }

                if (isInsert || isUpdate) {
                    try {
                        val cv = ContentValues()
                        for ((col, value) in row) {
                            val trimmedCol = col.trim().lowercase(Locale.ROOT)
                            val meta = colMetaMap[trimmedCol] ?: continue
                            val colType = meta.type
                            val isNotNull = meta.isNotNull
                            val cleanVal = value.trim()

                            if (cleanVal.isEmpty()) {
                                if (meta.name.equals("id", ignoreCase = true)) {
                                    cv.put("id", rowId)
                                    continue
                                }
                                if (isNotNull) {
                                    if (colType.contains("INT")) {
                                        cv.put(meta.name, 0L)
                                    } else if (colType.contains("REAL") || colType.contains("FLOAT") || colType.contains("DOUBLE") || colType.contains("NUMERIC")) {
                                        cv.put(meta.name, 0.0)
                                    } else {
                                        cv.put(meta.name, "")
                                    }
                                } else {
                                    cv.putNull(meta.name)
                                }
                            } else {
                                if (colType.contains("INT")) {
                                    val longVal = cleanVal.toLongOrNull() ?: cleanVal.toDoubleOrNull()?.toLong() ?: 0L
                                    cv.put(meta.name, longVal)
                                } else if (colType.contains("REAL") || colType.contains("FLOAT") || colType.contains("DOUBLE") || colType.contains("NUMERIC")) {
                                    val doubleVal = cleanVal.toDoubleOrNull() ?: 0.0
                                    cv.put(meta.name, doubleVal)
                                } else {
                                    cv.put(meta.name, cleanVal)
                                }
                            }
                        }

                        // Ensure default metadata columns are appropriately populated
                        val syncStatusMeta = colMetaMap["syncstatus"]
                        if (syncStatusMeta != null) {
                            cv.put(syncStatusMeta.name, 1)
                        }

                        val isDeletedMeta = colMetaMap["isdeleted"]
                        if (isDeletedMeta != null && !cv.containsKey(isDeletedMeta.name)) {
                            cv.put(isDeletedMeta.name, fileDeleted)
                        }

                        val updatedAtMeta = colMetaMap["updatedat"]
                        if (updatedAtMeta != null && !cv.containsKey(updatedAtMeta.name)) {
                            cv.put(updatedAtMeta.name, if (fileUpdated > 0L) fileUpdated else System.currentTimeMillis())
                        }

                        val resInsert = writableDb.insert(actualTableName, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, cv)
                        if (resInsert != -1L) {
                            if (isInsert) {
                                if (fileDeleted == 1) counts[3]++ else counts[0]++
                            } else {
                                if (fileDeleted == 1) counts[3]++ else counts[1]++
                            }
                        } else {
                            counts[2]++
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("Import", "Error inserting row $rowId in $actualTableName", e)
                        counts[2]++
                    }
                } else {
                    counts[2]++
                }
            }
            writableDb.setTransactionSuccessful()
        } catch (e: Exception) {
            android.util.Log.e("Import", "Transaction error in $actualTableName", e)
        } finally {
            writableDb.endTransaction()
        }
    }

    suspend fun importData(
        uri: Uri,
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): SyncResult {
        return withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences("store_settings", android.content.Context.MODE_PRIVATE)
            val autoBackup = prefs.getBoolean("auto_backup", true)

            if (autoBackup) {
                onProgress(0, "إنشاء نسخة احتياطية...")
                val backupFile = File(context.filesDir, "pre_import_${System.currentTimeMillis()}.db")
                try {
                    val dbFile = context.getDatabasePath("furniture_store_db")
                    if (dbFile.exists()) {
                        db.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")
                        dbFile.copyTo(backupFile, overwrite = true)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            android.util.Log.d("Import", "Starting import from URI: $uri")
            var sourceDevice = "UNKNOWN"
            val counts = IntArray(4) // added, updated, skipped, deleted

            val tempFile = File(context.cacheDir, "import_temp_${System.currentTimeMillis()}.xlsx")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                if (!tempFile.exists() || tempFile.length() == 0L) {
                    return@withContext SyncResult(0, 0, 0, 0)
                }

                ReadableWorkbook(tempFile).use { wb ->
                    val sheetsList = wb.sheets.collect(Collectors.toList())

                    // Read META
                    val metaSheet = sheetsList.find { it.name.equals("__META", ignoreCase = true) }
                    if (metaSheet != null) {
                        try {
                            metaSheet.openStream().use { rowStream ->
                                rowStream.forEach { row ->
                                    val cell0 = row.getCell(0)
                                    val cell1 = row.getCell(1)
                                    val key = safeCellText(cell0).trim().lowercase(Locale.ROOT)
                                    val value = safeCellText(cell1).trim()
                                    if (key == "source_device" || key == "sourcedevice" || key.contains("source_device")) {
                                        sourceDevice = value
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("Import", "Error reading __META", e)
                        }
                    }

                    // Process each table in topological order
                    for (tableName in tablesToSync) {
                        val sheet = sheetsList.find { it.name.equals(tableName, ignoreCase = true) }
                        if (sheet == null) continue

                        android.util.Log.d("Import", "Processing sheet: ${sheet.name}")
                        onProgress(0, "قراءة: ${sheet.name}")
                        val rowsBuffer = mutableListOf<Map<String, String>>()
                        var headers: List<String>? = null
                        var processedCount = 0

                        try {
                            sheet.openStream().use { rowStream ->
                                rowStream.forEach { row ->
                                    if (headers == null) {
                                        headers = (0 until row.cellCount).map { safeCellText(row.getCell(it)).trim() }
                                    } else {
                                        val rowData = mutableMapOf<String, String>()
                                        for (index in 0 until row.cellCount) {
                                            val cell = row.getCell(index)
                                            val header = headers?.getOrNull(index) ?: continue
                                            if (header.isNotBlank()) {
                                                rowData[header] = safeCellText(cell)
                                            }
                                        }
                                        if (rowData.values.any { it.isNotBlank() }) {
                                            rowsBuffer.add(rowData)
                                        }

                                        if (rowsBuffer.size >= 200) {
                                            processSheetRows(sheet.name, rowsBuffer, onProgress, counts)
                                            processedCount += rowsBuffer.size
                                            onProgress(0, "جاري استيراد ${sheet.name}: $processedCount سجل...")
                                            rowsBuffer.clear()
                                        }
                                    }
                                }
                            }

                            if (rowsBuffer.isNotEmpty()) {
                                processSheetRows(sheet.name, rowsBuffer, onProgress, counts)
                                rowsBuffer.clear()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("Import", "Error reading sheet ${sheet.name}", e)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("Import", "Failed to import file", e)
            } finally {
                tempFile.delete()
            }

            db.syncDao().insertSyncLog(
                SyncLog(
                    syncType = "IMPORT",
                    sourceDevice = sourceDevice,
                    recordsAdded = counts[0],
                    recordsUpdated = counts[1],
                    recordsSkipped = counts[2],
                    recordsDeleted = counts[3],
                    isSuccess = true
                )
            )

            try {
                db.invalidationTracker.refreshVersionsAsync()
            } catch (e: Exception) {
                android.util.Log.e("Import", "Error refreshing invalidation tracker", e)
            }

            try {
                val backups = context.filesDir.listFiles { _, name -> name.startsWith("pre_import_") }
                backups?.sortedByDescending { it.lastModified() }?.drop(5)?.forEach { it.delete() }
            } catch (e: Exception) { }

            android.util.Log.i("Import", "Import complete: added=${counts[0]} updated=${counts[1]} skipped=${counts[2]}")
            SyncResult(counts[0], counts[1], counts[2], counts[3])
        }
    }
}
