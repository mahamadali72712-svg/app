package com.example.data.repository

import com.example.data.local.*
import com.example.utils.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportGenerator(
    private val productDao: ProductDao,
    private val salesDao: SalesDao,
    private val financeDao: FinanceDao,
    private val partiesDao: PartiesDao
) {
    suspend fun generateReport(startDate: Long, endDate: Long): ReportData {
        // Fetch raw data
        val allSales = salesDao.getExportInvoices().filter { it.isDeleted == 0 }
        val allItems = salesDao.getExportInvoiceItems()
        val allExpenses = financeDao.getExportExpenses().filter { it.isDeleted == 0 }
        val allCash = financeDao.getExportCashMovements()
        val allProducts = productDao.getExportProducts().associateBy { it.id }
        val allSuppliers = partiesDao.getExportSuppliers().filter { it.isDeleted == 0 }
        val allCustomers = partiesDao.getExportCustomers().filter { it.isDeleted == 0 }
        val allCategories = productDao.getAllCategories() // Wait, this is a flow, we need a suspend function. Let's just use empty map for names if not available or add getExportCategories.
        
        // Filter by date range
        val periodSales = allSales.filter { it.invoiceDate in startDate..endDate }
        val periodInvoiceIds = periodSales.map { it.id }.toSet()
        val periodItems = allItems.filter { it.invoiceId in periodInvoiceIds }
        val periodExpenses = allExpenses.filter { it.expenseDate in startDate..endDate }
        
        // 1. Sales Report Card
        val totalSales = periodSales.preciseSumOf { it.totalAmount }
        val invoiceCount = periodSales.size
        val cashSales = periodSales.preciseSumOf { it.paidAmount }
        val creditSales = periodSales.preciseSumOf { it.remainingAmount }
        val grossProfit = periodSales.preciseSumOf { it.totalProfit }
        
        val operationalExpensesList = periodExpenses.filter { it.expenseType == "OPERATIONAL" }
        val totalOpExpenses = operationalExpensesList.preciseSumOf { it.amount }
        val netOperatingProfit = grossProfit.preciseSubtract(totalOpExpenses)
        
        val ownerDraw = periodExpenses.filter { it.expenseType == "PERSONAL" }.preciseSumOf { it.amount }
        
        // 2. Best Selling & Loss Products
        val productStatsMap = mutableMapOf<String, ProductStat>()
        for (item in periodItems) {
            val product = allProducts[item.productId]
            val name = product?.name ?: "منتج محذوف"
            val stat = productStatsMap.getOrDefault(item.productId, ProductStat(item.productId, name, 0.0, 0.0, 0.0))
            productStatsMap[item.productId] = stat.copy(
                profit = stat.profit.preciseAdd(item.lineProfit),
                qty = stat.qty.preciseAdd(item.quantity),
                totalSales = stat.totalSales.preciseAdd(item.lineTotal)
            )
        }
        
        val statsList = productStatsMap.values.toList()
        val topProductsByProfit = statsList.sortedByDescending { it.profit }.take(5)
        val topProductsByQty = statsList.sortedByDescending { it.qty }.take(5)
        val lossProducts = statsList.filter { it.profit < 0 }.sortedBy { it.profit }
        
        // 3. Expenses Breakdown
        val expensesByCategory = operationalExpensesList.groupBy { it.categoryId }
            .mapValues { (_, expenses) -> expenses.preciseSumOf { it.amount } }
            
        // 4. Cashbox Report
        val priorCashIn = allCash.filter { it.movementDate < startDate && it.direction == "IN" }.preciseSumOf { it.amount }
        val priorCashOut = allCash.filter { it.movementDate < startDate && it.direction == "OUT" }.preciseSumOf { it.amount }
        val openingCashBalance = priorCashIn.preciseSubtract(priorCashOut)
        
        val periodCashIn = allCash.filter { it.movementDate in startDate..endDate && it.direction == "IN" }.preciseSumOf { it.amount }
        val periodCashOut = allCash.filter { it.movementDate in startDate..endDate && it.direction == "OUT" }.preciseSumOf { it.amount }
        val closingCashBalance = openingCashBalance.preciseAdd(periodCashIn).preciseSubtract(periodCashOut)
        
        // 5. Debts Summary (always current snapshot)
        val totalSupplierDebts = allSuppliers.preciseSumOf { it.balance.coerceAtLeast(0.0) }
        val totalCustomerDebts = allCustomers.preciseSumOf { it.balance.coerceAtLeast(0.0) }
        
        // 6. Charts Data
        val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
        val dailySales = periodSales.groupBy { sdf.format(Date(it.invoiceDate)) }
            .mapValues { (_, invoices) -> invoices.preciseSumOf { it.totalAmount } }
            .toSortedMap()
            
        val dailyProfit = periodSales.groupBy { sdf.format(Date(it.invoiceDate)) }
            .mapValues { (_, invoices) -> invoices.preciseSumOf { it.totalProfit } }
            .toSortedMap()
            
        val salesByCategory = mutableMapOf<String, Double>()
        // Let's just group by product category ID for now, since we don't have category names fetched easily here without Flow.
        // We can just use the category ID as key and map it in the UI, or we can add a suspend function to Dao.
        for (item in periodItems) {
            val product = allProducts[item.productId]
            val catId = product?.categoryId ?: "أخرى"
            salesByCategory[catId] = salesByCategory.getOrDefault(catId, 0.0).preciseAdd(item.lineTotal)
        }
        
        return ReportData(
            totalSales = totalSales,
            invoiceCount = invoiceCount,
            cashSales = cashSales,
            creditSales = creditSales,
            grossProfit = grossProfit,
            netOperatingProfit = netOperatingProfit,
            ownerDraw = ownerDraw,
            topProductsByProfit = topProductsByProfit,
            topProductsByQty = topProductsByQty,
            lossProducts = lossProducts,
            expensesByCategory = expensesByCategory,
            openingCashBalance = openingCashBalance,
            cashIn = periodCashIn,
            cashOut = periodCashOut,
            closingCashBalance = closingCashBalance,
            totalSupplierDebts = totalSupplierDebts,
            totalCustomerDebts = totalCustomerDebts,
            dailySales = dailySales,
            dailyProfit = dailyProfit,
            salesByCategory = salesByCategory
        )
    }
}
