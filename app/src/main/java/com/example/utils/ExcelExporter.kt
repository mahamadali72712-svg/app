package com.example.utils

import android.content.Context
import android.net.Uri
import com.example.data.repository.ReportData
import org.dhatim.fastexcel.Workbook
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun exportReportToExcel(context: Context, uri: Uri, reportData: ReportData) {
    withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri)?.use { os: OutputStream ->
            val wb = Workbook(os, "StoreApp", "1.0")
            
            // 1. Summary Sheet
            val summarySheet = wb.newWorksheet("الملخص")
            summarySheet.value(0, 0, "إجمالي المبيعات")
            summarySheet.value(0, 1, reportData.totalSales)
            summarySheet.value(1, 0, "عدد الفواتير")
            summarySheet.value(1, 1, reportData.invoiceCount)
            summarySheet.value(2, 0, "إجمالي الأرباح")
            summarySheet.value(2, 1, reportData.grossProfit)
            summarySheet.value(3, 0, "صافي الربح التشغيلي")
            summarySheet.value(3, 1, reportData.netOperatingProfit)
            summarySheet.value(4, 0, "المسحوبات الشخصية")
            summarySheet.value(4, 1, reportData.ownerDraw)
            summarySheet.value(5, 0, "الرصيد الافتتاحي")
            summarySheet.value(5, 1, reportData.openingCashBalance)
            summarySheet.value(6, 0, "إجمالي الداخل")
            summarySheet.value(6, 1, reportData.cashIn)
            summarySheet.value(7, 0, "إجمالي الخارج")
            summarySheet.value(7, 1, reportData.cashOut)
            summarySheet.value(8, 0, "الرصيد الختامي للصندوق")
            summarySheet.value(8, 1, reportData.closingCashBalance)
            
            // 2. Products Performance
            val productsSheet = wb.newWorksheet("أداء المنتجات")
            productsSheet.value(0, 0, "المنتج")
            productsSheet.value(0, 1, "الكمية المباعة")
            productsSheet.value(0, 2, "إجمالي المبيعات")
            productsSheet.value(0, 3, "إجمالي الربح")
            
            reportData.topProductsByQty.forEachIndexed { index, product ->
                val r = index + 1
                productsSheet.value(r, 0, product.productName)
                productsSheet.value(r, 1, product.qty)
                productsSheet.value(r, 2, product.totalSales)
                productsSheet.value(r, 3, product.profit)
            }

            // 3. Expenses Detail
            val expensesSheet = wb.newWorksheet("تفاصيل المصروفات")
            expensesSheet.value(0, 0, "الفئة")
            expensesSheet.value(0, 1, "الإجمالي")
            
            var expenseRow = 1
            reportData.expensesByCategory.forEach { (cat, amount) ->
                expensesSheet.value(expenseRow, 0, cat)
                expensesSheet.value(expenseRow, 1, amount)
                expenseRow++
            }

            // 4. Sales Detail
            val salesSheet = wb.newWorksheet("المبيعات اليومية")
            salesSheet.value(0, 0, "التاريخ")
            salesSheet.value(0, 1, "المبيعات")
            salesSheet.value(0, 2, "الأرباح")
            
            var salesRow = 1
            reportData.dailySales.forEach { (date, amount) ->
                salesSheet.value(salesRow, 0, date)
                salesSheet.value(salesRow, 1, amount)
                salesSheet.value(salesRow, 2, reportData.dailyProfit[date] ?: 0.0)
                salesRow++
            }
            
            wb.finish()
        }
    }
}
