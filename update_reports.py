import os

new_content = """package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

val GlowPurple = Color(0xFF9D4EDD)
val GlowPink = Color(0xFFF15BB5)
val GlowBlue = Color(0xFF00F2FE)
val DarkBackground = Color(0xFF0A0514)
val CardBackground = Color(0xFF1B0C3B)

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
                title = { Text("التقارير المتقدمة", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
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
                        .background(Brush.linearGradient(listOf(GlowPurple, GlowPink)))
                        .clickable { exportLauncher.launch("Store_Report_${System.currentTimeMillis()}.xlsx") },
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
                .background(Brush.verticalGradient(listOf(DarkBackground, Color.Black)))
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
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) Brush.linearGradient(listOf(GlowPurple, GlowPink)) else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.05f))))
                                .border(1.dp, if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                                .clickable {
                                    selectedFilter = filter
                                    showCustomDate = filter == "مخصص"
                                }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filter,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                
                if (showCustomDate) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(45.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardBackground)
                                .border(1.dp, GlowBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable { showDatePicker(context, customStartDate) { customStartDate = it } }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("من: ${formatDate(customStartDate)}", color = Color.White, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(45.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardBackground)
                                .border(1.dp, GlowBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable { showDatePicker(context, customEndDate) { customEndDate = it } }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("إلى: ${formatDate(customEndDate)}", color = Color.White, fontSize = 14.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                val data = reportData
                if (data == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GlowPink)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp)
                    ) {
                        // A. Sales Report Card
                        item {
                            ReportSection(
                                title = "أداء المبيعات",
                                iconColor = GlowBlue,
                                data = listOf(
                                    "إجمالي المبيعات" to data.totalSales.formatCurrency(),
                                    "عدد الفواتير" to data.invoiceCount.toString(),
                                    "مبيعات نقدية" to data.cashSales.formatCurrency(),
                                    "مبيعات آجلة" to data.creditSales.formatCurrency(),
                                    "إجمالي الربح" to data.grossProfit.formatCurrency(),
                                    "صافي الربح التشغيلي" to data.netOperatingProfit.formatCurrency(),
                                    "مسحوبات شخصية" to data.ownerDraw.formatCurrency()
                                )
                            )
                        }
                        
                        // Charts
                        item {
                            NeonCard(title = "المبيعات اليومية (المبيعات)", borderColor = GlowBlue) {
                                SimpleBarChart(data.dailySales, modifier = Modifier.fillMaxWidth().height(150.dp))
                            }
                        }
                        
                        item {
                            NeonCard(title = "ترند الأرباح اليومية", borderColor = GlowPink) {
                                SimpleLineChart(data.dailyProfit, modifier = Modifier.fillMaxWidth().height(150.dp))
                            }
                        }
                        
                        // B. Best Selling Products
                        if (data.topProductsByQty.isNotEmpty()) {
                            item {
                                NeonCard(title = "أفضل المنتجات مبيعاً (بالكمية)", borderColor = GlowPurple) {
                                    data.topProductsByQty.forEachIndexed { index, p ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), 
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "${index + 1}. ${p.productName}", 
                                                color = Color.White, 
                                                modifier = Modifier.weight(1f),
                                                fontWeight = FontWeight.Medium
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(p.qty.formatQty(), fontWeight = FontWeight.Bold, color = GlowBlue, fontSize = 16.sp)
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Text(p.profit.formatCurrency(), color = Color(0xFF00FF87), modifier = Modifier.width(80.dp), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        if (index < data.topProductsByQty.size - 1) {
                                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                        }
                                    }
                                }
                            }
                        }
                        
                        // C. Loss Products
                        if (data.lossProducts.isNotEmpty()) {
                            item {
                                NeonCard(title = "منتجات بيعت بخسارة", borderColor = Color(0xFFFF3366), containerColor = Color(0xFF3B0C1B)) {
                                    data.lossProducts.forEachIndexed { index, p ->
                                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(p.productName, color = Color.White)
                                            Text(p.profit.formatCurrency(), fontWeight = FontWeight.Bold, color = Color(0xFFFF3366))
                                        }
                                        if (index < data.lossProducts.size - 1) {
                                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                        }
                                    }
                                }
                            }
                        }
                        
                        // D. Expenses Breakdown
                        item {
                            NeonCard(title = "توزيع المصروفات", borderColor = GlowPink) {
                                if (data.expensesByCategory.isNotEmpty()) {
                                    SimplePieChart(data.expensesByCategory, modifier = Modifier.fillMaxWidth())
                                } else {
                                    Text("لا توجد مصروفات", color = Color.White.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 16.dp))
                                }
                            }
                        }
                        
                        // D2. Sales by Category
                        item {
                            NeonCard(title = "المبيعات حسب الفئة", borderColor = GlowPurple) {
                                if (data.salesByCategory.isNotEmpty()) {
                                    SimplePieChart(data.salesByCategory, modifier = Modifier.fillMaxWidth())
                                } else {
                                    Text("لا توجد مبيعات", color = Color.White.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 16.dp))
                                }
                            }
                        }
                        
                        // E. Cashbox
                        item {
                            ReportSection(
                                title = "حركة الصندوق",
                                iconColor = Color(0xFF00FF87),
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
                            ReportSection(
                                title = "ملخص الديون",
                                iconColor = Color(0xFFFFD700),
                                data = listOf(
                                    "ديون الموردين (التزامات)" to data.totalSupplierDebts.formatCurrency(),
                                    "ديون العملاء (أصول)" to data.totalCustomerDebts.formatCurrency()
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NeonCard(
    title: String, 
    borderColor: Color, 
    containerColor: Color = CardBackground,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(containerColor)
            .border(1.dp, borderColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun ReportSection(title: String, iconColor: Color, data: List<Pair<String, String>>) {
    var expanded by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBackground)
            .border(1.dp, iconColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
            .clickable { expanded = !expanded }
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(6.dp)).background(iconColor))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconColor.copy(alpha = 0.1f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (expanded) "إخفاء التفاصيل" else "عرض التفاصيل",
                        fontSize = 12.sp,
                        color = iconColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(12.dp))
                data.forEach { (label, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 15.sp)
                        Text(value, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
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
                        Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                        Text(value, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f), fontSize = 15.sp)
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

with open("app/src/main/java/com/example/ui/screens/ReportsScreen.kt", "w") as f:
    f.write(new_content)

print("Done")
