package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.List
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.Customer
import com.example.data.local.Product
import com.example.data.local.SalesInvoiceItem
import com.example.ui.viewmodels.StoreViewModel
import com.example.utils.formatCurrency
import com.example.utils.formatQty
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class InvoiceItemUiModel(
    val product: Product,
    var quantity: Double,
    var unitPrice: Double,
    val unitCost: Double,
    var lineTotal: Double,
    var lineCost: Double,
    var lineProfit: Double
)

// Design System Palette (Emerald Green, Maroon Red, Pure White, Light Lavender)
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

private val PureWhite = Color(0xFFFFFFFF)
private val DarkText = Color(0xFF1E293B)
private val MutedText = Color(0xFF64748B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(viewModel: StoreViewModel, navController: androidx.navigation.NavController) {
    val products by viewModel.allProducts.collectAsStateWithLifecycle()
    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedCustomerId by remember { mutableStateOf<String?>(null) }
    var newCustomerName by remember { mutableStateOf("") }

    val invoiceItems = remember { mutableStateListOf<InvoiceItemUiModel>() }
    var discountText by remember { mutableStateOf("") }
    val discount = discountText.toDoubleOrNull() ?: 0.0

    var paymentType by remember { mutableStateOf("CASH") }
    var partialAmountText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    var showAddProductSheet by remember { mutableStateOf(false) }
    var selectedProductForConfig by remember { mutableStateOf<Product?>(null) }

    val subtotal = invoiceItems.sumOf { it.lineTotal }
    val totalCost = invoiceItems.sumOf { it.lineCost }
    val totalAmount = (subtotal - discount).coerceAtLeast(0.0)
    val totalProfit = invoiceItems.sumOf { it.lineProfit } - discount

    val pAmt = partialAmountText.toDoubleOrNull() ?: 0.0
    val finalPaidAmount = when (paymentType) {
        "CASH" -> totalAmount
        "CREDIT" -> 0.0
        else -> pAmt
    }
    val remainingAmount = (totalAmount - finalPaidAmount).coerceAtLeast(0.0)

    val mainBackgroundGradient = Brush.verticalGradient(
        colors = listOf(
            LightLavenderSurface,
            Color(0xFFF8FAFC),
            LightLavender.copy(alpha = 0.5f)
        )
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                SalesTopHeader(
                    onBack = { navController.popBackStack() },
                    onSalesHistory = { navController.navigate("sales_history") }
                )
            },
            bottomBar = {
                SalesBottomActionBar(
                    totalAmount = totalAmount,
                    itemCount = invoiceItems.size,
                    isProcessing = isProcessing,
                    onSaveInvoice = {
                        if (invoiceItems.isEmpty()) {
                            Toast.makeText(context, "الفاتورة فارغة، يرجى إضافة منتجات", Toast.LENGTH_SHORT).show()
                            return@SalesBottomActionBar
                        }

                        val isCredit = paymentType != "CASH"
                        if (isCredit && selectedCustomerId == null) {
                            Toast.makeText(
                                context,
                                "يجب اختيار عميل مسجل للبيع الآجل أو الدفع الجزئي",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@SalesBottomActionBar
                        }

                        isProcessing = true

                        val itemsToSave = invoiceItems.map { uiItem ->
                            SalesInvoiceItem(
                                invoiceId = "",
                                productId = uiItem.product.id,
                                quantity = uiItem.quantity,
                                unitPrice = uiItem.unitPrice,
                                unitCost = uiItem.unitCost,
                                lineTotal = uiItem.lineTotal,
                                lineCost = uiItem.lineCost,
                                lineProfit = uiItem.lineProfit
                            )
                        }

                        val custName = customers.find { it.id == selectedCustomerId }?.name ?: newCustomerName

                        viewModel.processSale(
                            customerName = custName,
                            customerId = selectedCustomerId,
                            items = itemsToSave,
                            discount = discount,
                            paidAmount = finalPaidAmount,
                            onSuccess = {
                                isProcessing = false
                                Toast.makeText(context, "تم حفظ الفاتورة بنجاح!", Toast.LENGTH_SHORT).show()
                                invoiceItems.clear()
                                discountText = ""
                                newCustomerName = ""
                                partialAmountText = ""
                                navController.popBackStack("dashboard", false)
                            }
                        )
                    }
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
                    // 1. Customer Selection Card
                    item {
                        CustomerSelectionCard(
                            customers = customers,
                            selectedCustomerId = selectedCustomerId,
                            onCustomerSelected = { selectedCustomerId = it },
                            newCustomerName = newCustomerName,
                            onNewCustomerNameChange = { newCustomerName = it },
                            onRegisterNewCustomer = {
                                if (newCustomerName.isNotBlank()) {
                                    viewModel.addCustomer(newCustomerName, "")
                                    Toast.makeText(context, "تم تسجيل العميل بنجاح", Toast.LENGTH_SHORT).show()
                                    newCustomerName = ""
                                }
                            }
                        )
                    }

                    // 2. Invoice Products Section Header
                    item {
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
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaroonBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ShoppingCart,
                                        contentDescription = null,
                                        tint = MaroonPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "منتجات الفاتورة (${invoiceItems.size})",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkText
                                )
                            }

                            // Add Product Button in Emerald Green
                            Button(
                                onClick = { showAddProductSheet = true },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Add,
                                    contentDescription = null,
                                    tint = PureWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "إضافة منتج",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = PureWhite
                                )
                            }
                        }
                    }

                    // 3. Invoice Items List or Empty Placeholder
                    if (invoiceItems.isEmpty()) {
                        item {
                            EmptyInvoicePlaceholder(onAddProduct = { showAddProductSheet = true })
                        }
                    } else {
                        items(items = invoiceItems, key = { it.product.id }) { item ->
                            InvoiceItemRowCard(
                                item = item,
                                onQuantityChange = { delta ->
                                    val newQty = item.quantity + delta
                                    if (newQty > 0) {
                                        if (newQty > item.product.stockQuantity) {
                                            Toast.makeText(
                                                context,
                                                "المخزون المتوفر ${item.product.stockQuantity.formatQty()} فقط",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            val total = newQty * item.unitPrice
                                            val cost = newQty * item.unitCost
                                            val profit = total - cost
                                            val idx = invoiceItems.indexOf(item)
                                            if (idx != -1) {
                                                invoiceItems[idx] = item.copy(
                                                    quantity = newQty,
                                                    lineTotal = total,
                                                    lineCost = cost,
                                                    lineProfit = profit
                                                )
                                            }
                                        }
                                    }
                                },
                                onRemove = { invoiceItems.remove(item) },
                                onClickConfig = { selectedProductForConfig = item.product }
                            )
                        }
                    }

                    // 4. Totals and Payment Method Section
                    if (invoiceItems.isNotEmpty()) {
                        item {
                            FinancialSummaryCard(
                                subtotal = subtotal,
                                discountText = discountText,
                                onDiscountTextChange = { discountText = it },
                                totalAmount = totalAmount,
                                totalProfit = totalProfit,
                                paymentType = paymentType,
                                onPaymentTypeChange = { paymentType = it },
                                partialAmountText = partialAmountText,
                                onPartialAmountTextChange = { partialAmountText = it },
                                finalPaidAmount = finalPaidAmount,
                                remainingAmount = remainingAmount
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }

        // Product Configuration Dialog (Dimensions/Custom Price)
        if (selectedProductForConfig != null) {
            ProductConfigDialog(
                product = selectedProductForConfig!!,
                onDismiss = { selectedProductForConfig = null },
                onSave = { qty, price ->
                    val product = selectedProductForConfig!!
                    if (qty > product.stockQuantity) {
                        Toast.makeText(
                            context,
                            "المخزون المتوفر ${product.stockQuantity.formatQty()} فقط",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    val existing = invoiceItems.find { it.product.id == product.id }
                    if (existing != null) {
                        val idx = invoiceItems.indexOf(existing)
                        if (idx != -1) {
                            val total = qty * price
                            val cost = qty * existing.unitCost
                            val profit = total - cost
                            invoiceItems[idx] = existing.copy(
                                quantity = qty,
                                unitPrice = price,
                                lineTotal = total,
                                lineCost = cost,
                                lineProfit = profit
                            )
                        }
                    } else {
                        invoiceItems.add(
                            InvoiceItemUiModel(
                                product = product,
                                quantity = qty,
                                unitPrice = price,
                                unitCost = product.costPrice,
                                lineTotal = qty * price,
                                lineCost = qty * product.costPrice,
                                lineProfit = (qty * price) - (qty * product.costPrice)
                            )
                        )
                    }
                    selectedProductForConfig = null
                }
            )
        }

        // Add Product Bottom Sheet
        if (showAddProductSheet) {
            AddProductBottomSheet(
                products = products,
                invoiceItems = invoiceItems,
                onDismiss = { showAddProductSheet = false },
                onSelectProductForConfig = { selectedProductForConfig = it }
            )
        }
    }
}

@Composable
fun SalesTopHeader(
    onBack: () -> Unit,
    onSalesHistory: () -> Unit
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
                        text = "فاتورة مبيعات جديدة",
                        color = PureWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("ar")).format(Date()),
                        color = LightLavender,
                        fontSize = 11.sp
                    )
                }
            }

            // Sales History Pill Button
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onSalesHistory() },
                color = Color(0x22FFFFFF),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.List,
                        contentDescription = null,
                        tint = EmeraldLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "سجل الفواتير",
                        color = PureWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerSelectionCard(
    customers: List<Customer>,
    selectedCustomerId: String?,
    onCustomerSelected: (String?) -> Unit,
    newCustomerName: String,
    onNewCustomerNameChange: (String) -> Unit,
    onRegisterNewCustomer: () -> Unit
) {
    var customerSelectionExpanded by remember { mutableStateOf(false) }
    val selectedCustomer = customers.find { it.id == selectedCustomerId }

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
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(LightLavender),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = MaroonPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "تفاصيل العميل",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = DarkText
                    )
                }

                if (selectedCustomerId != null) {
                    Surface(
                        color = EmeraldBg,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "عميل مسجل",
                            color = EmeraldDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Surface(
                        color = LightLavender,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "زبون عابر",
                            color = MaroonPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = customerSelectionExpanded,
                onExpandedChange = { customerSelectionExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedCustomer?.name ?: "زبون عابر (نقدي)",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("اختر العميل", fontSize = 12.sp) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerSelectionExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaroonPrimary,
                        unfocusedBorderColor = LightLavenderBorder,
                        focusedContainerColor = LightLavenderSurface,
                        unfocusedContainerColor = LightLavenderSurface
                    )
                )

                ExposedDropdownMenu(
                    expanded = customerSelectionExpanded,
                    onDismissRequest = { customerSelectionExpanded = false },
                    modifier = Modifier.background(PureWhite)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "زبون عابر (نقدي)",
                                fontWeight = FontWeight.Bold,
                                color = MaroonPrimary
                            )
                        },
                        onClick = {
                            onCustomerSelected(null)
                            customerSelectionExpanded = false
                        }
                    )
                    HorizontalDivider(color = LightLavenderBorder)
                    customers.forEach { cust ->
                        DropdownMenuItem(
                            text = { Text(cust.name, color = DarkText) },
                            onClick = {
                                onCustomerSelected(cust.id)
                                customerSelectionExpanded = false
                            }
                        )
                    }
                }
            }

            if (selectedCustomerId == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCustomerName,
                        onValueChange = onNewCustomerNameChange,
                        label = { Text("اسم الزبون العابر (اختياري)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = LightLavenderBorder
                        )
                    )

                    Button(
                        onClick = onRegisterNewCustomer,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        enabled = newCustomerName.isNotBlank()
                    ) {
                        Text("حفظ كعميل", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyInvoicePlaceholder(onAddProduct: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, LightLavenderBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(LightLavender),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.AddShoppingCart,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "الفاتورة فارغة حالياً",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = DarkText
            )

            Text(
                text = "اضغط على زر إمكانية إضافة منتج لإدراج الأصناف بالكمية والأسعار",
                fontSize = 12.sp,
                color = MutedText,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
            )

            Button(
                onClick = onAddProduct,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("إضافة منتجات الآن", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun InvoiceItemRowCard(
    item: InvoiceItemUiModel,
    onQuantityChange: (Double) -> Unit,
    onRemove: () -> Unit,
    onClickConfig: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClickConfig() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, LightLavenderBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(EmeraldBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Inventory2,
                            contentDescription = null,
                            tint = EmeraldDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = item.product.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = DarkText
                        )
                        Text(
                            text = "سعر الوحدة: ${item.unitPrice.formatCurrency()}",
                            fontSize = 11.sp,
                            color = MutedText
                        )
                    }
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaroonBg)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "حذف",
                        tint = MaroonPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quantity Control Bar & Line Total
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(LightLavenderSurface)
                    .border(1.dp, LightLavenderBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "الكمية:",
                        fontSize = 12.sp,
                        color = MutedText,
                        fontWeight = FontWeight.Medium
                    )

                    Surface(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onQuantityChange(1.0) },
                        color = EmeraldPrimary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("+", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    Text(
                        text = item.quantity.formatQty(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DarkText,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )

                    Surface(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onQuantityChange(-1.0) },
                        color = MaroonPrimary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("-", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
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

@Composable
fun FinancialSummaryCard(
    subtotal: Double,
    discountText: String,
    onDiscountTextChange: (String) -> Unit,
    totalAmount: Double,
    totalProfit: Double,
    paymentType: String,
    onPaymentTypeChange: (String) -> Unit,
    partialAmountText: String,
    onPartialAmountTextChange: (String) -> Unit,
    finalPaidAmount: Double,
    remainingAmount: Double
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
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "ملخص الحسابات والدفع",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = DarkText
            )

            // Subtotal Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("المجموع الفرعي:", fontSize = 13.sp, color = MutedText)
                Text(subtotal.formatCurrency(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkText)
            }

            // Discount Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("خصم إضافي:", fontSize = 13.sp, color = MutedText)
                OutlinedTextField(
                    value = discountText,
                    onValueChange = onDiscountTextChange,
                    placeholder = { Text("0.0", fontSize = 12.sp) },
                    modifier = Modifier.width(110.dp),
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaroonPrimary,
                        unfocusedBorderColor = LightLavenderBorder
                    )
                )
            }

            HorizontalDivider(color = LightLavenderBorder)

            // Final Total Highlight Box in Emerald & Maroon Accent
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(MaroonDark, MaroonPrimary)
                        )
                    )
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("الإجمالي النهائي المطلوب:", color = LightLavender, fontSize = 12.sp)
                        Text(
                            totalAmount.formatCurrency(),
                            color = PureWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Surface(
                        color = Color(0x33FFFFFF),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "الربح المقدر: ${totalProfit.formatCurrency()}",
                            color = EmeraldLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Payment Method Pill Switch
            Text("طريقة الدفع", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkText)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(LightLavender)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val types = listOf("CASH" to "نقدي", "CREDIT" to "آجل", "PARTIAL" to "جزئي")
                types.forEach { (typeKey, label) ->
                    val isSelected = paymentType == typeKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(50))
                            .then(
                                if (isSelected) {
                                    Modifier.background(
                                        when (typeKey) {
                                            "CASH" -> EmeraldPrimary
                                            "CREDIT" -> MaroonPrimary
                                            else -> Color(0xFF7C3AED)
                                        }
                                    )
                                } else Modifier
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onPaymentTypeChange(typeKey) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) PureWhite else DarkText,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            if (paymentType == "PARTIAL") {
                OutlinedTextField(
                    value = partialAmountText,
                    onValueChange = onPartialAmountTextChange,
                    label = { Text("المبلغ المدفوع حالياً (نقداً)", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = LightLavenderBorder
                    )
                )
            }

            if (paymentType != "CASH") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(LightLavenderSurface)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("المدفوع نقداً:", fontSize = 11.sp, color = MutedText)
                        Text(
                            finalPaidAmount.formatCurrency(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDark
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("المتبقي في الذمة:", fontSize = 11.sp, color = MutedText)
                        Text(
                            remainingAmount.formatCurrency(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaroonPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SalesBottomActionBar(
    totalAmount: Double,
    itemCount: Int,
    isProcessing: Boolean,
    onSaveInvoice: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PureWhite,
        shadowElevation = 12.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, LightLavenderBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "الإجمالي ($itemCount منتجات)",
                    fontSize = 11.sp,
                    color = MutedText
                )
                Text(
                    text = totalAmount.formatCurrency(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldDark
                )
            }

            Button(
                onClick = onSaveInvoice,
                modifier = Modifier
                    .height(50.dp)
                    .weight(1.5f),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                enabled = !isProcessing
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(EmeraldPrimary, EmeraldLight)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = PureWhite,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "حفظ الفاتورة",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductBottomSheet(
    products: List<Product>,
    invoiceItems: List<InvoiceItemUiModel>,
    onDismiss: () -> Unit,
    onSelectProductForConfig: (Product) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredProducts = remember(products, searchQuery) {
        products.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    (it.code != null && it.code.contains(searchQuery, ignoreCase = true))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.85f),
        containerColor = PureWhite,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "اختيار منتجات للفاتورة",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )

                TextButton(onClick = onDismiss) {
                    Text("تم", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث باسم المنتج أو الكود...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = MaroonPrimary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = LightLavenderBorder
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = filteredProducts, key = { it.id }) { product ->
                    val existingItem = invoiceItems.find { it.product.id == product.id }
                    val currentQty = existingItem?.quantity ?: 0.0

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (currentQty > 0) EmeraldBg else LightLavenderSurface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (currentQty > 0) EmeraldPrimary else LightLavenderBorder
                        )
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
                                    product.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = DarkText
                                )
                                Text(
                                    "السعر: ${product.suggestedPrice.formatCurrency()} | المخزون: ${product.stockQuantity.formatQty()}",
                                    fontSize = 11.sp,
                                    color = MutedText
                                )
                            }

                            if (currentQty == 0.0) {
                                Button(
                                    onClick = { onSelectProductForConfig(product) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("تحديد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Surface(
                                    color = EmeraldPrimary,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        "مضاف (${currentQty.formatQty()})",
                                        color = PureWhite,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductConfigDialog(
    product: Product,
    onDismiss: () -> Unit,
    onSave: (quantity: Double, unitPrice: Double) -> Unit
) {
    var isDimensional by remember { mutableStateOf(false) }
    var length by remember { mutableStateOf("") }
    var width by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var unitPrice by remember { mutableStateOf(product.suggestedPrice.toString()) }

    val calculatedQuantity = remember(isDimensional, length, width, quantity) {
        if (isDimensional) {
            val l = length.toDoubleOrNull() ?: 0.0
            val w = width.toDoubleOrNull() ?: 0.0
            if (l > 0 && w > 0) (l * w) / 4.0 else 0.0
        } else {
            quantity.toDoubleOrNull() ?: 0.0
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PureWhite,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                product.name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = DarkText
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isDimensional,
                        onCheckedChange = { isDimensional = it },
                        colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                    )
                    Text("بيع بالمقاس (طول × عرض)", fontSize = 13.sp, color = DarkText)
                }

                if (isDimensional) {
                    Text(
                        "المعيار: يحسب كل 4 متر مربع = 1 متر طولي",
                        fontSize = 11.sp,
                        color = MaroonPrimary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = length,
                            onValueChange = { length = it },
                            label = { Text("الطول (متر)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = width,
                            onValueChange = { width = it },
                            label = { Text("العرض (متر)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    Text(
                        "الكمية المحسوبة: ${calculatedQuantity.formatQty()} وحدة",
                        fontWeight = FontWeight.Bold,
                        color = EmeraldDark,
                        fontSize = 12.sp
                    )
                } else {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("الكمية", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                OutlinedTextField(
                    value = unitPrice,
                    onValueChange = { unitPrice = it },
                    label = { Text("سعر البيع للوحدة", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                val currentTotal = calculatedQuantity * (unitPrice.toDoubleOrNull() ?: 0.0)
                Text(
                    "الإجمالي: ${currentTotal.formatCurrency()}",
                    fontWeight = FontWeight.Bold,
                    color = EmeraldDark,
                    fontSize = 15.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalPrice = unitPrice.toDoubleOrNull() ?: 0.0
                    if (calculatedQuantity > 0 && finalPrice >= 0) {
                        onSave(calculatedQuantity, finalPrice)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("إضافة للفاتورة", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = MaroonPrimary)
            }
        }
    )
}
