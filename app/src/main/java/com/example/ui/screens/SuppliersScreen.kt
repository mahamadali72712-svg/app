package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.data.local.Supplier
import com.example.ui.viewmodels.StoreViewModel
import com.example.utils.formatCurrency

@Composable
fun SuppliersScreen(viewModel: StoreViewModel, navController: NavController) {
    val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showAddEditSheet by remember { mutableStateOf(false) }
    var supplierToEdit by remember { mutableStateOf<Supplier?>(null) }

    var showPaymentDialogForSupplier by remember { mutableStateOf<Supplier?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: الكل, 1: أنا مدين له, 2: له حساب دائن, 3: متزن

    // Stats calculations
    val totalSupplierDebts = remember(suppliers) {
        suppliers.filter { it.balance > 0 }.sumOf { it.balance }
    }
    val debtorCount = remember(suppliers) {
        suppliers.count { it.balance > 0 }
    }

    // Filtered list based on search and selected tab
    val filteredList = suppliers.filter { supp ->
        val matchesSearch = supp.name.contains(searchQuery, ignoreCase = true) ||
                (supp.phone?.contains(searchQuery, ignoreCase = true) == true) ||
                (supp.address?.contains(searchQuery, ignoreCase = true) == true) ||
                (supp.notes?.contains(searchQuery, ignoreCase = true) == true)

        val matchesTab = when (selectedTab) {
            1 -> supp.balance > 0
            2 -> supp.balance < 0
            3 -> supp.balance == 0.0
            else -> true
        }
        matchesSearch && matchesTab
    }.sortedByDescending { it.balance }

    // Main Gradient Background matching Dashboard / Expenses / Customers
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
                .height(320.dp)
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
            // Top Header
            SuppliersHeader(
                onBack = { navController.popBackStack() }
            )

            // KPI Stats Summary Banner
            SuppliersSummaryHeader(
                totalSuppliers = suppliers.size,
                totalDebts = totalSupplierDebts,
                debtorsCount = debtorCount
            )

            // Category Filter Tabs Row
            SuppliersTabRow(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            // Search Bar & Filter Options
            SuppliersSearchBar(
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it }
            )

            // Suppliers List Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocalShipping,
                                contentDescription = null,
                                tint = Color(0xFFA5ABC7),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = if (searchQuery.isBlank()) "لا يوجد موردين حالياً" else "لا توجد نتائج مطابقة للبحث",
                                color = Color(0xFFA5ABC7),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(filteredList, key = { it.id }) { supplier ->
                            SupplierItemCard(
                                supplier = supplier,
                                onClick = {
                                    navController.navigate("supplier_detail/${supplier.id}")
                                },
                                onPaySupplier = {
                                    showPaymentDialogForSupplier = supplier
                                },
                                onEdit = {
                                    supplierToEdit = supplier
                                    showAddEditSheet = true
                                },
                                onDelete = {
                                    viewModel.archiveSupplier(supplier.id) {
                                        Toast.makeText(context, "تم حذف المورد بنجاح", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }

                // Gradient Floating Action Button (+) positioned at Bottom Start
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 20.dp, bottom = 20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .shadow(12.dp, CircleShape, spotColor = Color(0xFFC084FC))
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFFE879F9),
                                        Color(0xFFC084FC),
                                        Color(0xFF818CF8),
                                        Color(0xFF38BDF8)
                                    )
                                )
                            )
                            .clickable {
                                supplierToEdit = null
                                showAddEditSheet = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddBusiness,
                            contentDescription = "إضافة مورد",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // Add / Edit Supplier Bottom Sheet Modal
        if (showAddEditSheet) {
            AddSupplierBottomSheet(
                supplier = supplierToEdit,
                onDismiss = { showAddEditSheet = false },
                onSave = { name, phone, address, notes ->
                    if (supplierToEdit == null) {
                        viewModel.addSupplier(
                            name = name,
                            phone = phone,
                            address = address,
                            notes = notes,
                            onSuccess = {
                                showAddEditSheet = false
                                Toast.makeText(context, "تمت إضافة المورد بنجاح", Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else {
                        viewModel.updateSupplier(
                            supplierToEdit!!.copy(
                                name = name,
                                phone = phone.takeIf { it.isNotBlank() },
                                address = address.takeIf { it.isNotBlank() },
                                notes = notes.takeIf { it.isNotBlank() }
                            ),
                            onSuccess = {
                                showAddEditSheet = false
                                Toast.makeText(context, "تم تحديث بيانات المورد بنجاح", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            )
        }

        // Quick Payment Dialog
        showPaymentDialogForSupplier?.let { supplier ->
            QuickSupplierPaymentDialog(
                supplier = supplier,
                onDismiss = { showPaymentDialogForSupplier = null },
                onConfirm = { amount, method, reference, note ->
                    viewModel.addSupplierPayment(
                        supplierId = supplier.id,
                        amount = amount,
                        method = method,
                        reference = reference,
                        note = note,
                        onSuccess = {
                            showPaymentDialogForSupplier = null
                            Toast.makeText(context, "تم تسديد الدفعة بنجاح", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            )
        }
    }
}

@Composable
fun SuppliersHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Button in circular translucent container
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0x28FFFFFF))
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.ChevronLeft,
                contentDescription = "رجوع",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        // Title
        Text(
            text = "إدارة الموردين",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        // LocalShipping Icon Badge with Gradient
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6), Color(0xFFEC4899))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.LocalShipping,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun SuppliersSummaryHeader(
    totalSuppliers: Int,
    totalDebts: Double,
    debtorsCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Debts Total KPI Card
        Card(
            modifier = Modifier
                .weight(1.3f)
                .clip(RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0x25FFFFFF))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x30EF4444)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ReceiptLong,
                        contentDescription = null,
                        tint = Color(0xFFFCA5A5),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "إجمالي مستحقات الموردين",
                        color = Color(0xFFA5ABC7),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${totalDebts.formatCurrency().replace("ر.س", "").trim()} ريال",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Supplier Count KPI Card
        Card(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0x25FFFFFF))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x303B82F6)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Store,
                        contentDescription = null,
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "عدد الموردين",
                        color = Color(0xFFA5ABC7),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$totalSuppliers مورد",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SuppliersTabRow(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf("الكل", "أنا مدين له", "له حساب دائن", "متزن")

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTab == index
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(index) }
                        .padding(vertical = 6.dp)
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) Color.White else Color(0xFFA5ABC7),
                        fontSize = if (isSelected) 15.sp else 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .width(64.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF60A5FA), Color(0xFF8B5CF6), Color(0xFFEC4899))
                                    )
                                )
                        )
                    } else {
                        Spacer(modifier = Modifier.height(3.dp))
                    }
                }
            }
        }
        HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 1.dp)
    }
}

@Composable
fun SuppliersSearchBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Filter Icon Container Button
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x28FFFFFF))
                .clickable { /* Filter Options */ },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = "تصفية",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // Custom Styled Search Text Field
        TextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .clip(RoundedCornerShape(16.dp)),
            placeholder = {
                Text(
                    "بحث باسم المورد، الهاتف، العنوان...",
                    color = Color(0xFF9096BD),
                    fontSize = 13.sp
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "بحث",
                    tint = Color(0xFF9096BD)
                )
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0x20FFFFFF),
                unfocusedContainerColor = Color(0x20FFFFFF),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
    }
}

@Composable
fun SupplierItemCard(
    supplier: Supplier,
    onClick: () -> Unit,
    onPaySupplier: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val balance = supplier.balance
    val hasPhone = !supplier.phone.isNullOrBlank()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEBF0FE)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Right Side (RTL start): Supplier Avatar + Info
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.weight(1f)
                ) {
                    // Avatar Icon Container
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (balance > 0) Color(0xFFFFEAEA) else if (balance < 0) Color(0xFFE8F5E9) else Color(0xFFDCE2FF)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Store,
                            contentDescription = null,
                            tint = if (balance > 0) Color(0xFFD32F2F) else if (balance < 0) Color(0xFF2E7D32) else Color(0xFF3B82F6),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column {
                        // Supplier Name
                        Text(
                            text = supplier.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF1B193B)
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        // Phone Number with Call Quick Icon
                        if (hasPhone) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.clickable {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${supplier.phone}"))
                                    context.startActivity(intent)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Phone,
                                    contentDescription = "اتصال",
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = supplier.phone!!,
                                    fontSize = 13.sp,
                                    color = Color(0xFF2563EB),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Address if present
                        if (!supplier.address.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFF6B7280),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = supplier.address!!,
                                    fontSize = 12.sp,
                                    color = Color(0xFF6B7280)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Balance Chip Status Badge
                        val chipBg: Color
                        val chipFg: Color
                        val balanceLabel: String

                        if (balance > 0) {
                            chipBg = Color(0xFFFFEBEE)
                            chipFg = Color(0xFFD32F2F)
                            balanceLabel = "• أنا مدين له: ${balance.formatCurrency().replace("ر.س", "").trim()} ريال"
                        } else if (balance < 0) {
                            chipBg = Color(0xFFE8F5E9)
                            chipFg = Color(0xFF2E7D32)
                            balanceLabel = "• له حساب دائن: ${(-balance).formatCurrency().replace("ر.س", "").trim()} ريال"
                        } else {
                            chipBg = Color(0xFFDBEAFE)
                            chipFg = Color(0xFF1E40AF)
                            balanceLabel = "• الرصيد متزن: 0 ريال"
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(chipBg)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = balanceLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = chipFg
                            )
                        }
                    }
                }

                // Left Side (RTL end): Quick Actions & Options Menu
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.height(90.dp)
                ) {
                    // Options Menu Button
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "خيارات",
                                tint = Color(0xFF6B7280)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("عرض كشف الحساب والمعاملات") },
                                onClick = { showMenu = false; onClick() },
                                leadingIcon = {
                                    Icon(Icons.Outlined.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("تسديد دفعة مالية") },
                                onClick = { showMenu = false; onPaySupplier() },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("تعديل البيانات") },
                                onClick = { showMenu = false; onEdit() },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("حذف المورد", color = Color.Red) },
                                onClick = { showMenu = false; onDelete() },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp))
                                }
                            )
                        }
                    }

                    // Pay Supplier Quick Button
                    Button(
                        onClick = { onPaySupplier() },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2563EB),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Payments,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "تسديد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddSupplierBottomSheet(
    supplier: Supplier?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(supplier?.name ?: "") }
    var phone by remember { mutableStateOf(supplier?.phone ?: "") }
    var address by remember { mutableStateOf(supplier?.address ?: "") }
    var notes by remember { mutableStateOf(supplier?.notes ?: "") }
    var isProcessing by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { if (!isProcessing) onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color(0xFFEFF2FE))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* prevent dismissal */ }
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Drag Handle
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFC2C7E2))
                    .align(Alignment.CenterHorizontally)
            )

            // Header Row: Close Button & Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Close Circle Button 'X'
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDEE2F8))
                        .clickable { if (!isProcessing) onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "إغلاق",
                        tint = Color(0xFF1B193B),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Title with Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (supplier == null) "إضافة مورد جديد" else "تعديل بيانات المورد",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B193B)
                    )
                    Icon(
                        imageVector = Icons.Outlined.AddBusiness,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Supplier Name Field (Required)
            CustomInputField(
                value = name,
                onValueChange = { name = it },
                placeholder = "اسم المورد / الشركة (مطلوب)",
                icon = Icons.Outlined.Store
            )

            // Phone Field
            CustomInputField(
                value = phone,
                onValueChange = { phone = it },
                placeholder = "رقم الهاتف",
                icon = Icons.Outlined.Phone,
                keyboardType = KeyboardType.Phone
            )

            // Address Field
            CustomInputField(
                value = address,
                onValueChange = { address = it },
                placeholder = "العنوان / المدينة / المقر الرئيسي",
                icon = Icons.Outlined.LocationOn
            )

            // Notes Field
            CustomInputField(
                value = notes,
                onValueChange = { notes = it },
                placeholder = "ملاحظات إضافية (اختياري)",
                icon = Icons.Outlined.Description
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons Row (Save & Cancel)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Save Button (Gradient)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF3B82F6), Color(0xFF6366F1), Color(0xFF8B5CF6))
                            )
                        )
                        .clickable(enabled = !isProcessing) {
                            if (name.isBlank()) {
                                Toast.makeText(context, "الرجاء إدخال اسم المورد", Toast.LENGTH_SHORT).show()
                            } else {
                                isProcessing = true
                                onSave(name.trim(), phone.trim(), address.trim(), notes.trim())
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "حفظ",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Cancel Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E6FA), RoundedCornerShape(16.dp))
                        .clickable(enabled = !isProcessing) { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "إلغاء",
                        color = Color(0xFF1B193B),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun QuickSupplierPaymentDialog(
    supplier: Supplier,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, String?, String?) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("CASH") }
    var reference by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Payments,
                    contentDescription = null,
                    tint = Color(0xFF2563EB)
                )
                Text("تسديد دفعة - ${supplier.name}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (supplier.balance > 0) {
                    Text(
                        text = "المبلغ المطلوب تسديده للمورد: ${supplier.balance.formatCurrency()}",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("المبلغ المسدد (ريال)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("طريقة الدفع:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF3F4F6))
                            .clickable { paymentMethod = "CASH" }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        RadioButton(selected = paymentMethod == "CASH", onClick = { paymentMethod = "CASH" })
                        Text("نقدي", fontSize = 13.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF3F4F6))
                            .clickable { paymentMethod = "BANK" }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        RadioButton(selected = paymentMethod == "BANK", onClick = { paymentMethod = "BANK" })
                        Text("تحويل بنكي", fontSize = 13.sp)
                    }
                }

                if (paymentMethod == "BANK") {
                    OutlinedTextField(
                        value = reference,
                        onValueChange = { reference = it },
                        label = { Text("رقم الحوالة / المرجع") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("ملاحظات / البيان") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull()
                    if (amt != null && amt > 0) {
                        onConfirm(amt, paymentMethod, reference.takeIf { it.isNotBlank() }, note.takeIf { it.isNotBlank() })
                    } else {
                        Toast.makeText(context, "الرجاء إدخال مبلغ صحيح", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text("تأكيد التسديد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
