import re

with open('app/src/main/java/com/example/ui/screens/PurchasesScreen.kt', 'r') as f:
    content = f.read()

# Make sure imports formatCurrency and formatQty are there
if 'import com.example.utils.formatQty' not in content:
    content = content.replace('import com.example.utils.formatCurrency', 'import com.example.utils.formatCurrency\nimport com.example.utils.formatQty')

state_declarations = '''    var showAddProductSheet by remember { mutableStateOf(false) }
    var selectedProductForConfig by remember { mutableStateOf<Product?>(null) }'''

content = content.replace('    var showAddProductSheet by remember { mutableStateOf(false) }', state_declarations)

config_dialog_code = '''
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseProductConfigDialog(
    product: Product,
    onDismiss: () -> Unit,
    onSave: (quantity: Double, unitCost: Double) -> Unit
) {
    var isDimensional by remember { mutableStateOf(false) }
    var length by remember { mutableStateOf("") }
    var width by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var unitCost by remember { mutableStateOf(product.costPrice.toString()) }

    val calculatedQuantity = remember(isDimensional, length, width, quantity) {
        if (isDimensional) {
            val l = length.toDoubleOrNull() ?: 0.0
            val w = width.toDoubleOrNull() ?: 0.0
            if (l > 0 && w > 0) (l * w) / 4.0 else 0.0
        } else {
            quantity.toDoubleOrNull() ?: 0.0
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(product.name, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isDimensional, onCheckedChange = { isDimensional = it })
                    Text("شراء بالمقاس (طول × عرض)")
                }

                if (isDimensional) {
                    Text("المعيار: يحسب كل 4 متر مربع = 1 متر طولي", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = length,
                            onValueChange = { length = it },
                            label = { Text("الطول (متر)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = width,
                            onValueChange = { width = it },
                            label = { Text("العرض (متر)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )
                    }
                    Text("الكمية المحسوبة: ${calculatedQuantity.formatQty()} وحدة", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                } else {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("الكمية") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                }

                OutlinedTextField(
                    value = unitCost,
                    onValueChange = { unitCost = it },
                    label = { Text("سعر الشراء للوحدة") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
                
                val currentTotal = calculatedQuantity * (unitCost.toDoubleOrNull() ?: 0.0)
                Text("الإجمالي: ${currentTotal.formatCurrency()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
            }
        },
        confirmButton = {
            Button(onClick = {
                val finalCost = unitCost.toDoubleOrNull() ?: 0.0
                if (calculatedQuantity > 0 && finalCost >= 0) {
                    onSave(calculatedQuantity, finalCost)
                }
            }) {
                Text("إضافة للفاتورة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
'''
if 'fun PurchaseProductConfigDialog' not in content:
    content = content + '\n' + config_dialog_code

old_add_btn = '''                                if (currentQty == 0.0) {
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
                                } else {'''

new_add_btn = '''                                if (currentQty == 0.0) {
                                    Button(
                                        onClick = {
                                            selectedProductForConfig = product
                                        }
                                    ) {
                                        Text("إضافة وتحديد")
                                    }
                                } else {'''

content = content.replace(old_add_btn, new_add_btn)

show_config_logic = '''
        if (selectedProductForConfig != null) {
            PurchaseProductConfigDialog(
                product = selectedProductForConfig!!,
                onDismiss = { selectedProductForConfig = null },
                onSave = { qty, cost ->
                    val product = selectedProductForConfig!!
                    val existing = invoiceItems.find { it.product.id == product.id }
                    if (existing != null) {
                        existing.quantity = qty
                        existing.unitCost = cost
                        existing.lineTotal = qty * cost
                        val idx = invoiceItems.indexOf(existing)
                        if (idx != -1) invoiceItems[idx] = existing.copy()
                    } else {
                        invoiceItems.add(
                            PurchaseItemUiModel(
                                product = product,
                                quantity = qty,
                                unitCost = cost,
                                lineTotal = qty * cost
                            )
                        )
                    }
                    selectedProductForConfig = null
                }
            )
        }
'''

content = content.replace('if (showAddProductSheet) {', show_config_logic + '        if (showAddProductSheet) {')

card_start = r'''Card\(modifier = Modifier.fillMaxWidth\(\).padding\(bottom = 8.dp\)\) \{'''
card_new = r'''Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { selectedProductForConfig = item.product }) {'''
content = re.sub(card_start, card_new, content)

qty_str_old = r'''Text\(" \$\{item.quantity\} ", modifier = Modifier.padding\(horizontal = 8.dp\)\)'''
qty_str_new = r'''Text(" ${item.quantity.formatQty()} ", modifier = Modifier.padding(horizontal = 8.dp), fontWeight = FontWeight.Bold)'''
content = re.sub(qty_str_old, qty_str_new, content)

qty_str_old2 = r'''Text\(" \$\{currentQty.toInt\(\)\} ", modifier = Modifier.padding\(horizontal = 12.dp\), fontWeight = FontWeight.Bold\)'''
qty_str_new2 = r'''Text(" ${currentQty.formatQty()} ", modifier = Modifier.padding(horizontal = 12.dp), fontWeight = FontWeight.Bold)'''
content = re.sub(qty_str_old2, qty_str_new2, content)


with open('app/src/main/java/com/example/ui/screens/PurchasesScreen.kt', 'w') as f:
    f.write(content)
