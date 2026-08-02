import re

# 1. Fix PurchasesScreen.kt
with open('app/src/main/java/com/example/ui/screens/PurchasesScreen.kt', 'r') as f:
    content = f.read()

# Add missing class
if 'data class PurchaseItemUiModel' not in content:
    class_def = """data class PurchaseItemUiModel(
    val product: com.example.data.local.Product,
    var quantity: Double,
    var unitCost: Double,
    var lineTotal: Double
)

@OptIn"""
    content = content.replace('@OptIn', class_def, 1)

# Fix processPurchase
process_regex = r'viewModel\.addPurchaseInvoice\(\s*supplierId = sId,\s*items = items,\s*totalAmount = totalCost,\s*paidAmount = finalPaidAmount,\s*onSuccess = \{'
new_process = """viewModel.processPurchase(
                                    supplierId = sId,
                                    items = items,
                                    discount = 0.0,
                                    paidAmount = finalPaidAmount,
                                    onSuccess = {"""
content = re.sub(process_regex, new_process, content)

with open('app/src/main/java/com/example/ui/screens/PurchasesScreen.kt', 'w') as f:
    f.write(content)


# 2. Fix PurchasesHistoryScreen.kt
with open('app/src/main/java/com/example/ui/screens/PurchasesHistoryScreen.kt', 'r') as f:
    content_history = f.read()

# Add rememberCoroutineScope
if 'val scope = rememberCoroutineScope()' not in content_history:
    content_history = content_history.replace('val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()', 'val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()\n    val scope = rememberCoroutineScope()')

# Fix getPurchaseInvoiceItems call
old_click = """onClick = {
                                    viewModel.getPurchaseInvoiceItems(invoice.id) { items ->
                                        invoiceItems = items
                                        selectedInvoice = invoice
                                    }
                                }"""
new_click = """onClick = {
                                    scope.launch {
                                        invoiceItems = viewModel.getPurchaseInvoiceItems(invoice.id)
                                        selectedInvoice = invoice
                                    }
                                }"""
content_history = content_history.replace(old_click, new_click)

# Add kotlinx.coroutines.launch import
if 'kotlinx.coroutines.launch' not in content_history:
    content_history = content_history.replace('import androidx.compose.runtime.*', 'import androidx.compose.runtime.*\nimport kotlinx.coroutines.launch')

with open('app/src/main/java/com/example/ui/screens/PurchasesHistoryScreen.kt', 'w') as f:
    f.write(content_history)

