with open('app/src/main/java/com/example/ui/screens/ReportsScreen.kt', 'r') as f:
    content = f.read()

# Fix totalProfit -> netOperatingProfit
content = content.replace('data.totalProfit.formatCurrency()', 'data.netOperatingProfit.formatCurrency()')

# Fix salesTrend -> data.dailySales.entries.toList().map { it.value }
content = content.replace('data.salesTrend', 'data.dailySales.values.toList()')

# Fix topSellingProducts -> topProductsByQty
content = content.replace('data.topSellingProducts', 'data.topProductsByQty')

# Fix quantitySold -> qty
content = content.replace('p.quantitySold', 'p.qty')

with open('app/src/main/java/com/example/ui/screens/ReportsScreen.kt', 'w') as f:
    f.write(content)
