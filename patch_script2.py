import re

with open('app/src/main/java/com/example/data/sync/SyncEngine.kt', 'r') as f:
    content = f.read()

content = content.replace("cell.asNumber?.toDouble()", "cell.asNumber().toDouble()")
content = content.replace("cell.asNumber?.toLong()", "cell.asNumber().toLong()")

content = content.replace("row.cells.map { safeCellText(it) }", "(0 until row.cellCount).map { safeCellText(row.getCell(it)) }")
content = content.replace("row.cells.forEachIndexed { index, cell ->", "for (index in 0 until row.cellCount) {\n                                        val cell = row.getCell(index)")

content = content.replace("wb.sheets.forEach { sheet ->", "for (sheet in wb.sheets.iterator()) {")

with open('app/src/main/java/com/example/data/sync/SyncEngine.kt', 'w') as f:
    f.write(content)
