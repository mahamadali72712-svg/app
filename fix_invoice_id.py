import re

with open('app/src/main/java/com/example/ui/screens/PurchasesScreen.kt', 'r') as f:
    content = f.read()

# Fix PurchaseInvoiceItem creation
old_item = """PurchaseInvoiceItem(
                                        productId = it.product.id,
                                        quantity = it.quantity,
                                        unitCost = it.unitCost,
                                        lineTotal = it.lineTotal
                                    )"""
new_item = """PurchaseInvoiceItem(
                                        invoiceId = "", // set by repository
                                        productId = it.product.id,
                                        quantity = it.quantity,
                                        unitCost = it.unitCost,
                                        lineTotal = it.lineTotal
                                    )"""
content = content.replace(old_item, new_item)

with open('app/src/main/java/com/example/ui/screens/PurchasesScreen.kt', 'w') as f:
    f.write(content)

