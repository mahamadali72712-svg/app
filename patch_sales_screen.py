import re

with open('app/src/main/java/com/example/ui/screens/SalesScreen.kt', 'r') as f:
    content = f.read()

# Make InvoiceItemUiModel mutable so we can increment quantity
content = content.replace(
    'data class InvoiceItemUiModel(\n    val product: Product,\n    val quantity: Double,\n    val unitPrice: Double,\n    val unitCost: Double,\n    val lineTotal: Double,\n    val lineCost: Double,\n    val lineProfit: Double\n)',
    'data class InvoiceItemUiModel(\n    val product: Product,\n    var quantity: Double,\n    var unitPrice: Double,\n    val unitCost: Double,\n    var lineTotal: Double,\n    var lineCost: Double,\n    var lineProfit: Double\n)'
)

# Update UI of items to allow editing quantity
item_ui_old = '''                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("الكمية: ${item.quantity}")
                                Text("السعر: ${item.unitPrice.formatCurrency()}")
                            }'''
item_ui_new = '''                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { 
                                        item.quantity += 1.0
                                        item.lineTotal = item.unitPrice * item.quantity
                                        item.lineCost = item.unitCost * item.quantity
                                        item.lineProfit = item.lineTotal - item.lineCost
                                        // force recomposition by replacing item
                                        val idx = invoiceItems.indexOf(item)
                                        if (idx != -1) invoiceItems[idx] = item.copy()
                                    }, modifier = Modifier.size(24.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))) {
                                        Text("+", fontWeight = FontWeight.Bold)
                                    }
                                    Text(" ${item.quantity} ", modifier = Modifier.padding(horizontal = 8.dp))
                                    IconButton(onClick = { 
                                        if (item.quantity > 1.0) {
                                            item.quantity -= 1.0
                                            item.lineTotal = item.unitPrice * item.quantity
                                            item.lineCost = item.unitCost * item.quantity
                                            item.lineProfit = item.lineTotal - item.lineCost
                                            val idx = invoiceItems.indexOf(item)
                                            if (idx != -1) invoiceItems[idx] = item.copy()
                                        }
                                    }, modifier = Modifier.size(24.dp).background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(4.dp))) {
                                        Text("-", fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text("السعر: ${item.unitPrice.formatCurrency()}")
                            }'''
content = content.replace(item_ui_old, item_ui_new)

# Replace the AddProductSheet content
sheet_old_start = content.find('if (showAddProductSheet) {')
sheet_old_end = content.find('    }\n}', sheet_old_start) + 5

sheet_new = '''    if (showAddProductSheet) {
        var searchQuery by remember { mutableStateOf("") }
        val filteredProducts = products.filter { it.name.contains(searchQuery, ignoreCase = true) || (it.code != null && it.code.contains(searchQuery, ignoreCase = true)) }

        ModalBottomSheet(
            onDismissRequest = { showAddProductSheet = false },
            modifier = Modifier.fillMaxHeight(0.9f)
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("إضافة منتجات للفاتورة", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Button(onClick = { showAddProductSheet = false }) {
                        Text("تم")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("بحث عن منتج...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredProducts) { product ->
                        val existingItem = invoiceItems.find { it.product.id == product.id }
                        val currentQty = existingItem?.quantity ?: 0.0
                        
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (currentQty > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                                               else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(product.name, fontWeight = FontWeight.Bold)
                                    Text("السعر: ${product.suggestedPrice.formatCurrency()} | المخزون: ${product.stockQuantity}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                
                                if (currentQty == 0.0) {
                                    Button(
                                        onClick = {
                                            if (product.stockQuantity >= 1.0) {
                                                invoiceItems.add(
                                                    InvoiceItemUiModel(
                                                        product = product,
                                                        quantity = 1.0,
                                                        unitPrice = product.suggestedPrice,
                                                        unitCost = product.costPrice,
                                                        lineTotal = product.suggestedPrice,
                                                        lineCost = product.costPrice,
                                                        lineProfit = product.suggestedPrice - product.costPrice
                                                    )
                                                )
                                            } else {
                                                Toast.makeText(context, "المخزون لا يكفي", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Text("إضافة")
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = {
                                            if (existingItem != null) {
                                                if (existingItem.quantity + 1.0 > product.stockQuantity) {
                                                    Toast.makeText(context, "المخزون لا يكفي", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    existingItem.quantity += 1.0
                                                    existingItem.lineTotal = existingItem.unitPrice * existingItem.quantity
                                                    existingItem.lineCost = existingItem.unitCost * existingItem.quantity
                                                    existingItem.lineProfit = existingItem.lineTotal - existingItem.lineCost
                                                    val idx = invoiceItems.indexOf(existingItem)
                                                    if (idx != -1) invoiceItems[idx] = existingItem.copy()
                                                }
                                            }
                                        }, modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))) {
                                            Text("+", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                                        }
                                        
                                        Text(" ${currentQty.toInt()} ", modifier = Modifier.padding(horizontal = 12.dp), fontWeight = FontWeight.Bold)
                                        
                                        IconButton(onClick = {
                                            if (existingItem != null) {
                                                if (existingItem.quantity <= 1.0) {
                                                    invoiceItems.remove(existingItem)
                                                } else {
                                                    existingItem.quantity -= 1.0
                                                    existingItem.lineTotal = existingItem.unitPrice * existingItem.quantity
                                                    existingItem.lineCost = existingItem.unitCost * existingItem.quantity
                                                    existingItem.lineProfit = existingItem.lineTotal - existingItem.lineCost
                                                    val idx = invoiceItems.indexOf(existingItem)
                                                    if (idx != -1) invoiceItems[idx] = existingItem.copy()
                                                }
                                            }
                                        }, modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.error, RoundedCornerShape(4.dp))) {
                                            Text("-", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }'''

content = content[:sheet_old_start] + sheet_new + '\n' + content[sheet_old_end:]

with open('app/src/main/java/com/example/ui/screens/SalesScreen.kt', 'w') as f:
    f.write(content)
