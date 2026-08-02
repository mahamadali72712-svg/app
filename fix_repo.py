import re

with open('app/src/main/java/com/example/data/repository/StoreRepository.kt', 'r') as f:
    content = f.read()

content = content.replace('    } Process\n    suspend fun processSale(', '    }\n\n    suspend fun processSale(')

with open('app/src/main/java/com/example/data/repository/StoreRepository.kt', 'w') as f:
    f.write(content)
