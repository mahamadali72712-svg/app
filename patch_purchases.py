import re

with open('app/src/main/java/com/example/ui/screens/PurchasesScreen.kt', 'r') as f:
    content = f.read()

new_model = '''data class PurchaseItemUiModel(
    val product: com.example.data.local.Product,
    var quantity: Double,
    var unitCost: Double,
    var lineTotal: Double
)
'''

content = content.replace('import androidx.compose.ui.platform.LocalContext\n', 'import androidx.compose.ui.platform.LocalContext\nimport androidx.compose.foundation.lazy.LazyColumn\nimport androidx.compose.foundation.lazy.items\nimport androidx.compose.foundation.background\nimport androidx.compose.foundation.shape.RoundedCornerShape\nimport androidx.compose.foundation.text.KeyboardOptions\nimport androidx.compose.ui.text.input.KeyboardType\nimport androidx.compose.material.icons.filled.Delete\n' + new_model)

content = re.sub(r'var supplierId by remember \{ mutableStateOf\(""\) \}.*', '''    var supplierId by remember { mutableStateOf("") }
    var newSupplierName by remember { mutableStateOf("") }
    
    val invoiceItems = remember { mutableStateListOf<PurchaseItemUiModel>() }
    var showAddProductSheet by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    
    val totalCost = invoiceItems.sumOf { it.lineTotal }
    
    var paymentType by remember { mutableStateOf("CASH") }
    var partialAmountText by remember { mutableStateOf("") }
    
    val pAmt = partialAmountText.toDoubleOrNull() ?: 0.0
    val finalPaidAmount = when (paymentType) {
        "CASH" -> totalCost
        "CREDIT" -> 0.0
        else -> pAmt
    }
    val remainingAmount = totalCost - finalPaidAmount

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مشتريات") },
                actions = {
                    IconButton(onClick = { navController.navigate("purchases_history") }) {
                        Icon(Icons.Filled.List, contentDescription = "سجل المشتريات")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = {
                            if (invoiceItems.isEmpty()) {
                                Toast.makeText(context, "الرجاء إضافة منتجات", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (supplierId.isEmpty()) {
                                Toast.makeText(context, "الرجاء اختيار مورد", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isProcessing = true
                            val items = invoiceItems.map {
                                PurchaseInvoiceItem(
                                    invoiceId = "",
                                    productId = it.product.id,
                                    quantity = it.quantity,
                                    unitCost = it.unitCost,
                                    lineTotal = it.lineTotal
                                )
                            }
                            
                            viewModel.processPurchase(
                                supplierId = supplierId,
                                items = items,
                                discount = 0.0,
                                paidAmount = finalPaidAmount,
                                onSuccess = {
                                    isProcessing = false
                                    Toast.makeText(context, "تم الحفظ بنجاح", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = !isProcessing
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("حفظ الفاتورة (${totalCost.formatCurrency()})", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(horizontal = 16.dp).fillMaxSize()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                // Supplier selection / creation
                var supplierExpanded by remember { mutableStateOf(false) }
                val selectedSupplier = suppliers.find { it.id == supplierId }
                
                ExposedDropdownMenuBox(
                    expanded = supplierExpanded,
                    onExpandedChange = { supplierExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedSupplier?.name ?: "اختر المورد",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = supplierExpanded, onDismissRequest = { supplierExpanded = false }) {
                        suppliers.forEach { sup ->
                            DropdownMenuItem(
                                text = { Text(sup.name) },
                                onClick = {
                                    supplierId = sup.id
                                    supplierExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                if (supplierId.isEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newSupplierName,
                            onValueChange = { newSupplierName = it },
                            label = { Text("أو أضف مورد جديد") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { 
                            if (newSupplierName.isNotBlank()) {
                                viewModel.addSupplier(newSupplierName, "")
                                newSupplierName = ""
                            }
                        }) {
                            Text("إضافة")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("المنتجات", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Button(onClick = { showAddProductSheet = true }) {
                        Text("+ إضافة منتج")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            if (invoiceItems.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("لم يتم إضافة منتجات للفاتورة", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(invoiceItems) { item ->
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(item.product.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                IconButton(
                                    onClick = { invoiceItems.remove(item) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "إزالة", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { 
                                        item.quantity += 1.0
                                        item.lineTotal = item.unitCost * item.quantity
                                        val idx = invoiceItems.indexOf(item)
                                        if (idx != -1) invoiceItems[idx] = item.copy()
                                    }, modifier = Modifier.size(24.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))) {
                                        Text("+", fontWeight = FontWeight.Bold)
                                    }
                                    Text(" ${item.quantity} ", modifier = Modifier.padding(horizontal = 8.dp))
                                    IconButton(onClick = { 
                                        if (item.quantity > 1.0) {
                                            item.quantity -= 1.0
                                            item.lineTotal = item.unitCost * item.quantity
                                            val idx = invoiceItems.indexOf(item)
                                            if (idx != -1) invoiceItems[idx] = item.copy()
                                        }
                                    }, modifier = Modifier.size(24.dp).background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(4.dp))) {
                                        Text("-", fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text("السعر: ${item.unitCost.formatCurrency()}")
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("المجموع: ${item.lineTotal.formatCurrency()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                
                // Payment Method
                Text("طريقة الدفع", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = paymentType == "CASH", onClick = { paymentType = "CASH" })
                    Text("نقدي", modifier = Modifier.padding(end = 8.dp))
                    
                    RadioButton(selected = paymentType == "CREDIT", onClick = { paymentType = "CREDIT" })
                    Text("آجل", modifier = Modifier.padding(end = 8.dp))
                    
                    RadioButton(selected = paymentType == "PARTIAL", onClick = { paymentType = "PARTIAL" })
                    Text("جزئي")
                }
                
                if (paymentType == "PARTIAL") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = partialAmountText,
                        onValueChange = { partialAmountText = it },
                        label = { Text("المبلغ المدفوع") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                if (paymentType != "CASH") {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("المدفوع:", fontSize = 16.sp)
                        Text(finalPaidAmount.formatCurrency(), fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("المتبقي:", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(remainingAmount.formatCurrency(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                }
                
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    if (showAddProductSheet) {
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
                    Text("إضافة منتجات للمشتريات", fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
                                    Text("التكلفة: ${product.costPrice.formatCurrency()} | المخزون: ${product.stockQuantity}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                
                                if (currentQty == 0.0) {
                                    Button(
                                        onClick = {
                                            invoiceItems.add(
                                                PurchaseItemUiModel(
                                                    product = product,
                                                    quantity = 1.0,
                                                    unitCost = product.costPrice,
                                                    lineTotal = product.costPrice
                                                )
                                            )
                                        }
                                    ) {
                                        Text("إضافة")
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = {
                                            if (existingItem != null) {
                                                existingItem.quantity += 1.0
                                                existingItem.lineTotal = existingItem.unitCost * existingItem.quantity
                                                val idx = invoiceItems.indexOf(existingItem)
                                                if (idx != -1) invoiceItems[idx] = existingItem.copy()
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
                                                    existingItem.lineTotal = existingItem.unitCost * existingItem.quantity
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
    }
}
''', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screens/PurchasesScreen.kt', 'w') as f:
    f.write(content)
