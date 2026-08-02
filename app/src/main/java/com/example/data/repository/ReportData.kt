package com.example.data.repository

data class ReportData(
    val totalSales: Double = 0.0,
    val invoiceCount: Int = 0,
    val cashSales: Double = 0.0,
    val creditSales: Double = 0.0,
    val grossProfit: Double = 0.0,
    val netOperatingProfit: Double = 0.0,
    val ownerDraw: Double = 0.0,
    
    val topProductsByProfit: List<ProductStat> = emptyList(),
    val topProductsByQty: List<ProductStat> = emptyList(),
    val lossProducts: List<ProductStat> = emptyList(),
    
    val expensesByCategory: Map<String, Double> = emptyMap(),
    
    val openingCashBalance: Double = 0.0,
    val cashIn: Double = 0.0,
    val cashOut: Double = 0.0,
    val closingCashBalance: Double = 0.0,
    
    val totalSupplierDebts: Double = 0.0,
    val totalCustomerDebts: Double = 0.0,
    
    val dailySales: Map<String, Double> = emptyMap(),
    val dailyProfit: Map<String, Double> = emptyMap(),
    val salesByCategory: Map<String, Double> = emptyMap()
)

data class ProductStat(
    val productId: String,
    val productName: String,
    val profit: Double,
    val qty: Double,
    val totalSales: Double
)
