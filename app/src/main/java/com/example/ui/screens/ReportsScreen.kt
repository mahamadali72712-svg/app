package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.data.local.ProductCategory
import com.example.data.repository.ProductStat
import com.example.data.repository.ReportData
import com.example.ui.viewmodels.StoreViewModel
import com.example.utils.exportReportToExcel
import com.example.utils.formatCurrency
import com.example.utils.formatQty
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Modern Luxury Palette
private val CardBg = Color(0x1AFFFFFF)
private val CardBorder = Color(0x26FFFFFF)
private val TextWhite = Color(0xFFFFFFFF)
private val TextSub = Color(0xFFCBD5E1)
private val TextMuted = Color(0xFF94A3B8)

private val AccentPurple = Color(0xFFA855F7)
private val AccentBlue = Color(0xFF3B82F6)
private val AccentGreen = Color(0xFF10B981)
private val AccentAmber = Color(0xFFF59E0B)
private val AccentRose = Color(0xFFF43F5E)
private val AccentCyan = Color(0xFF06B6D4)

@Composable
fun ReportsScreen(viewModel: StoreViewModel, navController: NavController) {
    val reportData by viewModel.reportData.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val categoriesMap = remember(categories) { categories.associateBy { it.id } }

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
                cal.set(Calendar.SECOND, 0)
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

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
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

    val bgGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF1E1B4B),
            Color(0xFF2E1065),
            Color(0xFF0F172A)
        )
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Header Bar
                ReportsHeader(onBack = { navController.popBackStack() })

                // Filter Pill Selector Bar
                ReportsFilterPillBar(
                    selectedFilter = selectedFilter,
                    onFilterSelected = { filter ->
                        selectedFilter = filter
                        showCustomDate = filter == "مخصص"
                    }
                )

                if (showCustomDate) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardBg)
                                .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                                .clickable { showDatePicker(context, customStartDate) { customStartDate = it } },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "من: ${formatDate(customStartDate)}",
                                color = TextWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Main Content Area
                reportData?.let { data ->
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Executive Summary Financial Matrix Card
                        item {
                            ExecutiveSummaryCard(data)
                        }

                        // 2. Cash Flow & Cashbox Breakdown Card
                        item {
                            CashFlowCard(data)
                        }

                        // 3. Debts & Receivables Card
                        item {
                            DebtsSummaryCard(data)
                        }

                        // 4. Sales by Category Breakdown (Numbers & Details)
                        item {
                            SalesByCategoryCard(data, categoriesMap)
                        }

                        // 5. Top Selling Products Details
                        item {
                            TopProductsCard(data)
                        }

                        // 6. Losing or Low Margin Products (if any)
                        if (data.lossProducts.isNotEmpty()) {
                            item {
                                LossProductsCard(data)
                            }
                        }

                        // 7. Expenses Breakdown Details
                        item {
                            ExpensesBreakdownCard(data, categoriesMap)
                        }

                        // Export Button
                        item {
                            Button(
                                onClick = {
                                    exportLauncher.launch("Store_Report_${System.currentTimeMillis()}.xlsx")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color(0xFF7C3AED), Color(0xFFEC4899), Color(0xFFF59E0B))
                                            ),
                                            shape = RoundedCornerShape(16.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.FileDownload,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Text(
                                            "تصدير التقرير الكامل إلى Excel",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                } ?: Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentPurple)
                }
            }
        }
    }
}

@Composable
fun ReportsHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "تقارير المتجر المالية والتشغيلية",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "بيانات وأرقام تفصيلية دقيقة لحركة النشاط",
                color = TextMuted,
                fontSize = 11.sp
            )
        }

        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0x28FFFFFF))
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "رجوع",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ReportsFilterPillBar(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    val filters = listOf("اليوم", "الأسبوع", "الشهر", "السنة", "مخصص")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0x20FFFFFF))
            .padding(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            filters.forEach { filter ->
                val isSelected = selectedFilter == filter
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(50))
                        .then(
                            if (isSelected) {
                                Modifier.background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFA855F7), Color(0xFF8B5CF6))
                                    )
                                )
                            } else {
                                Modifier
                            }
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onFilterSelected(filter) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filter,
                        color = if (isSelected) Color.White else TextSub,
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun ExecutiveSummaryCard(data: ReportData) {
    val opExpenses = (data.grossProfit - data.netOperatingProfit).coerceAtLeast(0.0)

    CardSectionContainer(
        title = "الملخص المالي والتشغيلي",
        subtitle = "تحليل شامل للإيرادات والربحية وأعداد الفواتير",
        icon = Icons.Outlined.AccountBalance,
        badgeColor = AccentBlue
    ) {
        // Top 2 Primary KPI Tiles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Total Sales Tile
            KPITile(
                modifier = Modifier.weight(1f),
                title = "إجمالي المبيعات",
                value = data.totalSales.formatCurrency(),
                subText = "${data.invoiceCount} فاتورة صادرة",
                color = AccentBlue,
                icon = Icons.Outlined.ShoppingCart
            )

            // Net Profit Tile
            KPITile(
                modifier = Modifier.weight(1f),
                title = "صافي الربح التشغيلي",
                value = data.netOperatingProfit.formatCurrency(),
                subText = "بعد خصم المصروفات",
                color = if (data.netOperatingProfit >= 0) AccentGreen else AccentRose,
                icon = Icons.Outlined.TrendingUp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Breakdown Table Grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x0FFFFFFF))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricDetailRow(
                label = "المبيعات النقدية (الكاش)",
                value = data.cashSales.formatCurrency(),
                valueColor = AccentGreen
            )
            HorizontalDivider(color = Color(0x1AFFFFFF))
            MetricDetailRow(
                label = "المبيعات الآجلة (الديون)",
                value = data.creditSales.formatCurrency(),
                valueColor = AccentAmber
            )
            HorizontalDivider(color = Color(0x1AFFFFFF))
            MetricDetailRow(
                label = "مجمل الربح (قبل المصروفات)",
                value = data.grossProfit.formatCurrency(),
                valueColor = TextWhite
            )
            HorizontalDivider(color = Color(0x1AFFFFFF))
            MetricDetailRow(
                label = "المصروفات التشغيلية",
                value = "- ${opExpenses.formatCurrency()}",
                valueColor = AccentRose
            )
            if (data.ownerDraw > 0) {
                HorizontalDivider(color = Color(0x1AFFFFFF))
                MetricDetailRow(
                    label = "مسحوبات المالك / الشركاء",
                    value = "- ${data.ownerDraw.formatCurrency()}",
                    valueColor = AccentAmber
                )
            }
        }
    }
}

@Composable
fun CashFlowCard(data: ReportData) {
    CardSectionContainer(
        title = "حركة الخزينة والنقدية",
        subtitle = "تدفقات المقبوضات والمدفوعات والرصيد الحالي",
        icon = Icons.Outlined.AccountBalanceWallet,
        badgeColor = AccentPurple
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x0FFFFFFF))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricDetailRow(
                label = "رصيد بداية الفترة (الافتتاحي)",
                value = data.openingCashBalance.formatCurrency(),
                valueColor = TextWhite
            )
            HorizontalDivider(color = Color(0x1AFFFFFF))
            MetricDetailRow(
                label = "إجمالي المقبوضات (داخل الخزينة)",
                value = "+ ${data.cashIn.formatCurrency()}",
                valueColor = AccentGreen
            )
            HorizontalDivider(color = Color(0x1AFFFFFF))
            MetricDetailRow(
                label = "إجمالي المدفوعات (خارج الخزينة)",
                value = "- ${data.cashOut.formatCurrency()}",
                valueColor = AccentRose
            )
            HorizontalDivider(color = Color(0x1AFFFFFF))
            MetricDetailRow(
                label = "الرصيد الختامي الحالي بالخزينة",
                value = data.closingCashBalance.formatCurrency(),
                valueColor = AccentPurple,
                isBold = true
            )
        }
    }
}

@Composable
fun DebtsSummaryCard(data: ReportData) {
    val netDebt = data.totalCustomerDebts - data.totalSupplierDebts

    CardSectionContainer(
        title = "موقف الديون والتزامات السوق",
        subtitle = "إجمالي مستحقات العملاء والتزامات الموردين",
        icon = Icons.Outlined.ReceiptLong,
        badgeColor = AccentAmber
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KPITile(
                modifier = Modifier.weight(1f),
                title = "ديون العملاء (لنا)",
                value = data.totalCustomerDebts.formatCurrency(),
                subText = "مستحقات خارجية",
                color = AccentAmber,
                icon = Icons.Outlined.Person
            )

            KPITile(
                modifier = Modifier.weight(1f),
                title = "ديون الموردين (علينا)",
                value = data.totalSupplierDebts.formatCurrency(),
                subText = "التزامات مستحقة",
                color = AccentRose,
                icon = Icons.Outlined.LocalShipping
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x14FFFFFF))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("صافي الفارق المطلوب تحصيله / سداده:", fontSize = 12.5.sp, color = TextSub)
                Text(
                    text = netDebt.formatCurrency(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (netDebt >= 0) AccentGreen else AccentRose
                )
            }
        }
    }
}

@Composable
fun SalesByCategoryCard(data: ReportData, categoriesMap: Map<String, ProductCategory>) {
    val totalCategorySales = data.salesByCategory.values.sum().coerceAtLeast(1.0)

    CardSectionContainer(
        title = "توزيع المبيعات حسب التصنيف",
        subtitle = "تفصيل الأداء المالي لكل قسم من أقسام المنتجات",
        icon = Icons.Outlined.Category,
        badgeColor = AccentCyan
    ) {
        if (data.salesByCategory.isEmpty()) {
            Text("لا توجد مبيعات مسجلة في هذه الفترة", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(6.dp))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                data.salesByCategory.forEach { (catId, amount) ->
                    val catName = categoriesMap[catId]?.name ?: "عام / غير محدد"
                    val percentage = (amount / totalCategorySales) * 100

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x0FFFFFFF))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(catName, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${percentage.toInt()}%", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(amount.formatCurrency(), color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Minimal percentage bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(CircleShape)
                                .background(Color(0x1AFFFFFF))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction = (percentage / 100.0).coerceIn(0.0, 1.0).toFloat())
                                    .clip(CircleShape)
                                    .background(AccentCyan)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopProductsCard(data: ReportData) {
    CardSectionContainer(
        title = "أكثر المنتجات مبيعاً وربحية",
        subtitle = "الأصناف الأكثر طلباً مع كمياتها وإجمالي مبيعاتها وأرباحها",
        icon = Icons.Outlined.Star,
        badgeColor = AccentGreen
    ) {
        if (data.topProductsByQty.isEmpty()) {
            Text("لا توجد مبيعات في هذه الفترة", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(6.dp))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                data.topProductsByQty.forEachIndexed { index, p ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x0FFFFFFF)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(AccentGreen.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${index + 1}", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }

                                Column {
                                    Text(p.productName, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("الكمية المباعة: ${p.qty.formatQty()} قطعة", color = TextSub, fontSize = 11.sp)
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(p.totalSales.formatCurrency(), color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("الربح: ${p.profit.formatCurrency()}", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LossProductsCard(data: ReportData) {
    CardSectionContainer(
        title = "المنتجات الخاسرة",
        subtitle = "الأصناف التي تباع بسعر أقل من التكلفة",
        icon = Icons.Outlined.Warning,
        badgeColor = AccentRose
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            data.lossProducts.forEach { p ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x1EF43F5E)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(p.productName, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("الكمية: ${p.qty.formatQty()}", color = TextSub, fontSize = 11.sp)
                        }
                        Text(p.profit.formatCurrency(), color = AccentRose, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ExpensesBreakdownCard(data: ReportData, categoriesMap: Map<String, ProductCategory>) {
    val totalExpenses = data.expensesByCategory.values.sum().coerceAtLeast(1.0)

    CardSectionContainer(
        title = "تفصيل المصروفات التشغيلية",
        subtitle = "توزيع المبالغ المنفقة على بند المصاريف",
        icon = Icons.Outlined.Payments,
        badgeColor = AccentRose
    ) {
        if (data.expensesByCategory.isEmpty()) {
            Text("لا توجد مصروفات مسجلة في هذه الفترة", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(6.dp))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                data.expensesByCategory.forEach { (catId, amount) ->
                    val catName = categoriesMap[catId]?.name ?: "مصاريف تشغيلية عامة"
                    val percentage = (amount / totalExpenses) * 100

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x0FFFFFFF))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(AccentRose)
                            )
                            Column {
                                Text(catName, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("النسبة: ${percentage.toInt()}%", color = TextMuted, fontSize = 11.sp)
                            }
                        }

                        Text("- ${amount.formatCurrency()}", color = AccentRose, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// Reuseable Components
@Composable
fun CardSectionContainer(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(badgeColor, badgeColor.copy(alpha = 0.75f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(text = title, color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(text = subtitle, color = TextSub, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun KPITile(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subText: String,
    color: Color,
    icon: ImageVector
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x12FFFFFF))
            .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(16.dp))
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = TextSub, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Text(value, color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Text(subText, color = TextMuted, fontSize = 10.sp)
        }
    }
}

@Composable
fun MetricDetailRow(
    label: String,
    value: String,
    valueColor: Color,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.5.sp,
            color = if (isBold) TextWhite else TextSub,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            fontSize = 13.5.sp,
            color = valueColor,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.Bold
        )
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
