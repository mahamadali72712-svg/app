import re

with open('app/src/main/java/com/example/ui/screens/SalesHistoryScreen.kt', 'r') as f:
    content = f.read()

# Make sure formatQty is imported
if 'import com.example.utils.formatQty' not in content:
    content = content.replace('import com.example.utils.formatCurrency', 'import com.example.utils.formatCurrency\nimport com.example.utils.formatQty\nimport android.widget.Toast\nimport androidx.compose.ui.platform.LocalContext\nimport androidx.compose.ui.Alignment\nimport androidx.compose.foundation.rememberScrollState\nimport androidx.compose.foundation.verticalScroll')


# Add the ReturnDialog component
return_dialog_code = '''
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReturnDialog(
    invoice: SalesInvoice,
    items: List<SalesInvoiceItem>,
    viewModel: StoreViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var returnQuantities by remember { mutableStateOf(items.associate { it.id to 0.0 }) }
    var refundCash by remember { mutableStateOf(true) }
    val context = LocalContext.current
    var isProcessing by remember { mutableStateOf(false) }

    val totalReturnAmount = items.sumOf { item ->
        val qty = returnQuantities[item.id] ?: 0.0
        qty * item.unitPrice
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إرجاع من الفاتورة ${invoice.invoiceNumber}") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text("حدد الكميات المراد إرجاعها:", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                items.forEach { item ->
                    var productName by remember { mutableStateOf("جاري التحميل...") }
                    LaunchedEffect(item.productId) {
                        val p = viewModel.getProductById(item.productId)
                        productName = p?.name ?: "منتج محذوف"
                    }
                    val currentQty = returnQuantities[item.id] ?: 0.0

                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(productName, fontWeight = FontWeight.Bold)
                            Text("السعر: ${item.unitPrice.formatCurrency()} | الكمية الأصلية: ${item.quantity.formatQty()}")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("مرتجع:")
                                IconButton(onClick = { 
                                    if (currentQty > 0) returnQuantities = returnQuantities.toMutableMap().apply { put(item.id, currentQty - 1) } 
                                }) { Text("-", fontWeight = FontWeight.Bold, fontSize = 20.sp) }
                                
                                OutlinedTextField(
                                    value = if (currentQty == 0.0) "" else currentQty.formatQty(),
                                    onValueChange = { 
                                        val v = it.toDoubleOrNull() ?: 0.0
                                        if (v <= item.quantity) {
                                            returnQuantities = returnQuantities.toMutableMap().apply { put(item.id, v) }
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                )
                                
                                IconButton(onClick = { 
                                    if (currentQty < item.quantity) returnQuantities = returnQuantities.toMutableMap().apply { put(item.id, currentQty + 1) }
                                }) { Text("+", fontWeight = FontWeight.Bold, fontSize = 20.sp) }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("إجمالي المرتجع: ${totalReturnAmount.formatCurrency()}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                
                if (invoice.customerId != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("طريقة إرجاع المبلغ:", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = refundCash, onClick = { refundCash = true })
                        Text("إرجاع نقداً من الصندوق", modifier = Modifier.clickable { refundCash = true })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = !refundCash, onClick = { refundCash = false })
                        Text("خصم من حساب العميل", modifier = Modifier.clickable { refundCash = false })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val returnItemsToProcess = items.filter { (returnQuantities[it.id] ?: 0.0) > 0 }.map { 
                        val rq = returnQuantities[it.id] ?: 0.0
                        it.copy(
                            quantity = rq,
                            lineTotal = rq * it.unitPrice,
                            lineCost = rq * it.unitCost,
                            lineProfit = (rq * it.unitPrice) - (rq * it.unitCost)
                        )
                    }
                    if (returnItemsToProcess.isEmpty()) {
                        Toast.makeText(context, "لم تقم بتحديد أي كمية للترجيع", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isProcessing = true
                    viewModel.processReturn(
                        originalInvoice = invoice,
                        returnItems = returnItemsToProcess,
                        refundCash = refundCash,
                        returnAmount = totalReturnAmount,
                        onSuccess = {
                            isProcessing = false
                            Toast.makeText(context, "تمت عملية الترجيع بنجاح", Toast.LENGTH_SHORT).show()
                            onSuccess()
                        }
                    )
                },
                enabled = !isProcessing
            ) {
                if (isProcessing) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                else Text("تأكيد الإرجاع")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isProcessing) {
                Text("إلغاء")
            }
        }
    )
}
'''

if 'fun ReturnDialog' not in content:
    content = content + '\n' + return_dialog_code


# Add the state for the return dialog
state_return_invoice = '''
    var selectedInvoice by remember { mutableStateOf<SalesInvoice?>(null) }
    var invoiceItems by remember { mutableStateOf<List<SalesInvoiceItem>>(emptyList()) }
    var returnInvoice by remember { mutableStateOf<SalesInvoice?>(null) }
    var returnItemsList by remember { mutableStateOf<List<SalesInvoiceItem>>(emptyList()) }
'''

content = content.replace('''    var selectedInvoice by remember { mutableStateOf<SalesInvoice?>(null) }
    var invoiceItems by remember { mutableStateOf<List<SalesInvoiceItem>>(emptyList()) }''', state_return_invoice)

# Add Return logic
# Wait, we need to find confirmButton in the original AlertDialog
confirm_button_search = '''            confirmButton = {
                TextButton(onClick = { selectedInvoice = null }) {
                    Text("إغلاق")
                }
            }'''

confirm_button_replace = '''            confirmButton = {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    if (inv.status != "RETURN") {
                        Button(onClick = {
                            returnInvoice = inv
                            returnItemsList = invoiceItems
                            selectedInvoice = null
                        }) {
                            Text("إرجاع منتجات")
                        }
                    }
                    TextButton(onClick = { selectedInvoice = null }) {
                        Text("إغلاق")
                    }
                }
            }'''

content = content.replace(confirm_button_search, confirm_button_replace)

show_return_dialog = '''
    if (returnInvoice != null) {
        ReturnDialog(
            invoice = returnInvoice!!,
            items = returnItemsList,
            viewModel = viewModel,
            onDismiss = { returnInvoice = null },
            onSuccess = {
                returnInvoice = null
                // Maybe refresh the list, but it's StateFlow so it should refresh automatically
            }
        )
    }
'''

content = content.replace('    var searchQuery by remember { mutableStateOf("") }', show_return_dialog + '    var searchQuery by remember { mutableStateOf("") }')


with open('app/src/main/java/com/example/ui/screens/SalesHistoryScreen.kt', 'w') as f:
    f.write(content)
