package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.example.data.local.SalesInvoice
import com.example.data.local.SalesInvoiceItem
import com.example.ui.viewmodels.StoreViewModel
import com.example.utils.formatCurrency
import com.example.utils.formatQty
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Design System Colors matching Sales module
private val EmeraldPrimary = Color(0xFF059669)
private val EmeraldLight = Color(0xFF10B981)
private val EmeraldDark = Color(0xFF047857)
private val EmeraldBg = Color(0xFFECFDF5)

private val MaroonPrimary = Color(0xFF881337)
private val MaroonDark = Color(0xFF4C0519)
private val MaroonLight = Color(0xFF9F1239)
private val MaroonBg = Color(0xFFFFF1F2)

private val LightLavender = Color(0xFFF3E8FF)
private val LightLavenderSurface = Color(0xFFFAF5FF)
private val LightLavenderBorder = Color(0xFFE9D5FF)

private val AmberAccent = Color(0xFFD97706)
private val AmberBg = Color(0xFFFFFBEB)
private val PurpleAccent = Color(0xFF7C3AED)
private val PurpleBg = Color(0xFFF5F3FF)

private val PureWhite = Color(0xFFFFFFFF)
private val DarkText = Color(0xFF1E293B)
private val MutedText = Color(0xFF64748B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesHistoryScreen(viewModel: StoreViewModel, navController: androidx.navigation.NavController) {
    val invoices by viewModel.allSalesInvoices.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("الكل") }

    var selectedInvoiceForDetails by remember { mutableStateOf<SalesInvoice?>(null) }
    var selectedInvoiceItems by remember { mutableStateOf<List<SalesInvoiceItem>>(emptyList()) }
    var isLoadingDetails by remember { mutableStateOf(false) }

    var returnInvoice by remember { mutableStateOf<SalesInvoice?>(null) }
    var returnItemsList by remember { mutableStateOf<List<SalesInvoiceItem>>(emptyList()) }
    val scope = rememberCoroutineScope()

    val now = System.currentTimeMillis()
    val dayMs = 24 * 60 * 60 * 1000L

    val filteredInvoices = remember(invoices, searchQuery, selectedFilter) {
        invoices.filter { inv ->
            val matchesSearch = inv.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                    (inv.customerName ?: "").contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "اليوم" -> now - inv.invoiceDate <= dayMs
                "هذا الأسبوع" -> now - inv.invoiceDate <= 7 * dayMs
                "هذا الشهر" -> now - inv.invoiceDate <= 30 * dayMs
                "آجل" -> inv.paymentType == "CREDIT" || inv.paymentType == "PARTIAL" || inv.remainingAmount > 0
                "مرتجع" -> inv.status == "RETURN"
                else -> true
            }

            matchesSearch && matchesFilter
        }.sortedByDescending { it.invoiceDate }
    }

    // Dynamic Summary metrics for filtered invoices
    val totalRevenue = remember(filteredInvoices) {
        filteredInvoices.filter { it.status != "RETURN" }.sumOf { it.totalAmount }
    }
    val totalNetProfit = remember(filteredInvoices) {
        filteredInvoices.sumOf { it.totalProfit }
    }
    val totalReturnsCount = remember(filteredInvoices) {
        filteredInvoices.count { it.status == "RETURN" }
    }
    val totalRemainingDebt = remember(filteredInvoices) {
        filteredInvoices.sumOf { it.remainingAmount }
    }

    val mainBackgroundGradient = Brush.verticalGradient(
        colors = listOf(
            LightLavenderSurface,
            Color(0xFFF8FAFC),
            LightLavender.copy(alpha = 0.4f)
        )
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                SalesHistoryTopHeader(
                    totalInvoicesCount = invoices.size,
                    onBack = { navController.popBackStack() }
                )
            },
            containerColor = LightLavenderSurface
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(mainBackgroundGradient)
                    .padding(padding)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Overview Summary Carousel Cards
                    item {
                        SalesHistorySummaryCards(
                            totalRevenue = totalRevenue,
                            totalNetProfit = totalNetProfit,
                            returnsCount = totalReturnsCount,
                            remainingDebt = totalRemainingDebt,
                            filteredCount = filteredInvoices.size
                        )
                    }

                    // 2. Search & Filter Bar
                    item {
                        SalesHistorySearchFilterSection(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            selectedFilter = selectedFilter,
                            onFilterSelected = { selectedFilter = it }
                        )
                    }

                    // 3. Invoices Section Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "قائمة الفواتير الموثقة (${filteredInvoices.size})",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkText
                            )
                            if (searchQuery.isNotEmpty() || selectedFilter != "الكل") {
                                TextButton(
                                    onClick = {
                                        searchQuery = ""
                                        selectedFilter = "الكل"
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("إعادة ضبط الفلاتر", fontSize = 12.sp, color = MaroonPrimary)
                                }
                            }
                        }
                    }

                    // 4. Invoice List or Empty Placeholder
                    if (filteredInvoices.isEmpty()) {
                        item {
                            EmptySalesHistoryPlaceholder(
                                isFiltered = searchQuery.isNotEmpty() || selectedFilter != "الكل",
                                onResetFilter = {
                                    searchQuery = ""
                                    selectedFilter = "الكل"
                                }
                            )
                        }
                    } else {
                        items(filteredInvoices, key = { it.id }) { invoice ->
                            SalesInvoiceCardItem(
                                invoice = invoice,
                                onViewDetails = {
                                    selectedInvoiceForDetails = invoice
                                    isLoadingDetails = true
                                },
                                onProcessReturn = {
                                    scope.launch {
                                        val items = viewModel.getSalesInvoiceItems(invoice.id)
                                        returnInvoice = invoice
                                        returnItemsList = items
                                    }
                                }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }

        // Invoice Detail Bottom Sheet / Dialog
        if (selectedInvoiceForDetails != null) {
            val invoice = selectedInvoiceForDetails!!
            LaunchedEffect(invoice.id) {
                isLoadingDetails = true
                selectedInvoiceItems = viewModel.getSalesInvoiceItems(invoice.id)
                isLoadingDetails = false
            }

            InvoiceDetailsBottomSheet(
                invoice = invoice,
                items = selectedInvoiceItems,
                isLoading = isLoadingDetails,
                viewModel = viewModel,
                onDismiss = { selectedInvoiceForDetails = null },
                onTriggerReturn = {
                    returnInvoice = invoice
                    returnItemsList = selectedInvoiceItems
                    selectedInvoiceForDetails = null
                }
            )
        }

        // Return Process Dialog / Sheet
        if (returnInvoice != null) {
            ReturnProcessDialog(
                invoice = returnInvoice!!,
                items = returnItemsList,
                viewModel = viewModel,
                onDismiss = { returnInvoice = null },
                onSuccess = {
                    returnInvoice = null
                }
            )
        }
    }
}

@Composable
fun SalesHistoryTopHeader(
    totalInvoicesCount: Int,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(MaroonDark, MaroonPrimary, MaroonLight)
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x2BFFFFFF))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "رجوع",
                        tint = PureWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = "سجل فواتير المبيعات",
                        color = PureWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "إرشيف الفواتير وتفاصيل المبيعات والترجيع",
                        color = LightLavender,
                        fontSize = 11.sp
                    )
                }
            }

            Surface(
                color = Color(0x33FFFFFF),
                shape = RoundedCornerShape(50),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FFFFFF))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                        contentDescription = null,
                        tint = EmeraldLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "$totalInvoicesCount فاتورة",
                        color = PureWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SalesHistorySummaryCards(
    totalRevenue: Double,
    totalNetProfit: Double,
    returnsCount: Int,
    remainingDebt: Double,
    filteredCount: Int
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        item {
            SummaryStatCard(
                title = "إجمالي المبيعات",
                value = totalRevenue.formatCurrency(),
                subtitle = "$filteredCount فاتورة في العرض",
                icon = Icons.Outlined.Payments,
                backgroundBrush = Brush.horizontalGradient(listOf(EmeraldDark, EmeraldPrimary)),
                accentColor = PureWhite
            )
        }

        item {
            SummaryStatCard(
                title = "صافي الأرباح",
                value = totalNetProfit.formatCurrency(),
                subtitle = if (totalNetProfit >= 0) "ربح ممتاز" else "خسارة محتملة",
                icon = Icons.Outlined.TrendingUp,
                backgroundBrush = Brush.horizontalGradient(listOf(MaroonDark, MaroonPrimary)),
                accentColor = EmeraldLight
            )
        }

        item {
            SummaryStatCard(
                title = "الديون المتبقية",
                value = remainingDebt.formatCurrency(),
                subtitle = "ذمم الفواتير الآجلة",
                icon = Icons.Outlined.AccountBalanceWallet,
                backgroundBrush = Brush.horizontalGradient(listOf(Color(0xFF6B21A8), PurpleAccent)),
                accentColor = PureWhite
            )
        }

        item {
            SummaryStatCard(
                title = "فواتير مرتجعة",
                value = "$returnsCount فواتير",
                subtitle = "عمليات إرجاع موثقة",
                icon = Icons.Outlined.AssignmentReturn,
                backgroundBrush = Brush.horizontalGradient(listOf(Color(0xFF991B1B), Color(0xFFDC2626))),
                accentColor = PureWhite
            )
        }
    }
}

@Composable
fun SummaryStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundBrush: Brush,
    accentColor: Color
) {
    Card(
        modifier = Modifier
            .width(165.dp)
            .shadow(3.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = LightLavender,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Text(
                    text = value,
                    color = accentColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = subtitle,
                    color = PureWhite.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesHistorySearchFilterSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, LightLavenderBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("بحث برقم الفاتورة أو اسم العميل...", fontSize = 12.5.sp, color = MutedText) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "بحث",
                        tint = MaroonPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "مسح",
                                tint = MutedText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaroonPrimary,
                    unfocusedBorderColor = LightLavenderBorder,
                    focusedContainerColor = LightLavenderSurface,
                    unfocusedContainerColor = LightLavenderSurface
                )
            )

            // Filter Chips
            val filterOptions = listOf("الكل", "اليوم", "هذا الأسبوع", "هذا الشهر", "آجل", "مرتجع")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(filterOptions) { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { onFilterSelected(filter) },
                        label = {
                            Text(
                                text = filter,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) PureWhite else DarkText
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaroonPrimary,
                            containerColor = LightLavenderSurface
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = LightLavenderBorder,
                            selectedBorderColor = MaroonPrimary
                        ),
                        shape = RoundedCornerShape(50)
                    )
                }
            }
        }
    }
}

@Composable
fun SalesInvoiceCardItem(
    invoice: SalesInvoice,
    onViewDetails: () -> Unit,
    onProcessReturn: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd | hh:mm a", Locale("ar")) }
    val isReturn = invoice.status == "RETURN"

    val statusBadgeColor = when {
        isReturn -> MaroonPrimary
        invoice.paymentType == "CREDIT" -> AmberAccent
        invoice.paymentType == "PARTIAL" -> PurpleAccent
        else -> EmeraldPrimary
    }

    val statusText = when {
        isReturn -> "مرتجع"
        invoice.paymentType == "CREDIT" -> "بيع آجل"
        invoice.paymentType == "PARTIAL" -> "دفع جزئي"
        else -> "نقدي"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, LightLavenderBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Invoice Number + Date + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(LightLavender),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Receipt,
                            contentDescription = null,
                            tint = MaroonPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = invoice.invoiceNumber,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = DarkText
                        )
                        Text(
                            text = dateFormat.format(Date(invoice.invoiceDate)),
                            fontSize = 10.5.sp,
                            color = MutedText
                        )
                    }
                }

                Surface(
                    color = statusBadgeColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(50),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusBadgeColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = statusText,
                        color = statusBadgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = LightLavenderBorder.copy(alpha = 0.6f))

            // Customer Name & Main Figures
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = MutedText,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (invoice.customerName.isNullOrBlank()) "زبون عابر (نقدي)" else invoice.customerName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = invoice.totalAmount.formatCurrency(),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldDark
                    )
                    if (invoice.remainingAmount > 0) {
                        Text(
                            text = "متبقي: ${invoice.remainingAmount.formatCurrency()}",
                            fontSize = 10.5.sp,
                            color = MaroonPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Profit & Actions Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(LightLavenderSurface)
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "الربح الصافي:",
                            fontSize = 11.sp,
                            color = MutedText
                        )
                        Text(
                            text = invoice.totalProfit.formatCurrency(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (invoice.totalProfit >= 0) EmeraldDark else MaroonPrimary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Details Button
                        OutlinedButton(
                            onClick = onViewDetails,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaroonPrimary),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Visibility,
                                contentDescription = null,
                                tint = MaroonPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("التفاصيل", fontSize = 11.sp, color = MaroonPrimary, fontWeight = FontWeight.Bold)
                        }

                        // Return Button if not already returned
                        if (!isReturn) {
                            Button(
                                onClick = onProcessReturn,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.RotateLeft,
                                    contentDescription = null,
                                    tint = PureWhite,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إرجاع", fontSize = 11.sp, color = PureWhite, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptySalesHistoryPlaceholder(
    isFiltered: Boolean,
    onResetFilter: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, LightLavenderBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(LightLavender),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ReceiptLong,
                    contentDescription = null,
                    tint = MaroonPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isFiltered) "لا تتوفر فواتير مطابقة للبحث" else "سجل الفواتير فارغ حالياً",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = DarkText
            )

            Text(
                text = if (isFiltered)
                    "جرب تغيير كلمة البحث أو اختيار فلتر آخر لعرض نتائج إضافية"
                else
                    "ستظهر جميع فواتير المبيعات الصادرة هنا تلقائياً بعد إتمام العمليات",
                fontSize = 12.sp,
                color = MutedText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
            )

            if (isFiltered) {
                Button(
                    onClick = onResetFilter,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("عرض جميع الفواتير", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailsBottomSheet(
    invoice: SalesInvoice,
    items: List<SalesInvoiceItem>,
    isLoading: Boolean,
    viewModel: StoreViewModel,
    onDismiss: () -> Unit,
    onTriggerReturn: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("EEEE, dd MMMM yyyy - hh:mm a", Locale("ar")) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = PureWhite,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Receipt Header Accent
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(MaroonDark, MaroonPrimary)
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تفاصيل الفاتورة ${invoice.invoiceNumber}",
                            color = PureWhite,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Surface(
                            color = Color(0x33FFFFFF),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = if (invoice.status == "RETURN") "مرتجع" else "مؤكدة",
                                color = PureWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = "العميل: ${if (invoice.customerName.isNullOrBlank()) "زبون عابر (نقدي)" else invoice.customerName}",
                        color = LightLavender,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = dateFormat.format(Date(invoice.invoiceDate)),
                        color = PureWhite.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
            }

            Text(
                text = "المنتجات المباعة في الفاتورة",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = DarkText,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaroonPrimary, strokeWidth = 2.dp)
                }
            } else if (items.isEmpty()) {
                Text(
                    text = "لا تتوفر عناصر مسجلة لهذه الفاتورة",
                    fontSize = 12.sp,
                    color = MutedText,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEach { item ->
                        var productName by remember { mutableStateOf("جاري تحميل اسم المنتج...") }
                        LaunchedEffect(item.productId) {
                            val p = viewModel.getProductById(item.productId)
                            productName = p?.name ?: "منتج محذوف"
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = LightLavenderSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LightLavenderBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = productName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = DarkText
                                    )
                                    Text(
                                        text = "الكمية: ${item.quantity.formatQty()} × ${item.unitPrice.formatCurrency()}",
                                        fontSize = 11.sp,
                                        color = MutedText
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = item.lineTotal.formatCurrency(),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = EmeraldDark
                                    )
                                    Text(
                                        text = "الربح: ${item.lineProfit.formatCurrency()}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.lineProfit >= 0) EmeraldPrimary else MaroonPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Financial Summary Breakdown Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, LightLavenderBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "الملخص المالي للفاتورة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = DarkText
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("إجمالي المبيعات:", fontSize = 12.sp, color = MutedText)
                        Text(invoice.totalAmount.formatCurrency(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkText)
                    }

                    if (invoice.discount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الخصم الممنوح:", fontSize = 12.sp, color = MutedText)
                            Text("-${invoice.discount.formatCurrency()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaroonPrimary)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("المدفوع نقداً:", fontSize = 12.sp, color = MutedText)
                        Text(invoice.paidAmount.formatCurrency(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldDark)
                    }

                    if (invoice.remainingAmount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("المتبقي في الذمة:", fontSize = 12.sp, color = MutedText)
                            Text(invoice.remainingAmount.formatCurrency(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaroonPrimary)
                        }
                    }

                    HorizontalDivider(color = LightLavenderBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("صافي أرباح الفاتورة:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkText)
                        Surface(
                            color = if (invoice.totalProfit >= 0) EmeraldBg else MaroonBg,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = invoice.totalProfit.formatCurrency(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (invoice.totalProfit >= 0) EmeraldDark else MaroonPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (invoice.status != "RETURN") {
                    Button(
                        onClick = onTriggerReturn,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
                    ) {
                        Icon(Icons.Outlined.RotateLeft, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إرجاع منتجات", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LightLavenderBorder)
                ) {
                    Text("إغلاق", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkText)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReturnProcessDialog(
    invoice: SalesInvoice,
    items: List<SalesInvoiceItem>,
    viewModel: StoreViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var returnQuantities by remember { mutableStateOf(items.associate { it.id to 0.0 }) }
    var refundCash by remember { mutableStateOf(true) }
    val context = LocalContext.current
    var isProcessing by remember { mutableStateOf(false) }

    val totalReturnAmount = remember(returnQuantities) {
        items.sumOf { item ->
            val qty = returnQuantities[item.id] ?: 0.0
            qty * item.unitPrice
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaroonBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RotateLeft,
                        contentDescription = null,
                        tint = MaroonPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "إرجاع من الفاتورة ${invoice.invoiceNumber}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "حدد الكميات المراد إرجاعها للمخزون:",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MutedText
                )

                items.forEach { item ->
                    var productName by remember { mutableStateOf("جاري التحميل...") }
                    LaunchedEffect(item.productId) {
                        val p = viewModel.getProductById(item.productId)
                        productName = p?.name ?: "منتج محذوف"
                    }
                    val currentQty = returnQuantities[item.id] ?: 0.0

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = LightLavenderSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LightLavenderBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(productName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkText)
                            Text(
                                text = "السعر: ${item.unitPrice.formatCurrency()} | الكمية بالفاتورة: ${item.quantity.formatQty()}",
                                fontSize = 11.sp,
                                color = MutedText
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                if (currentQty < item.quantity) {
                                                    returnQuantities = returnQuantities.toMutableMap().apply {
                                                        put(item.id, currentQty + 1)
                                                    }
                                                }
                                            },
                                        color = EmeraldPrimary
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("+", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        }
                                    }

                                    OutlinedTextField(
                                        value = if (currentQty == 0.0) "" else currentQty.formatQty(),
                                        onValueChange = { input ->
                                            val v = input.toDoubleOrNull() ?: 0.0
                                            if (v <= item.quantity) {
                                                returnQuantities = returnQuantities.toMutableMap().apply {
                                                    put(item.id, v)
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .width(70.dp)
                                            .height(42.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaroonPrimary,
                                            unfocusedBorderColor = LightLavenderBorder
                                        )
                                    )

                                    Surface(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                if (currentQty > 0) {
                                                    returnQuantities = returnQuantities.toMutableMap().apply {
                                                        put(item.id, (currentQty - 1).coerceAtLeast(0.0))
                                                    }
                                                }
                                            },
                                        color = MaroonPrimary
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("-", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        }
                                    }
                                }

                                Text(
                                    text = (currentQty * item.unitPrice).formatCurrency(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = EmeraldDark
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = LightLavenderBorder)

                // Total Return Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(EmeraldBg)
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("إجمالي قيمة المرتجع:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldDark)
                        Text(
                            totalReturnAmount.formatCurrency(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldDark
                        )
                    }
                }

                if (invoice.customerId != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("طريقة رد المبلغ:", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = DarkText)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { refundCash = true }
                        ) {
                            RadioButton(selected = refundCash, onClick = { refundCash = true }, colors = RadioButtonDefaults.colors(selectedColor = EmeraldPrimary))
                            Text("خصم نقدي من الصندوق", fontSize = 12.sp, color = DarkText)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { refundCash = false }
                        ) {
                            RadioButton(selected = !refundCash, onClick = { refundCash = false }, colors = RadioButtonDefaults.colors(selectedColor = MaroonPrimary))
                            Text("خصم من ذمة/حساب العميل", fontSize = 12.sp, color = DarkText)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val returnItemsToProcess = items.filter { (returnQuantities[it.id] ?: 0.0) > 0 }.map {
                        val rq = returnQuantities[it.id] ?: 0.0
                        it.copy(
                            quantity = rq,
                            lineTotal = rq * it.unitPrice,
                            lineCost = rq * it.unitCost,
                            lineProfit = (rq * it.unitPrice) - (rq * it.unitCost)
                        )
                    }
                    if (returnItemsToProcess.isEmpty()) {
                        Toast.makeText(context, "يرجى تحديد كمية منتج واحد على الأقل للترجيع", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isProcessing = true
                    viewModel.processReturn(
                        originalInvoice = invoice,
                        returnItems = returnItemsToProcess,
                        refundCash = refundCash,
                        returnAmount = totalReturnAmount,
                        onSuccess = {
                            isProcessing = false
                            Toast.makeText(context, "تمت عملية الإرجاع وتعديل المخزون بنجاح!", Toast.LENGTH_SHORT).show()
                            onSuccess()
                        }
                    )
                },
                enabled = !isProcessing,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = PureWhite, strokeWidth = 2.dp)
                } else {
                    Text("تأكيد الإرجاع", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isProcessing) {
                Text("إلغاء", fontSize = 12.5.sp, color = MutedText)
            }
        },
        containerColor = PureWhite,
        shape = RoundedCornerShape(20.dp)
    )
}
