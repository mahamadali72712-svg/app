import re

with open('app/src/main/java/com/example/data/sync/SyncEngine.kt', 'r') as f:
    content = f.read()

content = content.replace('var sourceDevice = "UNKNOWN"', 'android.util.Log.d("Import", "Starting import from URI: $uri")\n            var sourceDevice = "UNKNOWN"')
content = content.replace('onProgress(0, "قراءة: ${sheet.name}")', 'android.util.Log.d("Import", "Sheet found: ${sheet.name}")\n                        onProgress(0, "قراءة: ${sheet.name}")')
content = content.replace('processSheetRows(sheet.name, rowsBuffer, onProgress, counts)', 'android.util.Log.d("Import", "Rows in sheet ${sheet.name}: ${rowsBuffer.size}")\n                        processSheetRows(sheet.name, rowsBuffer, onProgress, counts)')
content = content.replace('rows.chunked(200).forEachIndexed { index, chunk ->', 'rows.chunked(200).forEachIndexed { index, chunk ->\n                android.util.Log.d("Import", "Insert batch: ${chunk.size} rows")')
content = content.replace('SyncResult(counts[0], counts[1], counts[2], counts[3])', 'android.util.Log.i("Import", "Import complete: added=${counts[0]} updated=${counts[1]} skipped=${counts[2]}")\n            SyncResult(counts[0], counts[1], counts[2], counts[3])')

with open('app/src/main/java/com/example/data/sync/SyncEngine.kt', 'w') as f:
    f.write(content)
