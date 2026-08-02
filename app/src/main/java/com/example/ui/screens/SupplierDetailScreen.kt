package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.local.PurchaseInvoice
import com.example.data.local.Supplier
import com.example.data.local.SupplierPayment
import com.example.ui.viewmodels.StoreViewModel
import com.example.utils.formatCurrency
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SupplierDetailScreen(
    supplierId: String,
    viewModel: StoreViewModel,
    navController: NavController
) {
    var supplier by remember { mutableStateOf<Supplier?>(null) }
    var payments by remember { mutableStateOf<List<SupplierPayment>>(emptyList()) }
    var purchases by remember { mutableStateOf<List<PurchaseInvoice>>(emptyList()) }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: كشف حساب والمعاملات, 1: معلومات التواصل
    var showPaymentDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(supplierId) {
        supplier = viewModel.getSupplierById(supplierId)
        payments = viewModel.getSupplierPayments(supplierId)
        purchases = viewModel.getSupplierPurchases(supplierId)
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
                        colors = listOf(Color(0x333B82F6), Color.Transparent),
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
                    text = supplier?.name ?: "تفاصيل المورد",
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
                                listOf(Color(0xFF3B82F6), Color(0xFF6366F1), Color(0xFF8B5CF6))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Store,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            if (supplier != null) {
                val supp = supplier!!
                val balance = supp.balance

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
                            text = "الرصيد المالي الحالي للمورد",
                            color = Color(0xFFA5ABC7),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        val balanceText: String
                        val balanceColor: Color

                        if (balance > 0) {
                            balanceText = "أنا مدين له: ${balance.formatCurrency()}"
                            balanceColor = Color(0xFFFCA5A5)
                        } else if (balance < 0) {
                            balanceText = "له حساب دائن: ${(-balance).formatCurrency()}"
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
                            // Pay supplier button
                            Button(
                                onClick = { showPaymentDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2563EB),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تسديد دفعة", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            // Call button if phone exists
                            if (!supp.phone.isNullOrBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${supp.phone}"))
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
                    val allTransactions = remember(payments, purchases) {
                        val list = mutableListOf<SupplierTransactionItemDetail>()
                        purchases.forEach { list.add(SupplierTransactionItemDetail.Purchase(it)) }
                        payments.forEach { list.add(SupplierTransactionItemDetail.Payment(it)) }
                        list.sortedByDescending { it.date }
                    }

                    if (allTransactions.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("لا توجد معاملات مسجلة لهذا المورد", color = Color(0xFFA5ABC7), fontSize = 15.sp)
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
                                    is SupplierTransactionItemDetail.Purchase -> {
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
                                                            .background(Color(0xFFFFEAEA)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(22.dp))
                                                    }
                                                    Column {
                                                        Text("فاتورة مشتريات #${item.purchase.invoiceNumber}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1B193B))
                                                        Text(dateStr, fontSize = 12.sp, color = Color(0xFF6B7280))
                                                    }
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        item.purchase.totalAmount.formatCurrency(),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 16.sp,
                                                        color = Color(0xFFD32F2F)
                                                    )
                                                    if (item.purchase.paidAmount > 0) {
                                                        Text(
                                                            "مسدد: ${item.purchase.paidAmount.formatCurrency()}",
                                                            fontSize = 11.sp,
                                                            color = Color(0xFF2E7D32)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    is SupplierTransactionItemDetail.Payment -> {
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
                                                            "تسديد دفعة (${if (item.payment.paymentMethod == "CASH") "نقدي" else "تحويل بنكي"})",
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
                                                    "-${item.payment.amount.formatCurrency()}",
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
                    // Supplier Info Tab
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SupplierInfoDetailCard(title = "اسم المورد", value = supp.name, icon = Icons.Outlined.Store)
                        SupplierInfoDetailCard(title = "رقم الهاتف", value = supp.phone ?: "غير متوفر", icon = Icons.Outlined.Phone)
                        SupplierInfoDetailCard(title = "العنوان / المقر", value = supp.address ?: "غير محدد", icon = Icons.Outlined.LocationOn)
                        SupplierInfoDetailCard(title = "ملاحظات", value = supp.notes ?: "لا توجد ملاحظات", icon = Icons.Outlined.Description)
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }

        // Payment Collection Dialog
        if (showPaymentDialog && supplier != null) {
            QuickSupplierPaymentDialog(
                supplier = supplier!!,
                onDismiss = { showPaymentDialog = false },
                onConfirm = { amount, method, reference, note ->
                    viewModel.addSupplierPayment(
                        supplierId = supplier!!.id,
                        amount = amount,
                        method = method,
                        reference = reference,
                        note = note,
                        onSuccess = {
                            showPaymentDialog = false
                            Toast.makeText(context, "تم تسديد الدفعة بنجاح", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                            navController.navigate("supplier_detail/$supplierId")
                        }
                    )
                }
            )
        }
    }
}

@Composable
fun SupplierInfoDetailCard(
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
            Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(22.dp))
            Column {
                Text(text = title, fontSize = 12.sp, color = Color(0xFF6B7280), fontWeight = FontWeight.Medium)
                Text(text = value, fontSize = 16.sp, color = Color(0xFF1B193B), fontWeight = FontWeight.Bold)
            }
        }
    }
}

sealed class SupplierTransactionItemDetail(val date: Long) {
    class Purchase(val purchase: PurchaseInvoice) : SupplierTransactionItemDetail(purchase.invoiceDate)
    class Payment(val payment: SupplierPayment) : SupplierTransactionItemDetail(payment.paymentDate)
}
