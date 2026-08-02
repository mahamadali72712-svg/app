import re

new_content = """package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.ShoppingCart
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.Product
import com.example.data.local.PurchaseInvoiceItem
import com.example.ui.viewmodels.StoreViewModel
import com.example.utils.formatCurrency
import com.example.utils.formatQty
import java.util.UUID

// Luxurious Light Colors
val LightGold = Color(0xFFFFD700)
val SoftPeach = Color(0xFFFFF2E6)
val SoftRose = Color(0xFFFDE8E9)
val LuxuryGold = Color(0xFFD4AF37)
val RoyalPurpleLight = Color(0xFFEADDFF)
val WhitePure = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF333333)
val TextSecondary = Color(0xFF757575)

val PurchaseBgGradient = Brush.verticalGradient(
    0.0f to SoftPeach,
    0.5f to SoftRose,
    1.0f to WhitePure
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun PurchasesScreen(viewModel: StoreViewModel, navController: androidx.navigation.NavController) {
    val products by viewModel.allProducts.collectAsStateWithLifecycle()
    val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    var supplierId by remember { mutableStateOf("") }
    var newSupplierName by remember { mutableStateOf("") }
    
    val invoiceItems = remember { mutableStateListOf<PurchaseItemUiModel>() }
    var showAddProductSheet by remember { mutableStateOf(false) }
    var selectedProductForConfig by remember { mutableStateOf<Product?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    
    val totalCost = invoiceItems.sumOf { it.lineTotal }
    
    var paymentType by remember { mutableStateOf("CASH") }
    var partialAmountText by remember { mutableStateOf("") }
    
    val pAmt = partialAmountText.toDoubleOrNull() ?: 0.0
    val finalPaidAmount = when (paymentType) {
        "CASH" -> totalCost
        "CREDIT" -> 0.0
        else -> pAmt
    }
    val remainingAmount = totalCost - finalPaidAmount

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            LuxuryPurchaseTopBar(
                onBack = { navController.popBackStack() },
                onHistory = { navController.navigate("purchases_history") }
            )
        },
        bottomBar = {
            Surface(
                color = WhitePure,
                shadowElevation = 16.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Box(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Button(
                        onClick = {
                            if (invoiceItems.isEmpty()) {
                                Toast.makeText(context, "الرجاء إضافة منتجات", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (supplierId.isEmpty() && newSupplierName.isBlank()) {
                                Toast.makeText(context, "الرجاء اختيار مورد أو إدخال مورد جديد", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isProcessing = true
                            
                            val createInvoice = { sId: String ->
                                val items = invoiceItems.map { 
                                    PurchaseInvoiceItem(
                                        productId = it.product.id,
                                        quantity = it.quantity,
                                        unitCost = it.unitCost,
                                        lineTotal = it.lineTotal
                                    )
                                }
                                viewModel.addPurchaseInvoice(
                                    supplierId = sId,
                                    items = items,
                                    totalAmount = totalCost,
                                    paidAmount = finalPaidAmount,
                                    onSuccess = {
                                        isProcessing = false
                                        Toast.makeText(context, "تم حفظ فاتورة المشتريات", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    }
                                )
                            }
                            
                            if (supplierId.isEmpty() && newSupplierName.isNotBlank()) {
                                val newId = UUID.randomUUID().toString()
                                viewModel.addSupplier(
                                    name = newSupplierName,
                                    phone = "",
                                    address = "",
                                    onSuccess = {
                                        createInvoice(newId)
                                    }
                                )
                            } else {
                                createInvoice(supplierId)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = !isProcessing,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold)
                    ) {
                        Text("اعتماد فاتورة المشتريات", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = WhitePure)
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PurchaseBgGradient)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Supplier Section
                item {
                    LuxurySectionCard(title = "بيانات المورد") {
                        Column {
                            var isNewSupplier by remember { mutableStateOf(false) }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LuxuryFilterChip(
                                    text = "مورد مسجل",
                                    isSelected = !isNewSupplier,
                                    onClick = { isNewSupplier = false },
                                    modifier = Modifier.weight(1f)
                                )
                                LuxuryFilterChip(
                                    text = "مورد جديد",
                                    isSelected = isNewSupplier,
                                    onClick = { isNewSupplier = true },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            AnimatedContent(targetState = isNewSupplier, transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) }) { isNew ->
                                if (isNew) {
                                    OutlinedTextField(
                                        value = newSupplierName,
                                        onValueChange = { newSupplierName = it },
                                        label = { Text("اسم المورد الجديد", color = TextSecondary) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = LuxuryGold,
                                            unfocusedBorderColor = Color(0xFFE0E0E0)
                                        )
                                    )
                                } else {
                                    if (suppliers.isEmpty()) {
                                        Text("لا يوجد موردين مسجلين", color = Color.Red, fontSize = 14.sp)
                                    } else {
                                        var expanded by remember { mutableStateOf(false) }
                                        ExposedDropdownMenuBox(
                                            expanded = expanded,
                                            onExpandedChange = { expanded = !expanded }
                                        ) {
                                            OutlinedTextField(
                                                value = suppliers.find { it.id == supplierId }?.name ?: "اختر مورد",
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("المورد", color = TextSecondary) },
                                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = LuxuryGold,
                                                    unfocusedBorderColor = Color(0xFFE0E0E0)
                                                )
                                            )
                                            ExposedDropdownMenu(
                                                expanded = expanded,
                                                onDismissRequest = { expanded = false },
                                                modifier = Modifier.background(WhitePure)
                                            ) {
                                                suppliers.forEach { sup ->
                                                    DropdownMenuItem(
                                                        text = { Text(sup.name) },
                                                        onClick = {
                                                            supplierId = sup.id
                                                            expanded = false
                                                        }
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
                
                // Products Section
                item {
                    LuxurySectionCard(title = "المنتجات") {
                        Column {
                            Button(
                                onClick = { showAddProductSheet = true },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SoftPeach, contentColor = LuxuryGold),
                                border = borderStrokeForLuxury()
                            ) {
                                Icon(Icons.Outlined.AddCircleOutline, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("إضافة منتجات للفاتورة", fontWeight = FontWeight.Bold)
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            if (invoiceItems.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text("الفاتورة فارغة حالياً", color = TextSecondary)
                                }
                            } else {
                                invoiceItems.forEachIndexed { index, item ->
                                    LuxuryInvoiceItemCard(
                                        item = item,
                                        onIncrease = {
                                            item.quantity += 1.0
                                            item.lineTotal = item.unitCost * item.quantity
                                            invoiceItems[index] = item.copy()
                                        },
                                        onDecrease = {
                                            if (item.quantity <= 1.0) {
                                                invoiceItems.removeAt(index)
                                            } else {
                                                item.quantity -= 1.0
                                                item.lineTotal = item.unitCost * item.quantity
                                                invoiceItems[index] = item.copy()
                                            }
                                        },
                                        onRemove = { invoiceItems.removeAt(index) }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
                
                // Totals & Payment Section
                item {
                    LuxurySectionCard(title = "الدفع والتفاصيل") {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("إجمالي الفاتورة", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(totalCost.formatCurrency(), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = LuxuryGold)
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = Color(0xFFEEEEEE))
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text("طريقة الدفع", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                LuxuryFilterChip(text = "نقدي كامل", isSelected = paymentType == "CASH", onClick = { paymentType = "CASH" }, modifier = Modifier.weight(1f))
                                LuxuryFilterChip(text = "آجل كامل", isSelected = paymentType == "CREDIT", onClick = { paymentType = "CREDIT" }, modifier = Modifier.weight(1f))
                                LuxuryFilterChip(text = "مبلغ جزئي", isSelected = paymentType == "PARTIAL", onClick = { paymentType = "PARTIAL" }, modifier = Modifier.weight(1f))
                            }
                            
                            if (paymentType == "PARTIAL") {
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = partialAmountText,
                                    onValueChange = { partialAmountText = it },
                                    label = { Text("المبلغ المدفوع", color = TextSecondary) },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = LuxuryGold,
                                        unfocusedBorderColor = Color(0xFFE0E0E0)
                                    )
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            if (paymentType != "CASH") {
                                Row(modifier = Modifier.fillMaxWidth().background(SoftRose, RoundedCornerShape(8.dp)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("المبلغ المتبقي (آجل)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                                    Text(remainingAmount.formatCurrency(), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFD32F2F))
                                }
                            }
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
    
    if (showAddProductSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddProductSheet = false },
            containerColor = WhitePure,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f).padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("تحديد المنتجات للشراء", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(16.dp))
                
                if (products.isEmpty()) {
                    Text("لا توجد منتجات مسجلة.", color = TextSecondary)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(products) { product ->
                            val currentQty = invoiceItems.find { it.product.id == product.id }?.quantity ?: 0.0
                            
                            Card(
                                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(16.dp)),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = WhitePure),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(product.name, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("التكلفة: ${product.costPrice.formatCurrency()} | المخزون الحالي: ${product.stockQuantity}", fontSize = 12.sp, color = TextSecondary)
                                    }
                                    
                                    if (currentQty == 0.0) {
                                        Button(
                                            onClick = { selectedProductForConfig = product },
                                            colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("إضافة", fontWeight = FontWeight.Bold, color = WhitePure)
                                        }
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.background(SoftPeach, RoundedCornerShape(20.dp)).padding(horizontal = 4.dp, vertical = 4.dp)
                                        ) {
                                            Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(WhitePure).clickable {
                                                val existingItem = invoiceItems.find { it.product.id == product.id }
                                                if (existingItem != null) {
                                                    existingItem.quantity += 1.0
                                                    existingItem.lineTotal = existingItem.unitCost * existingItem.quantity
                                                    val idx = invoiceItems.indexOf(existingItem)
                                                    if (idx != -1) invoiceItems[idx] = existingItem.copy()
                                                }
                                            }, contentAlignment = Alignment.Center) {
                                                Text("+", color = LuxuryGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            }
                                            
                                            Text("${currentQty.formatQty()}", modifier = Modifier.padding(horizontal = 12.dp), fontWeight = FontWeight.Bold, color = TextPrimary)
                                            
                                            Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(WhitePure).clickable {
                                                val existingItem = invoiceItems.find { it.product.id == product.id }
                                                if (existingItem != null) {
                                                    if (existingItem.quantity <= 1.0) {
                                                        invoiceItems.remove(existingItem)
                                                    } else {
                                                        existingItem.quantity -= 1.0
                                                        existingItem.lineTotal = existingItem.unitCost * existingItem.quantity
                                                        val idx = invoiceItems.indexOf(existingItem)
                                                        if (idx != -1) invoiceItems[idx] = existingItem.copy()
                                                    }
                                                }
                                            }, contentAlignment = Alignment.Center) {
                                                Text("-", color = LuxuryGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (selectedProductForConfig != null) {
        PurchaseProductConfigDialog(
            product = selectedProductForConfig!!,
            onDismiss = { selectedProductForConfig = null },
            onSave = { qty, cost ->
                invoiceItems.add(
                    PurchaseItemUiModel(
                        product = selectedProductForConfig!!,
                        quantity = qty,
                        unitCost = cost,
                        lineTotal = qty * cost
                    )
                )
                selectedProductForConfig = null
            }
        )
    }
}

@Composable
fun LuxuryPurchaseTopBar(onBack: () -> Unit, onHistory: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(WhitePure)
                .shadow(2.dp, CircleShape)
                .clickable { onHistory() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.List, contentDescription = "سجل المشتريات", tint = LuxuryGold)
        }
        Spacer(modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("المشتريات", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            Text("إنشاء فاتورة شراء", fontSize = 12.sp, color = TextSecondary)
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
fun LuxurySectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = LuxuryGold.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = WhitePure)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = LuxuryGold)
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun LuxuryFilterChip(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) LuxuryGold else WhitePure)
            .border(1.dp, if (isSelected) Color.Transparent else Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) WhitePure else TextSecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

@Composable
fun LuxuryInvoiceItemCard(
    item: PurchaseItemUiModel,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WhitePure),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Text(item.product.name, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = Color(0xFFD32F2F))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("السعر: ${item.unitCost.formatCurrency()}", fontSize = 13.sp, color = TextSecondary)
                    Text("الإجمالي: ${item.lineTotal.formatCurrency()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LuxuryGold)
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.background(SoftPeach, RoundedCornerShape(20.dp)).padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(WhitePure).clickable { onIncrease() }, contentAlignment = Alignment.Center) {
                        Text("+", color = LuxuryGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Text("${item.quantity.formatQty()}", modifier = Modifier.padding(horizontal = 12.dp), fontWeight = FontWeight.Bold, color = TextPrimary)
                    Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(WhitePure).clickable { onDecrease() }, contentAlignment = Alignment.Center) {
                        Text("-", color = LuxuryGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun borderStrokeForLuxury() = androidx.compose.foundation.BorderStroke(1.dp, LuxuryGold.copy(alpha = 0.3f))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseProductConfigDialog(
    product: Product,
    onDismiss: () -> Unit,
    onSave: (quantity: Double, unitCost: Double) -> Unit
) {
    var isDimensional by remember { mutableStateOf(false) }
    var length by remember { mutableStateOf("") }
    var width by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var unitCost by remember { mutableStateOf(product.costPrice.toString()) }
    
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
        containerColor = WhitePure,
        titleContentColor = LuxuryGold,
        textContentColor = TextPrimary,
        shape = RoundedCornerShape(24.dp),
        title = { Text("تحديد الكمية والسعر", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(product.name, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { isDimensional = !isDimensional }.padding(vertical = 4.dp)) {
                    Checkbox(checked = isDimensional, onCheckedChange = { isDimensional = it }, colors = CheckboxDefaults.colors(checkedColor = LuxuryGold))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("شراء بالمقاس (طول × عرض)", color = TextPrimary, fontWeight = FontWeight.Medium)
                }
                
                if (isDimensional) {
                    Text("المعيار: يحسب كل 4 متر مربع = 1 متر طولي", fontSize = 12.sp, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = length,
                            onValueChange = { length = it },
                            label = { Text("الطول (متر)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxuryGold, unfocusedBorderColor = Color(0xFFE0E0E0))
                        )
                        OutlinedTextField(
                            value = width,
                            onValueChange = { width = it },
                            label = { Text("العرض (متر)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxuryGold, unfocusedBorderColor = Color(0xFFE0E0E0))
                        )
                    }
                    Text("الكمية المحسوبة: ${calculatedQuantity.formatQty()} وحدة", fontWeight = FontWeight.Bold, color = LuxuryGold)
                } else {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("الكمية") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxuryGold, unfocusedBorderColor = Color(0xFFE0E0E0))
                    )
                }
                OutlinedTextField(
                    value = unitCost,
                    onValueChange = { unitCost = it },
                    label = { Text("سعر الشراء للوحدة") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxuryGold, unfocusedBorderColor = Color(0xFFE0E0E0))
                )
                
                val currentTotal = calculatedQuantity * (unitCost.toDoubleOrNull() ?: 0.0)
                Row(modifier = Modifier.fillMaxWidth().background(SoftPeach, RoundedCornerShape(12.dp)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("الإجمالي:", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(currentTotal.formatCurrency(), fontWeight = FontWeight.ExtraBold, color = LuxuryGold, fontSize = 16.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalCost = unitCost.toDoubleOrNull() ?: 0.0
                    if (calculatedQuantity > 0 && finalCost >= 0) {
                        onSave(calculatedQuantity, finalCost)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("تأكيد والإضافة", fontWeight = FontWeight.Bold, color = WhitePure)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)) {
                Text("إلغاء", fontWeight = FontWeight.Bold)
            }
        }
    )
}
"""
with open('app/src/main/java/com/example/ui/screens/PurchasesScreen.kt', 'w') as f:
    f.write(new_content)

