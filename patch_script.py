import re

with open('app/src/main/java/com/example/data/sync/SyncEngine.kt', 'r') as f:
    content = f.read()

# We need to insert ImportState at the top level
if 'sealed class ImportState' not in content:
    content = content.replace('class SyncEngine', '''
sealed class ImportState {
    object Idle : ImportState()
    data class Loading(val percent: Int, val message: String) : ImportState()
    data class Success(val result: SyncResult) : ImportState()
    data class Error(val message: String) : ImportState()
}

class SyncEngine''')

# We want to replace importData function with the new one
import_data_start = content.find('    suspend fun importData(uri: Uri): SyncResult {')
if import_data_start != -1:
    import_data_end = content.find('    }\n}', import_data_start)
    
    new_import_data = '''    private fun safeCellText(cell: org.dhatim.fastexcel.reader.Cell?): String {
        if (cell == null) return ""
        return try {
            cell.text ?: ""
        } catch (e: Exception) {
            try {
                cell.rawValue ?: ""
            } catch (e2: Exception) {
                ""
            }
        }
    }

    private fun safeCellDouble(cell: org.dhatim.fastexcel.reader.Cell?): Double {
        if (cell == null) return 0.0
        return try {
            cell.asNumber?.toDouble() ?: cell.text?.toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            cell.text?.replace(",", "")?.toDoubleOrNull() ?: 0.0
        }
    }

    private fun safeCellLong(cell: org.dhatim.fastexcel.reader.Cell?): Long {
        if (cell == null) return 0L
        return try {
            cell.asNumber?.toLong() ?: cell.text?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private suspend fun processSheetRows(
        sheetName: String,
        rows: List<Map<String, String>>,
        onProgress: (Int, String) -> Unit,
        counts: IntArray
    ) {
        if (rows.isEmpty()) return
        val actualTableName = tablesToSync.find { it.equals(sheetName, ignoreCase = true) } ?: return

        val writableDb = db.openHelper.writableDatabase
        writableDb.beginTransaction()
        try {
            val idIdx = "id"
            val versionIdx = "version"
            val updatedIdx = "updatedAt"
            val deletedIdx = "isDeleted"

            rows.chunked(200).forEachIndexed { index, chunk ->
                chunk.forEach { row ->
                    val rowId = row[idIdx]
                    if (rowId.isNullOrBlank()) return@forEach
                    
                    val fileVersion = row[versionIdx]?.toDoubleOrNull()?.toInt() ?: 1
                    val fileUpdated = row[updatedIdx]?.toDoubleOrNull()?.toLong() ?: 0L
                    val fileDeleted = row[deletedIdx]?.toDoubleOrNull()?.toInt() ?: 0

                    var localVersion = -1
                    var localUpdated = -1L

                    writableDb.query("SELECT version, updatedAt FROM $actualTableName WHERE id = ?", arrayOf(rowId)).use { cursor ->
                        if (cursor.moveToFirst()) {
                            localVersion = cursor.getInt(0)
                            localUpdated = cursor.getLong(1)
                        }
                    }

                    val isInsert = localVersion == -1
                    val isUpdate = !isInsert && (fileVersion > localVersion || (fileVersion == localVersion && fileUpdated > localUpdated))

                    if (isInsert || isUpdate) {
                        val cv = android.content.ContentValues()
                        for ((col, value) in row) {
                            cv.put(col, value)
                        }
                        if (isInsert) {
                            writableDb.insert(actualTableName, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, cv)
                            if (fileDeleted == 1) counts[3]++ else counts[0]++
                        } else {
                            writableDb.update(actualTableName, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, cv, "id = ?", arrayOf(rowId))
                            if (fileDeleted == 1) counts[3]++ else counts[1]++
                        }
                    } else {
                        counts[2]++
                    }
                }
                val percent = ((index + 1) * 100) / (rows.size / 200 + 1)
                onProgress(percent, "استيراد $actualTableName: ${(index + 1) * 200}/${rows.size}")
            }
            writableDb.setTransactionSuccessful()
        } finally {
            writableDb.endTransaction()
        }
    }

    suspend fun importData(uri: android.net.Uri, onProgress: (Int, String) -> Unit = { _, _ -> }): SyncResult {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            onProgress(0, "إنشاء نسخة احتياطية...")
            val backupFile = java.io.File(context.filesDir, "pre_import_${System.currentTimeMillis()}.db")
            try {
                val dbFile = context.getDatabasePath("furniture_store_db")
                if (dbFile.exists()) {
                    db.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")
                    dbFile.copyTo(backupFile, overwrite = true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            var sourceDevice = "UNKNOWN"
            val counts = IntArray(4) // added, updated, skipped, deleted

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                org.dhatim.fastexcel.reader.ReadableWorkbook(inputStream).use { wb ->
                    val sheetNames = wb.sheets.map { it.name }.toList()
                    
                    // First read META
                    wb.findSheet("__META").ifPresent { sheet ->
                        sheet.openStream().use { rowStream ->
                            rowStream.forEach { row ->
                                if (row.cellCount >= 2 && safeCellText(row.getCell(0)) == "source_device") {
                                    sourceDevice = safeCellText(row.getCell(1))
                                }
                            }
                        }
                    }

                    wb.sheets.forEach { sheet ->
                        if (sheet.name == "__META" || sheet.name == "__CHANGE_LOG") return@forEach
                        
                        onProgress(0, "قراءة: ${sheet.name}")
                        val rowsBuffer = mutableListOf<Map<String, String>>()
                        var headers: List<String>? = null

                        sheet.openStream().use { rowStream ->
                            rowStream.forEach { row ->
                                if (headers == null) {
                                    headers = row.cells.map { safeCellText(it) }
                                } else {
                                    val rowData = mutableMapOf<String, String>()
                                    row.cells.forEachIndexed { index, cell ->
                                        val header = headers?.getOrNull(index) ?: return@forEachIndexed
                                        if (header.isNotBlank()) {
                                            rowData[header] = safeCellText(cell)
                                        }
                                    }
                                    rowsBuffer.add(rowData)
                                }
                            }
                        }
                        
                        processSheetRows(sheet.name, rowsBuffer, onProgress, counts)
                        rowsBuffer.clear()
                    }
                }
            }

            db.syncDao().insertSyncLog(SyncLog(
                syncType = "IMPORT",
                sourceDevice = sourceDevice,
                recordsAdded = counts[0],
                recordsUpdated = counts[1],
                recordsSkipped = counts[2],
                recordsDeleted = counts[3],
                isSuccess = true
            ))
            
            // cleanup backups
            try {
                val backups = context.filesDir.listFiles { _, name -> name.startsWith("pre_import_") }
                backups?.sortedByDescending { it.lastModified() }?.drop(5)?.forEach { it.delete() }
            } catch(e:Exception){}

            SyncResult(counts[0], counts[1], counts[2], counts[3])
        }
'''
    content = content[:import_data_start] + new_import_data + content[import_data_end:]

with open('app/src/main/java/com/example/data/sync/SyncEngine.kt', 'w') as f:
    f.write(content)
