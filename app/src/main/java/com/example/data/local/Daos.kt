package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE isDeleted = 0 AND status = 'ACTIVE' ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE isDeleted = 0 AND status = 'ARCHIVED' ORDER BY name ASC")
    fun getArchivedProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE isDeleted = 0 AND stockQuantity <= minStockAlert ORDER BY stockQuantity ASC")
    fun getLowStockProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products")
    suspend fun getExportProducts(): List<Product>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: String): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>)

    @Query("UPDATE products SET stockQuantity = stockQuantity + :amount WHERE id = :id")
    suspend fun updateStock(id: String, amount: Double)

    @Query("SELECT * FROM product_categories WHERE isDeleted = 0 ORDER BY sortOrder ASC")
    fun getAllCategories(): Flow<List<ProductCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: ProductCategory)

    @Query("SELECT COUNT(*) FROM sales_invoice_items WHERE productId = :productId")
    suspend fun getSalesCountForProduct(productId: String): Int

    @Query("SELECT COUNT(*) FROM purchase_invoice_items WHERE productId = :productId")
    suspend fun getPurchasesCountForProduct(productId: String): Int
}

@Dao
interface SalesDao {
    @Query("SELECT * FROM sales_invoices WHERE isDeleted = 0 ORDER BY invoiceDate DESC")
    fun getAllInvoices(): Flow<List<SalesInvoice>>

    @Query("SELECT * FROM sales_invoices WHERE id = :id")
    suspend fun getInvoiceById(id: String): SalesInvoice?

    @Query("SELECT * FROM sales_invoices")
    suspend fun getExportInvoices(): List<SalesInvoice>

    @Query("SELECT * FROM sales_invoice_items")
    suspend fun getExportInvoiceItems(): List<SalesInvoiceItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: SalesInvoice)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoices(invoices: List<SalesInvoice>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItems(items: List<SalesInvoiceItem>)
    
    @Query("SELECT * FROM sales_invoice_items WHERE invoiceId = :invoiceId")
    suspend fun getInvoiceItems(invoiceId: String): List<SalesInvoiceItem>

    @Query("SELECT * FROM sales_invoices WHERE customerId = :customerId AND isDeleted = 0 ORDER BY invoiceDate DESC")
    suspend fun getCustomerSales(customerId: String): List<SalesInvoice>
    
    @Query("SELECT SUM(totalAmount) FROM sales_invoices WHERE isDeleted = 0")
    fun getTotalSales(): Flow<Double?>
    
    @Query("SELECT SUM(totalProfit) FROM sales_invoices WHERE isDeleted = 0")
    fun getTotalProfit(): Flow<Double?>
}

@Dao
interface PurchaseDao {
    @Query("SELECT * FROM purchase_invoices WHERE isDeleted = 0 ORDER BY invoiceDate DESC")
    fun getAllInvoices(): Flow<List<PurchaseInvoice>>

    @Query("SELECT * FROM purchase_invoices WHERE id = :id")
    suspend fun getInvoiceById(id: String): PurchaseInvoice?

    @Query("SELECT * FROM purchase_invoices")
    suspend fun getExportInvoices(): List<PurchaseInvoice>

    @Query("SELECT * FROM purchase_invoice_items")
    suspend fun getExportInvoiceItems(): List<PurchaseInvoiceItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: PurchaseInvoice)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoices(invoices: List<PurchaseInvoice>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItems(items: List<PurchaseInvoiceItem>)

    @Query("SELECT * FROM purchase_invoice_items WHERE invoiceId = :invoiceId")
    suspend fun getInvoiceItems(invoiceId: String): List<PurchaseInvoiceItem>

    @Query("SELECT * FROM purchase_invoices WHERE supplierId = :supplierId AND isDeleted = 0 ORDER BY invoiceDate DESC")
    suspend fun getSupplierPurchases(supplierId: String): List<PurchaseInvoice>
}

@Dao
interface PartiesDao {
    @Query("SELECT * FROM suppliers WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<Supplier>>

    @Query("SELECT * FROM suppliers")
    suspend fun getExportSuppliers(): List<Supplier>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: Supplier)

    @Query("UPDATE suppliers SET isDeleted = 1, updatedAt = :timestamp, syncStatus = 0 WHERE id = :id")
    suspend fun archiveSupplier(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM suppliers WHERE id = :id")
    suspend fun getSupplierById(id: String): Supplier?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuppliers(suppliers: List<Supplier>)

    @Query("UPDATE suppliers SET balance = balance + :amount WHERE id = :id")
    suspend fun updateSupplierBalance(id: String, amount: Double)

    @Query("SELECT * FROM supplier_payments WHERE supplierId = :supplierId ORDER BY paymentDate DESC")
    suspend fun getSupplierPayments(supplierId: String): List<SupplierPayment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplierPayment(payment: SupplierPayment)

    @Query("SELECT * FROM customers WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers")
    suspend fun getExportCustomers(): List<Customer>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: String): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer)

    @Query("UPDATE customers SET isDeleted = 1, updatedAt = :timestamp, syncStatus = 0 WHERE id = :id")
    suspend fun archiveCustomer(id: String, timestamp: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<Customer>)

    @Query("UPDATE customers SET balance = balance + :amount WHERE id = :id")
    suspend fun updateCustomerBalance(id: String, amount: Double)

    @Query("SELECT * FROM customer_payments WHERE customerId = :customerId ORDER BY paymentDate DESC")
    suspend fun getCustomerPayments(customerId: String): List<CustomerPayment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomerPayment(payment: CustomerPayment)
}

@Dao
interface FinanceDao {
    @Query("SELECT * FROM cash_movements ORDER BY movementDate DESC")
    fun getAllCashMovements(): Flow<List<CashMovement>>

    @Query("SELECT * FROM cash_movements")
    suspend fun getExportCashMovements(): List<CashMovement>

    @Query("SELECT * FROM expenses")
    suspend fun getExportExpenses(): List<Expense>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashMovement(movement: CashMovement)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashMovements(movements: List<CashMovement>)

    @Query("SELECT SUM(CASE WHEN direction = 'IN' THEN amount ELSE -amount END) FROM cash_movements")
    fun getCashBalance(): Flow<Double?>
    
    @Query("SELECT SUM(amount) FROM expenses WHERE expenseType = 'OPERATIONAL' AND isDeleted = 0")
    fun getTotalOperationalExpenses(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Query("UPDATE expenses SET isDeleted = 1, updatedAt = :timestamp, syncStatus = 0 WHERE id = :id")
    suspend fun archiveExpense(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM expenses WHERE isDeleted = 0 ORDER BY expenseDate DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<Expense>)
}

@Dao
interface SyncDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncLog(log: SyncLog)

    @Query("SELECT * FROM sync_logs ORDER BY syncDate DESC")
    fun getAllSyncLogs(): Flow<List<SyncLog>>
}
