import re

with open('app/src/main/java/com/example/data/sync/SyncEngine.kt', 'r') as f:
    content = f.read()

content = content.replace("return@forEach", "continue")
content = content.replace("return@forEachIndexed", "continue")

with open('app/src/main/java/com/example/data/sync/SyncEngine.kt', 'w') as f:
    f.write(content)
