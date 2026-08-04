package com.example.ui.components

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.Customer
import com.example.data.local.CustomerPayment
import com.example.data.local.SalesInvoice
import com.example.data.local.SalesInvoiceItem
import com.example.ui.screens.CustomerTransactionItemDetail
import com.example.ui.viewmodels.StoreViewModel
import com.example.utils.PdfStatementGenerator
import com.example.utils.formatCurrency
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerPdfStatementDialog(
    customer: Customer,
    viewModel: StoreViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var selectedPeriod by remember { mutableStateOf("ALL") } // "ALL", "MONTH", "DAYS30", "CUSTOM"
    var customFromDate by remember { mutableStateOf<Long?>(null) }
    var customToDate by remember { mutableStateOf<Long?>(null) }
    var includeItemsDetail by remember { mutableStateOf(true) }
    var isGenerating by remember { mutableStateOf(false) }

    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val productsMap = remember(allProducts) { allProducts.associateBy { it.id } }

    var payments by remember { mutableStateOf<List<CustomerPayment>>(emptyList()) }
    var sales by remember { mutableStateOf<List<SalesInvoice>>(emptyList()) }
    var salesInvoiceItemsMap by remember { mutableStateOf<Map<String, List<SalesInvoiceItem>>>(emptyMap()) }
    var isLoadingData by remember { mutableStateOf(true) }

    LaunchedEffect(customer.id) {
        isLoadingData = true
        payments = viewModel.getCustomerPayments(customer.id)
        sales = viewModel.getCustomerSales(customer.id)

        val map = mutableMapOf<String, List<SalesInvoiceItem>>()
        sales.forEach { sale ->
            val items = viewModel.getSalesInvoiceItems(sale.id)
            map[sale.id] = items
        }
        salesInvoiceItemsMap = map
        isLoadingData = false
    }

    val transactions = remember(payments, sales) {
        val list = mutableListOf<CustomerTransactionItemDetail>()
        sales.forEach { list.add(CustomerTransactionItemDetail.Sale(it)) }
        payments.forEach { list.add(CustomerTransactionItemDetail.Payment(it)) }
        list.sortedByDescending { it.date }
    }

    fun getPeriodTimeRange(): Pair<Long?, Long?> {
        val cal = Calendar.getInstance()
        val now = System.currentTimeMillis()
        return when (selectedPeriod) {
            "MONTH" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                Pair(cal.timeInMillis, now)
            }
            "DAYS30" -> {
                cal.add(Calendar.DAY_OF_YEAR, -30)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                Pair(cal.timeInMillis, now)
            }
            "CUSTOM" -> {
                if (customFromDate != null && customToDate != null) {
                    val fromCal = Calendar.getInstance().apply {
                        timeInMillis = customFromDate!!
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val toCal = Calendar.getInstance().apply {
                        timeInMillis = customToDate!!
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }
                    Pair(fromCal.timeInMillis, toCal.timeInMillis)
                } else Pair(null, null)
            }
            else -> Pair(null, null)
        }
    }

    fun generatePdfFile(): java.io.File? {
        val (fromTime, toTime) = getPeriodTimeRange()
        val config = PdfStatementGenerator.StatementConfig(
            fromTime = fromTime,
            toTime = toTime,
            includeItemsDetail = includeItemsDetail
        )
        return PdfStatementGenerator.generateCustomerStatementPdf(
            context = context,
            customer = customer,
            transactions = transactions,
            salesInvoiceItemsMap = salesInvoiceItemsMap,
            productsMap = productsMap,
            config = config
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.93f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(26.dp)),
            color = Color(0xFF1E1B4B),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF16A34A), Color(0xFF15803D))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PictureAsPdf,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "كشف حساب PDF متقدم",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Text(
                                text = "إرسال كشف الحساب عبر واتساب",
                                color = Color(0xFFA5ABC7),
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "إغلاق",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Customer Info Quick Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x20FFFFFF))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = customer.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = customer.phone ?: "بدون رقم هاتف",
                                color = Color(0xFFCBD5E1),
                                fontSize = 13.sp
                            )
                        }

                        val balance = customer.balance
                        val (balText, balColor) = when {
                            balance > 0 -> Pair("عليه دَين: ${balance.formatCurrency()}", Color(0xFFFCA5A5))
                            balance < 0 -> Pair("له مستحقات: ${(-balance).formatCurrency()}", Color(0xFF86EFAC))
                            else -> Pair("متزن (0 ريال)", Color.White)
                        }

                        Text(
                            text = balText,
                            color = balColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Period Selection Options
                Text(
                    text = "تحديد الفترة الزمنية لكشف الحساب:",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val periods = listOf(
                        "ALL" to "الكل",
                        "MONTH" to "هذا الشهر",
                        "DAYS30" to "آخر 30 يوم",
                        "CUSTOM" to "مخصص"
                    )
                    periods.forEach { (key, label) ->
                        FilterChip(
                            selected = selectedPeriod == key,
                            onClick = { selectedPeriod = key },
                            label = { Text(label, fontSize = 11.5.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF16A34A),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0x20FFFFFF),
                                labelColor = Color(0xFFE2E8F0)
                            )
                        )
                    }
                }

                if (selectedPeriod == "CUSTOM") {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance()
                                if (customFromDate != null) cal.timeInMillis = customFromDate!!
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        val selected = Calendar.getInstance().apply { set(year, month, day) }
                                        customFromDate = selected.timeInMillis
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(Color(0x60FFFFFF), Color(0x60FFFFFF))))
                        ) {
                            Text(
                                text = if (customFromDate != null) "من: ${dateFormat.format(Date(customFromDate!!))}" else "من تاريخ",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance()
                                if (customToDate != null) cal.timeInMillis = customToDate!!
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        val selected = Calendar.getInstance().apply { set(year, month, day) }
                                        customToDate = selected.timeInMillis
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(Color(0x60FFFFFF), Color(0x60FFFFFF))))
                        ) {
                            Text(
                                text = if (customToDate != null) "إلى: ${dateFormat.format(Date(customToDate!!))}" else "إلى تاريخ",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Detail Items Toggle Switch Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x18FFFFFF))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تضمين تفاصيل الأصناف المشتراة",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "عرض أسماء وكميات المنتجات داخل الفواتير",
                            color = Color(0xFFA5ABC7),
                            fontSize = 11.sp
                        )
                    }

                    Switch(
                        checked = includeItemsDetail,
                        onCheckedChange = { includeItemsDetail = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF16A34A)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (isLoadingData || isGenerating) {
                    CircularProgressIndicator(color = Color(0xFF16A34A))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isLoadingData) "جاري تحميل بيانات كشف الحساب..." else "جاري إنشاء ملف PDF وتجهيز واتساب...",
                        color = Color(0xFFA5ABC7),
                        fontSize = 12.sp
                    )
                } else {
                    // Action 1: Direct WhatsApp Button
                    Button(
                        onClick = {
                            isGenerating = true
                            val pdfFile = generatePdfFile()
                            isGenerating = false

                            if (pdfFile != null) {
                                val (fromTime, toTime) = getPeriodTimeRange()
                                val filteredTx = transactions.filter { tx ->
                                    val matchFrom = fromTime == null || tx.date >= fromTime
                                    val matchTo = toTime == null || tx.date <= toTime
                                    matchFrom && matchTo
                                }
                                var totalSales = 0.0
                                var totalPayments = 0.0
                                filteredTx.forEach { tx ->
                                    when (tx) {
                                        is CustomerTransactionItemDetail.Sale -> {
                                            totalSales += tx.sale.totalAmount
                                            totalPayments += tx.sale.paidAmount
                                        }
                                        is CustomerTransactionItemDetail.Payment -> totalPayments += tx.payment.amount
                                    }
                                }

                                PdfStatementGenerator.shareStatementToWhatsApp(
                                    context = context,
                                    pdfFile = pdfFile,
                                    customer = customer,
                                    totalSales = totalSales,
                                    totalPayments = totalPayments
                                )
                                onDismiss()
                            } else {
                                Toast.makeText(context, "فشل في إنشاء ملف PDF", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF16A34A),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Send,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إرسال كشف حساب PDF عبر واتساب",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action 2: Open / Preview PDF Button
                    OutlinedButton(
                        onClick = {
                            isGenerating = true
                            val pdfFile = generatePdfFile()
                            isGenerating = false

                            if (pdfFile != null) {
                                PdfStatementGenerator.openPdfFile(context, pdfFile)
                                onDismiss()
                            } else {
                                Toast.makeText(context, "فشل في إنشاء ملف PDF", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(listOf(Color.White, Color.White))
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Visibility,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "معاينة وفتح ملف PDF",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }
                }
            }
        }
    }
}
