package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.Customer
import com.example.data.local.CustomerPayment
import com.example.data.local.Product
import com.example.data.local.SalesInvoice
import com.example.data.local.SalesInvoiceItem
import com.example.ui.viewmodels.StoreViewModel
import com.example.utils.formatCurrency
import com.example.utils.formatQty
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CustomerDetailScreen(
    customerId: String,
    viewModel: StoreViewModel,
    navController: NavController
) {
    var customer by remember { mutableStateOf<Customer?>(null) }
    var payments by remember { mutableStateOf<List<CustomerPayment>>(emptyList()) }
    var sales by remember { mutableStateOf<List<SalesInvoice>>(emptyList()) }

    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val productsMap = remember(allProducts) { allProducts.associateBy { it.id } }

    var salesInvoiceItemsMap by remember { mutableStateOf<Map<String, List<SalesInvoiceItem>>>(emptyMap()) }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: كشف حساب والمعاملات, 1: معلومات التواصل
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showPdfDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(customerId) {
        customer = viewModel.getCustomerById(customerId)
        payments = viewModel.getCustomerPayments(customerId)
        sales = viewModel.getCustomerSales(customerId)
    }

    LaunchedEffect(sales) {
        if (sales.isNotEmpty()) {
            val map = mutableMapOf<String, List<SalesInvoiceItem>>()
            sales.forEach { sale ->
                val items = viewModel.getSalesInvoiceItems(sale.id)
                map[sale.id] = items
            }
            salesInvoiceItemsMap = map
        }
    }

    val bgGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF231454),
            Color(0xFF271C67),
            Color(0xFF2B2570)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {
        // Ambient background glow
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(300.dp)
                .align(Alignment.TopStart)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x33A855F7), Color.Transparent),
                        radius = 650f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0x28FFFFFF))
                        .clickable { navController.popBackStack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronLeft,
                        contentDescription = "رجوع",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = customer?.name ?: "تفاصيل العميل",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF8B5CF6), Color(0xFF6366F1), Color(0xFF3B82F6))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            if (customer != null) {
                val cust = customer!!
                val balance = cust.balance

                // Header Financial Stats Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(22.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0x28FFFFFF))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "الرصيد المالي الحالي",
                            color = Color(0xFFA5ABC7),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        val balanceText: String
                        val balanceColor: Color

                        if (balance > 0) {
                            balanceText = "عليه دين: ${balance.formatCurrency()}"
                            balanceColor = Color(0xFFFCA5A5)
                        } else if (balance < 0) {
                            balanceText = "له مستحقات: ${(-balance).formatCurrency()}"
                            balanceColor = Color(0xFF86EFAC)
                        } else {
                            balanceText = "الرصيد متزن (0 ريال)"
                            balanceColor = Color.White
                        }

                        Text(
                            text = balanceText,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = balanceColor
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Collect payment button
                            Button(
                                onClick = { showPaymentDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4F46E5),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تحصيل دفعة", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            // Call button if phone exists
                            if (!cust.phone.isNullOrBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${cust.phone}"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(Color.White, Color.White)))
                                ) {
                                    Icon(Icons.Outlined.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("اتصال هاتفي", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // WhatsApp PDF Statement Button
                        Button(
                            onClick = { showPdfDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF16A34A),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("إرسال كشف حساب (PDF) عبر واتساب", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                // Details Tab Bar
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    divider = { HorizontalDivider(color = Color(0x1AFFFFFF)) }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "كشف حساب والمعاملات",
                                color = if (selectedTab == 0) Color.White else Color(0xFFA5ABC7),
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "بيانات الاتصال والمعلومات",
                                color = if (selectedTab == 1) Color.White else Color(0xFFA5ABC7),
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }

                if (selectedTab == 0) {
                    val allTransactions = remember(payments, sales) {
                        val list = mutableListOf<CustomerTransactionItemDetail>()
                        sales.forEach { list.add(CustomerTransactionItemDetail.Sale(it)) }
                        payments.forEach { list.add(CustomerTransactionItemDetail.Payment(it)) }
                        list.sortedByDescending { it.date }
                    }

                    if (allTransactions.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("لا توجد معاملات مسجلة لهذا العميل", color = Color(0xFFA5ABC7), fontSize = 15.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(allTransactions) { item ->
                                val dateStr = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.ENGLISH).format(Date(item.date))

                                when (item) {
                                    is CustomerTransactionItemDetail.Sale -> {
                                        val items = salesInvoiceItemsMap[item.sale.id] ?: emptyList()
                                        DetailedSalesInvoiceCard(
                                            sale = item.sale,
                                            dateStr = dateStr,
                                            items = items,
                                            productsMap = productsMap
                                        )
                                    }
                                    is CustomerTransactionItemDetail.Payment -> {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(18.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEBF0FE))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(16.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(42.dp)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(Color(0xFFE8F5E9)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(Icons.Outlined.Payments, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(22.dp))
                                                    }
                                                    Column {
                                                        Text(
                                                            "تحصيل دفعة (${if (item.payment.paymentMethod == "CASH") "نقدي" else "تحويل بنكي"})",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 16.sp,
                                                            color = Color(0xFF1B193B)
                                                        )
                                                        Text(dateStr, fontSize = 12.sp, color = Color(0xFF6B7280))
                                                        if (!item.payment.note.isNullOrBlank()) {
                                                            Text(item.payment.note!!, fontSize = 12.sp, color = Color(0xFF4B5563))
                                                        }
                                                    }
                                                }
                                                Text(
                                                    "+${item.payment.amount.formatCurrency()}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp,
                                                    color = Color(0xFF2E7D32)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Customer Info Tab
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        InfoDetailCard(title = "اسم العميل", value = cust.name, icon = Icons.Outlined.Person)
                        InfoDetailCard(title = "رقم الهاتف", value = cust.phone ?: "غير متوفر", icon = Icons.Outlined.Phone)
                        InfoDetailCard(title = "العنوان", value = cust.address ?: "غير محدد", icon = Icons.Outlined.LocationOn)
                        InfoDetailCard(title = "ملاحظات", value = cust.notes ?: "لا توجد ملاحظات", icon = Icons.Outlined.Description)
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }

        // Payment Collection Dialog
        if (showPaymentDialog && customer != null) {
            QuickCustomerPaymentDialog(
                customer = customer!!,
                onDismiss = { showPaymentDialog = false },
                onConfirm = { amount, method, reference, note ->
                    viewModel.addCustomerPayment(
                        customerId = customer!!.id,
                        amount = amount,
                        method = method,
                        reference = reference,
                        note = note,
                        onSuccess = {
                            showPaymentDialog = false
                            Toast.makeText(context, "تم تحصيل الدفعة بنجاح", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                            navController.navigate("customer_detail/$customerId")
                        }
                    )
                }
            )
        }

        // WhatsApp PDF Statement Dialog
        if (showPdfDialog && customer != null) {
            com.example.ui.components.CustomerPdfStatementDialog(
                customer = customer!!,
                viewModel = viewModel,
                onDismiss = { showPdfDialog = false }
            )
        }
    }
}

@Composable
fun InfoDetailCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEBF0FE))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(22.dp))
            Column {
                Text(text = title, fontSize = 12.sp, color = Color(0xFF6B7280), fontWeight = FontWeight.Medium)
                Text(text = value, fontSize = 16.sp, color = Color(0xFF1B193B), fontWeight = FontWeight.Bold)
            }
        }
    }
}

sealed class CustomerTransactionItemDetail(val date: Long) {
    class Sale(val sale: SalesInvoice) : CustomerTransactionItemDetail(sale.invoiceDate)
    class Payment(val payment: CustomerPayment) : CustomerTransactionItemDetail(payment.paymentDate)
}

@Composable
fun DetailedSalesInvoiceCard(
    sale: SalesInvoice,
    dateStr: String,
    items: List<SalesInvoiceItem>,
    productsMap: Map<String, Product>
) {
    var isExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Main Invoice Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.ShoppingCart,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "فاتورة مبيعات #${sale.invoiceNumber}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF1E1B4B)
                            )
                            // Payment method tag badge
                            val (payTag, tagBg, tagFg) = when (sale.paymentType) {
                                "CASH" -> Triple("نقدي", Color(0xFFDCFCE7), Color(0xFF166534))
                                "CREDIT" -> Triple("آجل", Color(0xFFFEE2E2), Color(0xFF991B1B))
                                else -> Triple("جزئي", Color(0xFFF3E8FF), Color(0xFF6B21A8))
                            }
                            Surface(
                                color = tagBg,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = payTag,
                                    color = tagFg,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(dateStr, fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        sale.totalAmount.formatCurrency(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = Color(0xFFDC2626)
                    )
                    if (sale.paidAmount > 0) {
                        Text(
                            "مسدد: ${sale.paidAmount.formatCurrency()}",
                            fontSize = 11.sp,
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (sale.remainingAmount > 0) {
                        Text(
                            "متبقي: ${sale.remainingAmount.formatCurrency()}",
                            fontSize = 11.sp,
                            color = Color(0xFFE11D48),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Expand/Collapse toggle bar with item count badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFE2E8F0))
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.List,
                        contentDescription = null,
                        tint = Color(0xFF4338CA),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "تفاصيل المنتجات (${items.size} أصناف)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isExpanded) "إخفاء التفاصيل" else "عرض التفاصيل",
                        fontSize = 11.sp,
                        color = Color(0xFF6366F1),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Products details breakdown
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (items.isEmpty()) {
                        Text(
                            "جاري تحميل الأصناف...",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(8.dp)
                        )
                    } else {
                        items.forEachIndexed { index, item ->
                            val product = productsMap[item.productId]
                            val productName = product?.name ?: "منتج (${item.productId.take(6)})"

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                shadowElevation = 1.dp
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
                                                .background(Color(0xFFEEF2FF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF4F46E5)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = productName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color(0xFF1E293B)
                                            )

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (!product?.color.isNullOrBlank()) {
                                                    Surface(
                                                        color = Color(0xFFF1F5F9),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = product!!.color!!,
                                                            fontSize = 10.sp,
                                                            color = Color(0xFF475569),
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                                if (!product?.size.isNullOrBlank()) {
                                                    Surface(
                                                        color = Color(0xFFF1F5F9),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = product!!.size!!,
                                                            fontSize = 10.sp,
                                                            color = Color(0xFF475569),
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "${item.quantity.formatQty()} × ${item.unitPrice.formatCurrency()}",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = item.lineTotal.formatCurrency(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }
                        }

                        if (sale.discount > 0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("خصم الفاتورة:", fontSize = 12.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                                Text("- ${sale.discount.formatCurrency()}", fontSize = 12.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
