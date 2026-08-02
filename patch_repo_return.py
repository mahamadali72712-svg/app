import re

with open('app/src/main/java/com/example/data/repository/StoreRepository.kt', 'r') as f:
    content = f.read()

return_method = '''
    suspend fun processReturn(
        originalInvoice: SalesInvoice,
        returnItems: List<SalesInvoiceItem>,
        refundCash: Boolean,
        returnAmount: Double
    ) {
        val returnInvoiceId = java.util.UUID.randomUUID().toString()
        val totalReturnAmount = returnItems.sumOf { it.lineTotal }
        val totalReturnCost = returnItems.sumOf { it.lineCost }
        val totalReturnProfit = returnItems.sumOf { it.lineProfit }
        
        // Save as a SalesInvoice with status = "RETURN" and negative values
        val invoice = SalesInvoice(
            id = returnInvoiceId,
            invoiceNumber = "RET-${System.currentTimeMillis()}",
            customerName = originalInvoice.customerName,
            customerId = originalInvoice.customerId,
            paymentType = "RETURN",
            totalAmount = -totalReturnAmount,
            totalCost = -totalReturnCost,
            discount = 0.0,
            paidAmount = if (refundCash) -returnAmount else 0.0,
            remainingAmount = if (!refundCash) -returnAmount else 0.0,
            totalProfit = -totalReturnProfit,
            status = "RETURN"
        )

        val updatedItems = returnItems.map { 
            it.copy(
                id = java.util.UUID.randomUUID().toString(),
                invoiceId = returnInvoiceId,
                quantity = -it.quantity, // negative quantity
                lineTotal = -it.lineTotal,
                lineCost = -it.lineCost,
                lineProfit = -it.lineProfit
            ) 
        }

        salesDao.insertInvoice(invoice)
        salesDao.insertInvoiceItems(updatedItems)

        // Add back to stock
        updatedItems.forEach {
            // Note: updateStock does stockQuantity = stockQuantity + amount. So passing it.quantity (which is negative here) would decrease stock. Wait!
            // I should pass positive value to increase stock. 
            // -it.quantity is positive because it.quantity was already negated above. 
            productDao.updateStock(it.productId, -it.quantity)
        }

        if (!refundCash && originalInvoice.customerId != null) {
            // decrease customer debt (negative amount means decrease balance)
            partiesDao.updateCustomerBalance(originalInvoice.customerId, -returnAmount)
        }

        if (refundCash && returnAmount > 0) {
            financeDao.insertCashMovement(
                CashMovement(
                    movementType = "RETURN",
                    direction = "OUT",
                    amount = returnAmount,
                    referenceType = "SALES_RETURN",
                    referenceId = returnInvoiceId,
                    note = "مرتجع مبيعات للفاتورة ${originalInvoice.invoiceNumber}"
                )
            )
        }
    }
'''

if 'fun processReturn' not in content:
    content = content.replace('    // Sales', '    // Sales\n' + return_method)
    with open('app/src/main/java/com/example/data/repository/StoreRepository.kt', 'w') as f:
        f.write(content)
