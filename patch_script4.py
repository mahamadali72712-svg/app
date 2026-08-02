import re

with open('app/src/main/java/com/example/data/sync/SyncEngine.kt', 'r') as f:
    content = f.read()

content = content.replace("chunk.forEach { row ->", "for (row in chunk) {")
content = content.replace("continueIndexed", "continue")

with open('app/src/main/java/com/example/data/sync/SyncEngine.kt', 'w') as f:
    f.write(content)
