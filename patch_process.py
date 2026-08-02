import re

with open('app/src/main/java/com/example/data/sync/SyncEngine.kt', 'r') as f:
    content = f.read()

# Let's remove the chunked from processSheetRows since we chunk outside
old_process = '''
            rows.chunked(200).forEachIndexed { index, chunk ->
                android.util.Log.d("Import", "Insert batch: ${chunk.size} rows")
                for (row in chunk) {
'''

new_process = '''
            android.util.Log.d("Import", "Insert batch: ${rows.size} rows")
            for (row in rows) {
'''

content = content.replace(old_process, new_process)

# fix the closing brace
old_end = '''                }
                val percent = ((index + 1) * 100) / (rows.size / 200 + 1)
                onProgress(percent, "استيراد $actualTableName: ${(index + 1) * 200}/${rows.size}")
            }
            writableDb.setTransactionSuccessful()'''

new_end = '''                }
            writableDb.setTransactionSuccessful()'''

content = content.replace(old_end, new_end)

with open('app/src/main/java/com/example/data/sync/SyncEngine.kt', 'w') as f:
    f.write(content)

