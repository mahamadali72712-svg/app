package com.example.data.sync

import android.content.Context
import android.net.Uri
import android.provider.Settings
import androidx.sqlite.db.SimpleSQLiteQuery
import com.example.data.local.AppDatabase
import com.example.data.local.SyncLog
import org.dhatim.fastexcel.Workbook
import org.dhatim.fastexcel.reader.ReadableWorkbook
import java.io.OutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.ContentValues
import android.database.Cursor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.File
import org.dhatim.fastexcel.reader.Cell

sealed class ImportState {
    object Idle : ImportState()
    data class Loading(val percent: Int, val message: String) : ImportState()
    data class Success(val result: SyncResult) : ImportState()
    data class Error(val message: String) : ImportState()
}

class SyncEngine(private val context: Context, private val db: AppDatabase) {

    private val deviceId: String by lazy {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN_DEVICE"
    }

    private val tablesToSync = listOf(
        "products", "product_categories", "sales_invoices", "sales_invoice_items",
        "expenses", "suppliers", "supplier_payments", "customers", "customer_payments",
        "purchase_invoices", "purchase_invoice_items", "cash_movements"
    )

    // Keep getPreview and exportData exactly as they were... (Wait, I need to fetch their bodies)
