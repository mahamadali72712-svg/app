import re

with open('app/src/main/java/com/example/ui/viewmodels/StoreViewModel.kt', 'r') as f:
    content = f.read()

return_method = '''
    fun processReturn(
        originalInvoice: SalesInvoice,
        returnItems: List<SalesInvoiceItem>,
        refundCash: Boolean,
        returnAmount: Double,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.processReturn(originalInvoice, returnItems, refundCash, returnAmount)
            onSuccess()
        }
    }
'''

if 'fun processReturn' not in content:
    content = content.replace('    fun processSale(', return_method + '\n    fun processSale(')
    with open('app/src/main/java/com/example/ui/viewmodels/StoreViewModel.kt', 'w') as f:
        f.write(content)
