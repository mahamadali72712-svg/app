import re

with open('app/src/main/java/com/example/ui/viewmodels/StoreViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace('import kotlinx.coroutines.flow.map', 'import kotlinx.coroutines.flow.map\nimport kotlinx.coroutines.flow.first')

new_seed = '''    fun seedCategories() {
        viewModelScope.launch {
            val current = repository.allCategories.first()
            if (current.isEmpty()) {
                val defaults = listOf("غرف نوم", "كنب", "مراتب", "طاولات", "ستائر", "سجاد", "أخرى")
                defaults.forEachIndexed { index, name ->
                    repository.addCategory(com.example.data.local.ProductCategory(name = name, sortOrder = index))
                }
            }
        }
    }'''

content = re.sub(r'    fun seedCategories\(\) \{[\s\S]*?    \}', new_seed, content)

with open('app/src/main/java/com/example/ui/viewmodels/StoreViewModel.kt', 'w') as f:
    f.write(content)
