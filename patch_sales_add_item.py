import re

with open('app/src/main/java/com/example/ui/screens/SalesScreen.kt', 'r') as f:
    content = f.read()

# We need to add a config dialog state
state_declarations = '''    var showAddProductSheet by remember { mutableStateOf(false) }
    var selectedProductForConfig by remember { mutableStateOf<Product?>(null) }'''

content = content.replace('    var showAddProductSheet by remember { mutableStateOf(false) }', state_declarations)

# Now, we define the ProductConfigDialog composable and place it at the end of SalesScreen
config_dialog_code = '''
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductConfigDialog(
    product: Product,
    onDismiss: () -> Unit,
    onSave: (quantity: Double, unitPrice: Double) -> Unit
) {
    var isDimensional by remember { mutableStateOf(false) }
    var length by remember { mutableStateOf("") }
    var width by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var unitPrice by remember { mutableStateOf(product.suggestedPrice.toString()) }

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
                    Text("بيع بالمقاس (طول × عرض)")
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
                    value = unitPrice,
                    onValueChange = { unitPrice = it },
                    label = { Text("سعر البيع للوحدة") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
                
                val currentTotal = calculatedQuantity * (unitPrice.toDoubleOrNull() ?: 0.0)
                Text("الإجمالي: ${currentTotal.formatCurrency()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                
                val maxAllowed = product.stockQuantity
                if (calculatedQuantity > maxAllowed) {
                    Text("تحذير: الكمية المطلوبة أكبر من المخزون المتوفر (${maxAllowed.formatQty()})", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val finalPrice = unitPrice.toDoubleOrNull() ?: 0.0
                if (calculatedQuantity > 0 && finalPrice >= 0) {
                    onSave(calculatedQuantity, finalPrice)
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
if 'fun ProductConfigDialog' not in content:
    content = content + '\n' + config_dialog_code

# We need to change the add to cart logic inside showAddProductSheet
# Find the button that adds the item
old_add_btn = '''                                if (currentQty == 0.0) {
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

# We also need to change the + button in the else branch to allow config or just increment by 1
# Actually, the user wants custom prices per item. If they click + it increments by 1. That's fine for simple items, but for dimensional ones they might want to click the item itself to edit. Let's just allow editing from the main screen cart items!
# In the invoiceItems display loop in SalesScreen, let's make the items clickable to edit quantity/price.
# Let's add the ProductConfigDialog showing condition:
show_config_logic = '''
        if (selectedProductForConfig != null) {
            ProductConfigDialog(
                product = selectedProductForConfig!!,
                onDismiss = { selectedProductForConfig = null },
                onSave = { qty, price ->
                    val product = selectedProductForConfig!!
                    if (qty > product.stockQuantity) {
                        Toast.makeText(context, "المخزون المتوفر ${product.stockQuantity} فقط", Toast.LENGTH_SHORT).show()
                    }
                    val existing = invoiceItems.find { it.product.id == product.id }
                    if (existing != null) {
                        existing.quantity = qty
                        existing.unitPrice = price
                        existing.lineTotal = qty * price
                        existing.lineCost = qty * existing.unitCost
                        existing.lineProfit = existing.lineTotal - existing.lineCost
                        val idx = invoiceItems.indexOf(existing)
                        if (idx != -1) invoiceItems[idx] = existing.copy()
                    } else {
                        invoiceItems.add(
                            InvoiceItemUiModel(
                                product = product,
                                quantity = qty,
                                unitPrice = price,
                                unitCost = product.costPrice,
                                lineTotal = qty * price,
                                lineCost = qty * product.costPrice,
                                lineProfit = (qty * price) - (qty * product.costPrice)
                            )
                        )
                    }
                    selectedProductForConfig = null
                }
            )
        }
'''

content = content.replace('if (showAddProductSheet) {', show_config_logic + '        if (showAddProductSheet) {')

# Edit item logic: inside the main LazyColumn for items(invoiceItems)
# Find the Card in the cart list
card_start = r'''Card\(modifier = Modifier.fillMaxWidth\(\).padding\(bottom = 8.dp\)\) \{'''
card_new = r'''Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { selectedProductForConfig = item.product }) {'''
content = re.sub(card_start, card_new, content)

# Change Text(" ${item.quantity.toInt()} ") to formatQty()
qty_str_old = r'''Text\(" \$\{item.quantity.toInt\(\)\} ", modifier = Modifier.padding\(horizontal = 12.dp\), fontWeight = FontWeight.Bold\)'''
qty_str_new = r'''Text(" ${item.quantity.formatQty()} ", modifier = Modifier.padding(horizontal = 12.dp), fontWeight = FontWeight.Bold)'''
content = re.sub(qty_str_old, qty_str_new, content)

qty_str_old2 = r'''Text\(" \$\{currentQty.toInt\(\)\} ", modifier = Modifier.padding\(horizontal = 12.dp\), fontWeight = FontWeight.Bold\)'''
qty_str_new2 = r'''Text(" ${currentQty.formatQty()} ", modifier = Modifier.padding(horizontal = 12.dp), fontWeight = FontWeight.Bold)'''
content = re.sub(qty_str_old2, qty_str_new2, content)


with open('app/src/main/java/com/example/ui/screens/SalesScreen.kt', 'w') as f:
    f.write(content)
