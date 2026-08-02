package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.PurchaseInvoice
import com.example.data.local.PurchaseInvoiceItem
import com.example.ui.viewmodels.StoreViewModel
import com.example.utils.formatCurrency
import com.example.utils.formatQty
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle

private val glowingShadow = Shadow(color = Color(0x442E8B57), blurRadius = 8f)
private val glowingTextStyle = TextStyle(shadow = glowingShadow, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)

private val ThemeGreen = Color(0xFF2E8B57)
private val ThemeRed = Color(0xFFD32F2F)
private val SoftGreen = Color(0xFFE9F5EC)
private val SoftRedBg = Color(0xFFFFEBEE)
private val WhitePure = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF111111)
private val TextSecondary = Color(0xFF757575)

private val HistoryBgGradient = Brush.verticalGradient(
    0.0f to SoftGreen,
    0.5f to SoftRedBg,
    1.0f to WhitePure
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasesHistoryScreen(viewModel: StoreViewModel, navController: androidx.navigation.NavController) {
    val invoices by viewModel.allPurchaseInvoices.collectAsStateWithLifecycle()
    val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var selectedInvoice by remember { mutableStateOf<PurchaseInvoice?>(null) }
    var invoiceItems by remember { mutableStateOf<List<PurchaseInvoiceItem>>(emptyList()) }
    
    var searchQuery by remember { mutableStateOf("") }
    var dateFilter by remember { mutableStateOf("الكل") }
    
    val now = System.currentTimeMillis()
    val dayMs = 24 * 60 * 60 * 1000L
    
    val filteredInvoices = invoices.filter { invoice ->
        val supplierName = suppliers.find { it.id == invoice.supplierId }?.name ?: ""
        val matchesSearch = invoice.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
            supplierName.contains(searchQuery, ignoreCase = true)
            
        val matchesDate = when (dateFilter) {
            "اليوم" -> now - invoice.invoiceDate <= dayMs
            "هذا الأسبوع" -> now - invoice.invoiceDate <= 7 * dayMs
            "هذا الشهر" -> now - invoice.invoiceDate <= 30 * dayMs
            else -> true
        }
        
        matchesSearch && matchesDate
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            LuxuryHistoryTopBar(onBack = { navController.popBackStack() })
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(HistoryBgGradient)
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Search & Filter
                Box(modifier = Modifier.padding(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("بحث برقم الفاتورة أو المورد", color = TextSecondary) },
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = ThemeGreen) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(12.dp)),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = WhitePure,
                                unfocusedContainerColor = WhitePure,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        val filterOptions = listOf("الكل", "اليوم", "هذا الأسبوع", "هذا الشهر")
                        var expandedFilter by remember { mutableStateOf(false) }
                        
                        ExposedDropdownMenuBox(
                            expanded = expandedFilter,
                            onExpandedChange = { expandedFilter = it }
                        ) {
                            TextField(
                                value = dateFilter,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("تصفية بالتاريخ", color = TextSecondary, fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Outlined.FilterList, contentDescription = null, tint = ThemeGreen) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFilter) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .shadow(2.dp, RoundedCornerShape(12.dp)),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = WhitePure,
                                    unfocusedContainerColor = WhitePure,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = expandedFilter,
                                onDismissRequest = { expandedFilter = false },
                                modifier = Modifier.background(WhitePure)
                            ) {
                                filterOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option, fontWeight = FontWeight.ExtraBold, color = TextPrimary, style = glowingTextStyle) },
                                        onClick = {
                                            dateFilter = option
                                            expandedFilter = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // List of Invoices
                if (filteredInvoices.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("لا توجد فواتير مشتريات تطابق البحث", color = TextSecondary)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredInvoices.sortedByDescending { it.invoiceDate }) { invoice ->
                            val supplierName = suppliers.find { it.id == invoice.supplierId }?.name ?: "مورد غير معروف"
                            LuxuryInvoiceHistoryCard(
                                invoice = invoice,
                                supplierName = supplierName,
                                onClick = {
                                    scope.launch {
                                        invoiceItems = viewModel.getPurchaseInvoiceItems(invoice.id)
                                        selectedInvoice = invoice
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Details Dialog
    if (selectedInvoice != null) {
        val supplierName = suppliers.find { it.id == selectedInvoice!!.supplierId }?.name ?: "مورد غير معروف"
        LuxuryInvoiceDetailsDialog(
            invoice = selectedInvoice!!,
            supplierName = supplierName,
            items = invoiceItems,
            onDismiss = { selectedInvoice = null }
        )
    }
}

@Composable
fun LuxuryHistoryTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(42.dp)) // Spacer to center title
        Spacer(modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("سجل المشتريات", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary, style = glowingTextStyle)
            Text("تصفح الفواتير السابقة", fontSize = 12.sp, color = TextSecondary)
        }
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(WhitePure)
                .shadow(2.dp, CircleShape)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.ArrowForward, contentDescription = "رجوع", tint = TextPrimary)
        }
    }
}

@Composable
fun LuxuryInvoiceHistoryCard(
    invoice: PurchaseInvoice,
    supplierName: String,
    onClick: () -> Unit
) {
    val formatter = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
    val dateString = formatter.format(Date(invoice.invoiceDate))
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = ThemeGreen.copy(alpha = 0.3f))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WhitePure)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text("فاتورة #${invoice.invoiceNumber}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = TextPrimary, style = glowingTextStyle)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(supplierName, fontSize = 14.sp, color = ThemeGreen, fontWeight = FontWeight.ExtraBold, style = glowingTextStyle.copy(color = ThemeGreen))
                }
                Text(dateString, fontSize = 12.sp, color = TextSecondary)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("الإجمالي", fontSize = 12.sp, color = TextSecondary)
                    Text(invoice.totalAmount.formatCurrency(), fontWeight = FontWeight.Black, fontSize = 16.sp, color = TextPrimary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("المدفوع", fontSize = 12.sp, color = TextSecondary)
                    Text(invoice.paidAmount.formatCurrency(), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF4CAF50))
                }
            }
        }
    }
}

@Composable
fun LuxuryInvoiceDetailsDialog(
    invoice: PurchaseInvoice,
    supplierName: String,
    items: List<PurchaseInvoiceItem>,
    onDismiss: () -> Unit
) {
    val formatter = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WhitePure,
        titleContentColor = ThemeGreen,
        textContentColor = TextPrimary,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("تفاصيل فاتورة المشتريات", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("#${invoice.invoiceNumber}", fontSize = 14.sp, color = TextSecondary)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header Info
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("المورد:", color = TextSecondary, fontSize = 14.sp)
                    Text(supplierName, fontWeight = FontWeight.ExtraBold, color = TextPrimary, fontSize = 14.sp, style = glowingTextStyle)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("التاريخ:", color = TextSecondary, fontSize = 14.sp)
                    Text(formatter.format(Date(invoice.invoiceDate)), fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(16.dp))
                
                // Items
                Text("الأصناف", fontWeight = FontWeight.ExtraBold, color = ThemeGreen, fontSize = 16.sp, style = glowingTextStyle.copy(color = ThemeGreen))
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    modifier = Modifier.heightIn(max = 250.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SoftGreen, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("المنتج ID: ${item.productId.take(5)}...", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                Text("${item.quantity.formatQty()} × ${item.unitCost.formatCurrency()}", fontSize = 12.sp, color = TextSecondary)
                            }
                            Text(item.lineTotal.formatCurrency(), fontWeight = FontWeight.Bold, color = ThemeGreen, fontSize = 14.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(16.dp))
                
                // Summary
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("الإجمالي:", fontWeight = FontWeight.ExtraBold, color = TextPrimary, style = glowingTextStyle)
                    Text(invoice.totalAmount.formatCurrency(), fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("المدفوع:", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                    Text(invoice.paidAmount.formatCurrency(), fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val remaining = invoice.totalAmount - invoice.paidAmount
                    Text("المتبقي:", fontWeight = FontWeight.ExtraBold, color = ThemeRed, style = glowingTextStyle.copy(color = ThemeRed))
                    Text(remaining.formatCurrency(), fontWeight = FontWeight.Bold, color = ThemeRed)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ThemeGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("إغلاق", fontWeight = FontWeight.ExtraBold, color = WhitePure, style = glowingTextStyle)
            }
        }
    )
}
