package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Product::class,
        ProductCategory::class,
        SalesInvoice::class,
        SalesInvoiceItem::class,
        Expense::class,
        CashMovement::class,
        Supplier::class,
        SupplierPayment::class,
        Customer::class,
        CustomerPayment::class,
        PurchaseInvoice::class,
        PurchaseInvoiceItem::class,
        SyncLog::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun salesDao(): SalesDao
    abstract fun financeDao(): FinanceDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun partiesDao(): PartiesDao
    abstract fun syncDao(): SyncDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val tables = listOf(
            "product_categories", "products", "sales_invoices", "sales_invoice_items",
            "expenses", "suppliers", "supplier_payments", "customers", "customer_payments",
            "purchase_invoices", "purchase_invoice_items", "cash_movements"
        )

        private val roomCallback = object : RoomDatabase.Callback() {
            override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                super.onCreate(db)
                tables.forEach { table ->
                    db.execSQL("""
                        CREATE TRIGGER IF NOT EXISTS trigger_update_$table
                        AFTER UPDATE ON $table
                        FOR EACH ROW
                        WHEN NEW.updatedAt <= OLD.updatedAt AND NEW.syncStatus != 1
                        BEGIN
                            UPDATE $table SET 
                                version = OLD.version + 1, 
                                syncStatus = 2,
                                updatedAt = (strftime('%s','now') * 1000)
                            WHERE id = OLD.id;
                        END;
                    """.trimIndent())
                }
            }
            override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                super.onOpen(db)
                tables.forEach { table ->
                    db.execSQL("""
                        CREATE TRIGGER IF NOT EXISTS trigger_update_$table
                        AFTER UPDATE ON $table
                        FOR EACH ROW
                        WHEN NEW.updatedAt <= OLD.updatedAt AND NEW.syncStatus != 1
                        BEGIN
                            UPDATE $table SET 
                                version = OLD.version + 1, 
                                syncStatus = 2,
                                updatedAt = (strftime('%s','now') * 1000)
                            WHERE id = OLD.id;
                        END;
                    """.trimIndent())
                }
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "furniture_store_db"
                ).addCallback(roomCallback).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
