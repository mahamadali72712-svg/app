import re

with open('app/src/main/java/com/example/ui/screens/ReportsScreen.kt', 'r') as f:
    content = f.read()

# I will completely replace the contents of ReportsScreen.kt with a new design.
new_content = """package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import android.app.DatePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodels.StoreViewModel
import com.example.utils.formatCurrency
import com.example.utils.formatQty
import com.example.ui.components.SimpleBarChart
import com.example.ui.components.SimpleLineChart
import com.example.ui.components.SimplePieChart
import com.example.utils.exportReportToExcel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Vibrant Colors for Branches
val ColorSales = Color(0xFF0077B6)
val ColorProfit = Color(0xFF2A9D8F)
val ColorTrend = Color(0xFFF77F00)
val ColorTopProd = Color(0xFF7209B7)
val ColorLossProd = Color(0xFFD62828)
val ColorExpenses = Color(0xFFD81159)
val ColorCategory = Color(0xFF00B4D8)
val ColorCashbox = Color(0xFF3F37C9)
val ColorDebts = Color(0xFFF5B700)

val MainBgGradient = Brush.verticalGradient(
    0.0f to Color(0xFFFFF3E0), // Light Orange
    0.5f to Color(0xFFF1F8E9), // Light Green
    1.0f to Color(0xFFF3E5F5)  // Light Purple
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: StoreViewModel, navController: androidx.navigation.NavController) {
    val reportData by viewModel.reportData.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedFilter by remember { mutableStateOf("اليوم") }
    var customStartDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var customEndDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showCustomDate by remember { mutableStateOf(false) }

    fun updateReport() {
        val cal = Calendar.getInstance()
        val end = cal.timeInMillis
        var start = end
        when (selectedFilter) {
            "اليوم" -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                start = cal.timeInMillis
            }
            "الأسبوع" -> {
                cal.add(Calendar.DAY_OF_YEAR, -7)
                start = cal.timeInMillis
            }
            "الشهر" -> {
                cal.add(Calendar.MONTH, -1)
                start = cal.timeInMillis
            }
            "السنة" -> {
                cal.add(Calendar.YEAR, -1)
                start = cal.timeInMillis
            }
            "مخصص" -> {
                start = customStartDate
            }
        }
        viewModel.fetchReport(start, end)
    }

    LaunchedEffect(selectedFilter, customStartDate, customEndDate) {
        updateReport()
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    reportData?.let {
                        exportReportToExcel(context, uri, it)
                        Toast.makeText(context, "تم تصدير التقرير بنجاح", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "فشل التصدير: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("التقارير", color = Color(0xFF1F2937), fontWeight = FontWeight.ExtraBold, fontSize = 22.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع", tint = Color(0xFF1F2937))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFFF9800), Color(0xFF4CAF50))))
                        .clickable { exportLauncher.launch("Store_Report_${System.currentTimeMillis()}.xlsx") }
                        .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Color(0x33000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("تصدير التقرير إلى Excel", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MainBgGradient)
        ) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                // Filter Bar
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf("اليوم", "الأسبوع", "الشهر", "السنة", "مخصص")
                    items(filters) { filter ->
                        val isSelected = selectedFilter == filter
                        val filterBg = if (isSelected) Color(0xFF1F2937) else Color.White
                        val filterText = if (isSelected) Color.White else Color(0xFF4B5563)
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(filterBg)
                                .clickable {
                                    selectedFilter = filter
                                    showCustomDate = filter == "مخصص"
                                }
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                                .shadow(if (isSelected) 4.dp else 1.dp, RoundedCornerShape(20.dp), spotColor = Color(0x22000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filter,
                                color = filterText,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                if (showCustomDate) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(45.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                                .clickable { showDatePicker(context, customStartDate) { customStartDate = it } }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("من: ${formatDate(customStartDate)}", color = Color(0xFF374151), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                reportData?.let { data ->
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // A. Overview
                        item {
                            ColoredSectionCard(title = "نظرة عامة", themeColor = ColorSales) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("المبيعات", color = Color(0xFF6B7280), fontSize = 14.sp)
                                        Text(data.totalSales.formatCurrency(), fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = ColorSales)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("الأرباح", color = Color(0xFF6B7280), fontSize = 14.sp)
                                        Text(data.totalProfit.formatCurrency(), fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = ColorProfit)
                                    }
                                }
                            }
                        }

                        // B. Sales Trend
                        item {
                            ColoredSectionCard(title = "حركة المبيعات والأرباح", themeColor = ColorTrend) {
                                if (data.salesTrend.isNotEmpty()) {
                                    SimpleLineChart(
                                        data = data.salesTrend,
                                        modifier = Modifier.fillMaxWidth().height(200.dp),
                                        lineColor = ColorTrend
                                    )
                                } else {
                                    Text("لا توجد بيانات للفترة المحددة", color = Color(0xFF9CA3AF), modifier = Modifier.padding(vertical = 16.dp))
                                }
                            }
                        }

                        // C. Top Products
                        item {
                            ColoredSectionCard(title = "أكثر المنتجات مبيعاً", themeColor = ColorTopProd) {
                                if (data.topSellingProducts.isEmpty()) {
                                    Text("لا توجد بيانات", color = Color(0xFF9CA3AF), modifier = Modifier.padding(vertical = 16.dp))
                                } else {
                                    Column {
                                        data.topSellingProducts.forEachIndexed { index, p ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier.size(24.dp).clip(RoundedCornerShape(12.dp)).background(ColorTopProd.copy(alpha = 0.1f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text("${index + 1}", color = ColorTopProd, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(p.productName, color = Color(0xFF374151), fontWeight = FontWeight.Bold)
                                                }
                                                Text("${p.quantitySold.formatQty()} قطعة", color = ColorTopProd, fontWeight = FontWeight.Bold)
                                            }
                                            if (index < data.topSellingProducts.size - 1) {
                                                HorizontalDivider(color = Color(0xFFF3F4F6))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // C2. Losing Products
                        item {
                            ColoredSectionCard(title = "المنتجات الخاسرة", themeColor = ColorLossProd) {
                                if (data.lossProducts.isEmpty()) {
                                    Text("لا توجد منتجات خاسرة الحمدلله", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 16.dp))
                                } else {
                                    Column {
                                        data.lossProducts.forEachIndexed { index, p ->
                                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(p.productName, color = Color(0xFF374151), fontWeight = FontWeight.Bold)
                                                Text(p.profit.formatCurrency(), fontWeight = FontWeight.Bold, color = ColorLossProd)
                                            }
                                            if (index < data.lossProducts.size - 1) {
                                                HorizontalDivider(color = Color(0xFFF3F4F6))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // D. Expenses Breakdown
                        item {
                            ColoredSectionCard(title = "توزيع المصروفات", themeColor = ColorExpenses) {
                                if (data.expensesByCategory.isNotEmpty()) {
                                    SimplePieChart(data.expensesByCategory, modifier = Modifier.fillMaxWidth())
                                } else {
                                    Text("لا توجد مصروفات", color = Color(0xFF9CA3AF), modifier = Modifier.padding(vertical = 16.dp))
                                }
                            }
                        }

                        // D2. Sales by Category
                        item {
                            ColoredSectionCard(title = "المبيعات حسب الفئة", themeColor = ColorCategory) {
                                if (data.salesByCategory.isNotEmpty()) {
                                    SimplePieChart(data.salesByCategory, modifier = Modifier.fillMaxWidth())
                                } else {
                                    Text("لا توجد مبيعات", color = Color(0xFF9CA3AF), modifier = Modifier.padding(vertical = 16.dp))
                                }
                            }
                        }

                        // E. Cashbox
                        item {
                            CollapsibleSection(
                                title = "حركة الصندوق",
                                themeColor = ColorCashbox,
                                data = listOf(
                                    "الرصيد الافتتاحي" to data.openingCashBalance.formatCurrency(),
                                    "إجمالي الداخل" to data.cashIn.formatCurrency(),
                                    "إجمالي الخارج" to data.cashOut.formatCurrency(),
                                    "الرصيد الختامي" to data.closingCashBalance.formatCurrency()
                                )
                            )
                        }

                        // F. Debts
                        item {
                            CollapsibleSection(
                                title = "ملخص الديون",
                                themeColor = ColorDebts,
                                data = listOf(
                                    "ديون الموردين (التزامات)" to data.totalSupplierDebts.formatCurrency(),
                                    "ديون العملاء (أصول)" to data.totalCustomerDebts.formatCurrency()
                                )
                            )
                        }
                        
                        item { Spacer(modifier = Modifier.height(60.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
fun ColoredSectionCard(
    title: String,
    themeColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = themeColor.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(6.dp)).background(themeColor))
                Spacer(modifier = Modifier.width(10.dp))
                Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = themeColor)
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun CollapsibleSection(title: String, themeColor: Color, data: List<Pair<String, String>>) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = themeColor.copy(alpha = 0.4f))
            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(6.dp)).background(themeColor))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = themeColor)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(themeColor.copy(alpha = 0.1f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (expanded) "إخفاء" else "التفاصيل",
                        fontSize = 12.sp,
                        color = themeColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFF3F4F6))
                Spacer(modifier = Modifier.height(12.dp))
                data.forEach { (label, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, color = Color(0xFF6B7280), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text(value, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1F2937), fontSize = 16.sp)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                if (data.isNotEmpty()) {
                    val (label, value) = data.first()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, color = Color(0xFF9CA3AF), fontSize = 14.sp)
                        Text(value, fontWeight = FontWeight.Bold, color = Color(0xFF4B5563), fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

private fun showDatePicker(context: Context, initialDate: Long, onDateSelected: (Long) -> Unit) {
    val cal = Calendar.getInstance().apply { timeInMillis = initialDate }
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            cal.set(year, month, dayOfMonth)
            onDateSelected(cal.timeInMillis)
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}

private fun formatDate(time: Long): String {
    return SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(time))
}
"""
with open('app/src/main/java/com/example/ui/screens/ReportsScreen.kt', 'w') as f:
    f.write(new_content)

