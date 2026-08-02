import re

with open('app/src/main/java/com/example/ui/screens/ProductsScreen.kt', 'r') as f:
    content = f.read()

# Fix duplicates in dropdown
dropdown_old = '''                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.name) },
                                        onClick = { categoryId = cat.id; expanded = false }
                                    )
                                }'''
dropdown_new = '''                                categories.distinctBy { it.name }.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.name) },
                                        onClick = { categoryId = cat.id; expanded = false }
                                    )
                                }'''
content = content.replace(dropdown_old, dropdown_new)

# Fix duplicates in filter chips
chips_old = '''                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategoryId == category.id,
                            onClick = { selectedCategoryId = category.id },
                            label = { Text(category.name) }
                        )
                    }'''
chips_new = '''                    items(categories.distinctBy { it.name }) { category ->
                        FilterChip(
                            selected = selectedCategoryId == category.id,
                            onClick = { selectedCategoryId = category.id },
                            label = { Text(category.name) }
                        )
                    }'''
content = content.replace(chips_old, chips_new)

with open('app/src/main/java/com/example/ui/screens/ProductsScreen.kt', 'w') as f:
    f.write(content)
