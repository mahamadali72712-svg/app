package com.example.data.repository

import com.example.data.local.*
import com.example.utils.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class StoreRepository(
    private val productDao: ProductDao,
    private val salesDao: SalesDao,
    private val financeDao: FinanceDao,
    private val purchaseDao: PurchaseDao,
    private val partiesDao: PartiesDao,
    private val syncDao: SyncDao,
    val syncEngine: com.example.data.sync.SyncEngine
) {

    // Products
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val archivedProducts: Flow<List<Product>> = productDao.getArchivedProducts()
    val lowStockProducts: Flow<List<Product>> = productDao.getLowStockProducts()
    val allCategories: Flow<List<ProductCategory>> = productDao.getAllCategories()

    suspend fun addProduct(product: Product) = productDao.insertProduct(product)
    suspend fun updateProduct(product: Product) = productDao.insertProduct(product) // using REPLACE
    suspend fun getProductById(id: String) = productDao.getProductById(id)

    suspend fun addCategory(category: ProductCategory) = productDao.insertCategory(category)
    
    suspend fun archiveProduct(productId: String) {
        val product = productDao.getProductById(productId)
        if (product != null) {
            productDao.insertProduct(product.copy(status = "ARCHIVED"))
        }
    }
    
    suspend fun restoreProduct(productId: String) {
        val product = productDao.getProductById(productId)
        if (product != null) {
            productDao.insertProduct(product.copy(status = "ACTIVE"))
        }
    }
    
    suspend fun canDeleteProduct(productId: String): Boolean {
        val salesCount = productDao.getSalesCountForProduct(productId)
        val purchasesCount = productDao.getPurchasesCountForProduct(productId)
        return salesCount == 0 && purchasesCount == 0
    }
    
    suspend fun deleteProduct(productId: String) {
        val product = productDao.getProductById(productId)
        if (product != null) {
            productDao.insertProduct(product.copy(isDeleted = 1))
        }
    }

    val allSalesInvoices: Flow<List<SalesInvoice>> = salesDao.getAllInvoices()
    val allPurchaseInvoices: Flow<List<PurchaseInvoice>> = purchaseDao.getAllInvoices()
    
    suspend fun getSalesInvoiceItems(invoiceId: String) = salesDao.getInvoiceItems(invoiceId)
    suspend fun getPurchaseInvoiceItems(invoiceId: String) = purchaseDao.getInvoiceItems(invoiceId)

    // Parties
    val allSuppliers: Flow<List<Supplier>> = partiesDao.getAllSuppliers()
    val allCustomers: Flow<List<Customer>> = partiesDao.getAllCustomers()
    
    suspend fun getSupplierById(id: String) = partiesDao.getSupplierById(id)
    suspend fun getSupplierPayments(supplierId: String) = partiesDao.getSupplierPayments(supplierId)
    suspend fun getSupplierPurchases(supplierId: String) = purchaseDao.getSupplierPurchases(supplierId)
    
    suspend fun getCustomerById(id: String) = partiesDao.getCustomerById(id)
    suspend fun getCustomerPayments(customerId: String) = partiesDao.getCustomerPayments(customerId)
    suspend fun getCustomerSales(customerId: String) = salesDao.getCustomerSales(customerId)
    
    suspend fun addSupplier(supplier: Supplier) = partiesDao.insertSupplier(supplier)
    suspend fun updateSupplier(supplier: Supplier) = partiesDao.insertSupplier(supplier.copy(updatedAt = System.currentTimeMillis(), syncStatus = 0))
    suspend fun archiveSupplier(id: String) = partiesDao.archiveSupplier(id)
    suspend fun addCustomer(customer: Customer) = partiesDao.insertCustomer(customer)
    
    suspend fun addSupplierPayment(supplierId: String, amount: Double, method: String, reference: String?, note: String?) {
        val payment = SupplierPayment(
            supplierId = supplierId,
            amount = amount,
            paymentMethod = method,
            reference = reference,
            note = note
        )
        partiesDao.insertSupplierPayment(payment)
        partiesDao.updateSupplierBalance(supplierId, -amount)
        
        financeDao.insertCashMovement(
            CashMovement(
                movementType = "SUPPLIER_PAYMENT",
                direction = "OUT",
                amount = amount,
                referenceType = "SUPPLIER_PAYMENT",
                referenceId = payment.id,
                note = note ?: "دفعة لمورد"
            )
        )
    }

    suspend fun addCustomerPayment(customerId: String, amount: Double, method: String, reference: String?, note: String?) {
        val payment = CustomerPayment(
            customerId = customerId,
            amount = amount,
            paymentMethod = method,
            reference = reference,
            note = note
        )
        partiesDao.insertCustomerPayment(payment)
        partiesDao.updateCustomerBalance(customerId, -amount)
        
        financeDao.insertCashMovement(
            CashMovement(
                movementType = "CUSTOMER_PAYMENT",
                direction = "IN",
                amount = amount,
                referenceType = "CUSTOMER_PAYMENT",
                referenceId = payment.id,
                note = note ?: "تحصيل دفعة من عميل"
            )
        )
    }

    suspend fun updateCustomer(customer: com.example.data.local.Customer) {
        partiesDao.insertCustomer(customer.copy(updatedAt = System.currentTimeMillis(), syncStatus = 0))
    }

    suspend fun archiveCustomer(id: String) {
        partiesDao.archiveCustomer(id)
    }

    // Expenses
    val allExpenses: Flow<List<Expense>> = financeDao.getAllExpenses()

    suspend fun updateExpense(expense: Expense) {
        financeDao.updateExpense(expense.copy(updatedAt = System.currentTimeMillis(), syncStatus = 0))
        val currentMovement = financeDao.getExportCashMovements().find { it.referenceId == expense.id }
        if (currentMovement != null) {
            financeDao.insertCashMovement(
                currentMovement.copy(
                    amount = expense.amount,
                    note = expense.note ?: expense.categoryId,
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = 0
                )
            )
        }
    }

    suspend fun archiveExpense(id: String) {
        financeDao.archiveExpense(id)
        val currentMovement = financeDao.getExportCashMovements().find { it.referenceId == id }
        if (currentMovement != null) {
            // Reverse or delete the cash movement. We can soft delete the movement or negate it.
            // Since we don't have isDeleted on cash_movements in this setup, let's just create a reversing entry or if there is soft delete, we'd use it.
            // Wait, does CashMovement have isDeleted? Let's check entities. We'll just negate it or add a counter movement for simplicity, OR if isDeleted exists, set it.
            // Let's assume it doesn't have soft delete, we'll just add a counter movement.
            financeDao.insertCashMovement(
                CashMovement(
                    movementType = "EXPENSE_REVERSAL",
                    amount = currentMovement.amount,
                    direction = "IN",
                    referenceType = "EXPENSE",
                    referenceId = id,
                    note = "عكس مصروف محذوف"
                )
            )
        }
    }
    suspend fun addExpense(expense: Expense) {
        financeDao.insertExpense(expense)
        
        // Handle cash movement out
        financeDao.insertCashMovement(
            CashMovement(
                movementType = "EXPENSE",
                direction = "OUT",
                amount = expense.amount,
                referenceType = "EXPENSE",
                referenceId = expense.id,
                note = expense.note ?: expense.categoryId
            )
        )
    }

    // Purchases
    suspend fun processPurchase(
        supplierId: String,
        items: List<PurchaseInvoiceItem>,
        discount: Double,
        paidAmount: Double
    ) {
        val invoiceId = UUID.randomUUID().toString()
        val totalAmount = items.preciseSumOf { it.lineTotal }.preciseSubtract(discount)

        val invoice = PurchaseInvoice(
            id = invoiceId,
            invoiceNumber = "PUR-${System.currentTimeMillis()}",
            supplierId = supplierId,
            paymentType = if (paidAmount >= totalAmount) "CASH" else if (paidAmount == 0.0) "CREDIT" else "PARTIAL",
            totalAmount = totalAmount,
            discount = discount,
            paidAmount = paidAmount,
            remainingAmount = totalAmount.preciseSubtract(paidAmount),
            status = if (totalAmount.preciseSubtract(paidAmount) <= 0.0) "PAID" else if (paidAmount > 0.0) "PARTIALLY_PAID" else "POSTED"
        )

        val updatedItems = items.map { it.copy(invoiceId = invoiceId) }

        purchaseDao.insertInvoice(invoice)
        purchaseDao.insertInvoiceItems(updatedItems)

        // Update Stock and Cost
        updatedItems.forEach { item ->
            productDao.updateStock(item.productId, item.quantity)
            // Ideally update cost average, for now we just keep the last cost.
            // productDao.updateCost(item.productId, item.unitCost) // Needs DAO method
        }

        // Supplier Balance
        if (invoice.remainingAmount > 0) {
            partiesDao.updateSupplierBalance(supplierId, invoice.remainingAmount)
        }

        // Cash Movement
        if (paidAmount > 0) {
            financeDao.insertCashMovement(
                CashMovement(
                    movementType = "PURCHASE_PAYMENT",
                    direction = "OUT",
                    amount = paidAmount,
                    referenceType = "PURCHASE_INVOICE",
                    referenceId = invoiceId,
                    note = "Purchase Payment"
                )
            )
        }
    }

    // Dashboard Stats
    val totalSales: Flow<Double?> = salesDao.getTotalSales()
    val totalProfit: Flow<Double?> = salesDao.getTotalProfit()
    // Finance
    val cashMovements: Flow<List<CashMovement>> = financeDao.getAllCashMovements()
    val cashBalance: Flow<Double?> = financeDao.getCashBalance()
    val operationalExpenses: Flow<Double?> = financeDao.getTotalOperationalExpenses()
    
    // Sync
    val syncLogs: Flow<List<SyncLog>> = syncDao.getAllSyncLogs()

    
    suspend fun generateReport(startDate: Long, endDate: Long): ReportData {
        val generator = ReportGenerator(productDao, salesDao, financeDao, partiesDao)
        return generator.generateReport(startDate, endDate)
    }

    // Sales

    suspend fun processReturn(
        originalInvoice: SalesInvoice,
        returnItems: List<SalesInvoiceItem>,
        refundCash: Boolean,
        returnAmount: Double
    ) {
        val returnInvoiceId = java.util.UUID.randomUUID().toString()
        val totalReturnAmount = returnItems.preciseSumOf { it.lineTotal }
        val totalReturnCost = returnItems.preciseSumOf { it.lineCost }
        val totalReturnProfit = returnItems.preciseSumOf { it.lineProfit }
        
        // Save as a SalesInvoice with status = "RETURNED" and negative values
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
            status = "RETURNED"
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

    suspend fun processSale(
        customerName: String,
        customerId: String?,
        items: List<SalesInvoiceItem>,
        discount: Double,
        paidAmount: Double
    ) {
        val invoiceId = UUID.randomUUID().toString()
        val totalAmount = items.preciseSumOf { it.lineTotal }.preciseSubtract(discount)
        val totalCost = items.preciseSumOf { it.lineCost }
        val totalProfit = items.preciseSumOf { it.lineProfit }.preciseSubtract(discount)
        val remainingAmount = totalAmount.preciseSubtract(paidAmount)

        val invoice = SalesInvoice(
            id = invoiceId,
            invoiceNumber = "INV-${System.currentTimeMillis()}",
            customerName = customerName.takeIf { it.isNotBlank() },
            customerId = customerId,
            paymentType = if (paidAmount >= totalAmount) "CASH" else if (paidAmount == 0.0) "CREDIT" else "PARTIAL",
            totalAmount = totalAmount,
            totalCost = totalCost,
            discount = discount,
            paidAmount = paidAmount,
            remainingAmount = remainingAmount,
            totalProfit = totalProfit,
            status = if (remainingAmount <= 0.0) "PAID" else if (paidAmount > 0.0) "PARTIALLY_PAID" else "POSTED"
        )

        val updatedItems = items.map { it.copy(invoiceId = invoiceId) }

        // 1. Save Invoice & Items
        salesDao.insertInvoice(invoice)
        salesDao.insertInvoiceItems(updatedItems)

        // 2. Update Stock
        updatedItems.forEach {
            productDao.updateStock(it.productId, -it.quantity)
        }

        // 3. Update Customer Balance
        if (remainingAmount > 0 && customerId != null) {
            partiesDao.updateCustomerBalance(customerId, remainingAmount)
        }

        // 4. Cash Movement (if paid)
        if (paidAmount > 0) {
            val cm = CashMovement(
                movementType = "SALE",
                direction = "IN",
                amount = paidAmount,
                referenceType = "SALES_INVOICE",
                referenceId = invoiceId,
                note = "Sale Payment"
            )
            financeDao.insertCashMovement(cm)
        }
    }

    suspend fun voidSalesInvoice(invoiceId: String) {
        val invoice = salesDao.getInvoiceById(invoiceId) ?: return
        if (invoice.status == "VOIDED") return

        // 1. Mark status as VOIDED
        val voidedInvoice = invoice.copy(
            status = "VOIDED",
            updatedAt = System.currentTimeMillis(),
            syncStatus = 0
        )
        salesDao.insertInvoice(voidedInvoice)

        // 2. Fetch invoice items
        val items = salesDao.getInvoiceItems(invoiceId)

        // 3. Reverse Stock (Add items back to stock)
        items.forEach { item ->
            productDao.updateStock(item.productId, item.quantity)
        }

        // 4. Reverse Customer Balance
        if (invoice.remainingAmount > 0 && invoice.customerId != null) {
            partiesDao.updateCustomerBalance(invoice.customerId, -invoice.remainingAmount)
        }

        // 5. Reverse Cash Movement (Insert opposite OUT movement)
        if (invoice.paidAmount > 0) {
            financeDao.insertCashMovement(
                CashMovement(
                    movementType = "SALE_REVERSAL",
                    direction = "OUT",
                    amount = invoice.paidAmount,
                    referenceType = "SALES_INVOICE",
                    referenceId = invoiceId,
                    note = "عكس فاتورة مبيعات ملغاة #${invoice.invoiceNumber}"
                )
            )
        }
    }

    suspend fun voidPurchaseInvoice(invoiceId: String) {
        val invoice = purchaseDao.getInvoiceById(invoiceId) ?: return
        if (invoice.status == "VOIDED") return

        // 1. Mark status as VOIDED
        val voidedInvoice = invoice.copy(
            status = "VOIDED",
            updatedAt = System.currentTimeMillis(),
            syncStatus = 0
        )
        purchaseDao.insertInvoice(voidedInvoice)

        // 2. Fetch invoice items
        val items = purchaseDao.getInvoiceItems(invoiceId)

        // 3. Reverse Stock (Deduct items from stock)
        items.forEach { item ->
            productDao.updateStock(item.productId, -item.quantity)
        }

        // 4. Reverse Supplier Balance
        if (invoice.remainingAmount > 0) {
            partiesDao.updateSupplierBalance(invoice.supplierId, -invoice.remainingAmount)
        }

        // 5. Reverse Cash Movement (Insert opposite IN movement)
        if (invoice.paidAmount > 0) {
            financeDao.insertCashMovement(
                CashMovement(
                    movementType = "PURCHASE_REVERSAL",
                    direction = "IN",
                    amount = invoice.paidAmount,
                    referenceType = "PURCHASE_INVOICE",
                    referenceId = invoiceId,
                    note = "عكس فاتورة مشتريات ملغاة #${invoice.invoiceNumber}"
                )
            )
        }
    }
}
