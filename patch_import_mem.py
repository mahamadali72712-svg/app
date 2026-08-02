import re

with open('app/src/main/java/com/example/data/sync/SyncEngine.kt', 'r') as f:
    content = f.read()

import_logic = '''
                    for (sheet in wb.sheets.iterator()) {
                        if (sheet.name == "__META" || sheet.name == "__CHANGE_LOG") continue
                        
                        android.util.Log.d("Import", "Sheet found: ${sheet.name}")
                        onProgress(0, "قراءة: ${sheet.name}")
                        val rowsBuffer = mutableListOf<Map<String, String>>()
                        var headers: List<String>? = null
                        var processedCount = 0

                        sheet.openStream().use { rowStream ->
                            rowStream.forEach { row ->
                                if (headers == null) {
                                    headers = (0 until row.cellCount).map { safeCellText(row.getCell(it)) }
                                } else {
                                    val rowData = mutableMapOf<String, String>()
                                    for (index in 0 until row.cellCount) {
                                        val cell = row.getCell(index)
                                        val header = headers?.getOrNull(index) ?: continue
                                        if (header.isNotBlank()) {
                                            rowData[header] = safeCellText(cell)
                                        }
                                    }
                                    rowsBuffer.add(rowData)
                                    
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
                    }
'''

# The previous logic was:
old_logic = '''
                    for (sheet in wb.sheets.iterator()) {
                        if (sheet.name == "__META" || sheet.name == "__CHANGE_LOG") continue
                        
                        android.util.Log.d("Import", "Sheet found: ${sheet.name}")
                        onProgress(0, "قراءة: ${sheet.name}")
                        val rowsBuffer = mutableListOf<Map<String, String>>()
                        var headers: List<String>? = null

                        sheet.openStream().use { rowStream ->
                            rowStream.forEach { row ->
                                if (headers == null) {
                                    headers = (0 until row.cellCount).map { safeCellText(row.getCell(it)) }
                                } else {
                                    val rowData = mutableMapOf<String, String>()
                                    for (index in 0 until row.cellCount) {
                                        val cell = row.getCell(index)
                                        val header = headers?.getOrNull(index) ?: continue
                                        if (header.isNotBlank()) {
                                            rowData[header] = safeCellText(cell)
                                        }
                                    }
                                    rowsBuffer.add(rowData)
                                }
                            }
                        }
                        
                        android.util.Log.d("Import", "Rows in sheet ${sheet.name}: ${rowsBuffer.size}")
                        processSheetRows(sheet.name, rowsBuffer, onProgress, counts)
                        rowsBuffer.clear()
                    }'''

if 'processedCount += rowsBuffer.size' not in content:
    content = content.replace(old_logic, import_logic)
    with open('app/src/main/java/com/example/data/sync/SyncEngine.kt', 'w') as f:
        f.write(content)
