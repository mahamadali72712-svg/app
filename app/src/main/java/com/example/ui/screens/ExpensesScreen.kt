package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.ReceiptLong
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.data.local.Expense
import com.example.ui.navigation.Screen
import com.example.ui.viewmodels.StoreViewModel
import com.example.utils.formatCurrency
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExpensesScreen(viewModel: StoreViewModel, navController: NavController) {
    val expenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAddSheet by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(1) } // 0: الكل, 1: تشغيلي (Default active as in screenshot), 2: شخصي

    val filteredList = expenses.filter { exp ->
        val matchesSearch = exp.categoryId.contains(searchQuery, ignoreCase = true) ||
                            exp.note?.contains(searchQuery, ignoreCase = true) == true
        val matchesTab = when (selectedTab) {
            1 -> exp.expenseType == "OPERATIONAL"
            2 -> exp.expenseType == "PERSONAL"
            else -> true
        }
        matchesSearch && matchesTab
    }

    // Main Gradient Background matching the screenshot
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
        // Ambient background glows
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(300.dp)
                .align(Alignment.TopStart)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x33A855F7), Color.Transparent),
                        radius = 600f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header Row
            ExpensesHeader(
                onBack = { navController.popBackStack() }
            )

            // Tabs Row
            ExpensesTabRow(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            // Search Bar & Filter Button
            ExpensesSearchBar(
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it }
            )

            // List of Expenses
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
                        Text(
                            text = "لا توجد مصروفات حالياً",
                            color = Color(0xFFA5ABC7),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(filteredList, key = { it.id }) { exp ->
                            ExpenseItemCard(
                                expense = exp,
                                onEdit = {
                                    expenseToEdit = exp
                                    showAddSheet = true
                                },
                                onDelete = {
                                    viewModel.archiveExpense(exp.id) {
                                        Toast.makeText(context, "تم حذف المصروف", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }

                // Floating Action Button (+) positioned on left side
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
                                expenseToEdit = null
                                showAddSheet = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "إضافة مصروف",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        // Add / Edit Expense Bottom Sheet Modal Overlay
        if (showAddSheet) {
            AddExpenseBottomSheet(
                expense = expenseToEdit,
                onDismiss = { showAddSheet = false },
                onSave = { type, cat, amt, nt ->
                    if (expenseToEdit == null) {
                        viewModel.addExpense(
                            categoryId = cat,
                            type = type,
                            amount = amt,
                            note = nt,
                            onSuccess = {
                                showAddSheet = false
                                Toast.makeText(context, "تمت إضافة المصروف بنجاح", Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else {
                        viewModel.updateExpense(
                            expenseToEdit!!.copy(
                                expenseType = type,
                                categoryId = cat,
                                amount = amt,
                                note = nt
                            ),
                            onSuccess = {
                                showAddSheet = false
                                Toast.makeText(context, "تم تحديث المصروف بنجاح", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun ExpensesHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Button in circular container
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
            text = "إدارة المصروفات",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        // Wallet Icon Badge with Gradient
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFC084FC), Color(0xFF8B5CF6), Color(0xFF6366F1))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.AccountBalanceWallet,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun ExpensesTabRow(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf("الكل", "تشغيلي", "شخصي")

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
                        fontSize = if (isSelected) 16.sp else 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFE086FE), Color(0xFFC084FC), Color(0xFF8B5CF6))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesSearchBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Filter Button
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x28FFFFFF))
                .clickable { /* Filter Action */ },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = "تصفية",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // Search Field
        TextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .clip(RoundedCornerShape(16.dp)),
            placeholder = {
                Text(
                    "بحث في المصروفات...",
                    color = Color(0xFF9096BD),
                    fontSize = 14.sp
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
fun ExpenseItemCard(
    expense: Expense,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.ENGLISH) }
    val dateFormatter = remember { SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH) }
    val date = Date(expense.expenseDate)
    val timeStr = timeFormatter.format(date)
    val dateStr = dateFormatter.format(date)

    val typeName = if (expense.expenseType == "OPERATIONAL") "تشغيلي" else "شخصي"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp)),
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
                // Right Side (RTL start): Wallet Icon + Content
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.weight(1f)
                ) {
                    // Soft Purple Wallet Icon Container
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFDCE2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountBalanceWallet,
                            contentDescription = null,
                            tint = Color(0xFF6B46C1),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        // Title / Category Name
                        Text(
                            text = expense.categoryId,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF1B193B)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        // Amount
                        Text(
                            text = "${expense.amount.formatCurrency().replace("ر.س", "").trim()} ريال",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color(0xFFE53935)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        // Type Badge Chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFFDDD8FC))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "• $typeName",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF673AB7)
                                )
                            }
                        }
                    }
                }

                // Left Side (RTL end): Options Menu & Timestamps
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.height(82.dp)
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
                                text = { Text("تعديل") },
                                onClick = { showMenu = false; onEdit() }
                            )
                            DropdownMenuItem(
                                text = { Text("حذف") },
                                onClick = { showMenu = false; onDelete() }
                            )
                        }
                    }

                    // Time and Date Info
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccessTime,
                                contentDescription = null,
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = timeStr,
                                fontSize = 11.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = null,
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = dateStr,
                                fontSize = 11.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddExpenseBottomSheet(
    expense: Expense?,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, String) -> Unit
) {
    var expenseType by remember { mutableStateOf(expense?.expenseType ?: "OPERATIONAL") }
    var category by remember { mutableStateOf(expense?.categoryId ?: "") }
    var amount by remember { mutableStateOf(expense?.amount?.let { if (it > 0) it.toString() else "" } ?: "") }
    var note by remember { mutableStateOf(expense?.note ?: "") }
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
                ) { /* prevent dismissal on clicking inside modal */ }
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        text = if (expense == null) "إضافة مصروف" else "تعديل مصروف",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B193B)
                    )
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = null,
                        tint = Color(0xFF7C4DFF),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Expense Type Label
            Text(
                text = "نوع المصروف:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B193B)
            )

            // Expense Type Radio Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // "تشغيلي" Radio Card
                val isOperational = expenseType == "OPERATIONAL"
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isOperational) Color(0xFFE4E7FB) else Color(0xFFF7F8FF))
                        .border(
                            width = 1.dp,
                            color = if (isOperational) Color(0xFFC084FC) else Color(0xFFE2E6FA),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { expenseType = "OPERATIONAL" }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = if (isOperational) Icons.Rounded.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isOperational) Color(0xFF7C4DFF) else Color(0xFF9CA3AF),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "تشغيلي",
                        fontSize = 14.sp,
                        fontWeight = if (isOperational) FontWeight.Bold else FontWeight.Medium,
                        color = Color(0xFF1B193B)
                    )
                }

                // "شخصي" Radio Card
                val isPersonal = expenseType == "PERSONAL"
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isPersonal) Color(0xFFE4E7FB) else Color(0xFFF7F8FF))
                        .border(
                            width = 1.dp,
                            color = if (isPersonal) Color(0xFFC084FC) else Color(0xFFE2E6FA),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { expenseType = "PERSONAL" }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = if (isPersonal) Icons.Rounded.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isPersonal) Color(0xFF7C4DFF) else Color(0xFF9CA3AF),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "شخصي",
                        fontSize = 14.sp,
                        fontWeight = if (isPersonal) FontWeight.Bold else FontWeight.Medium,
                        color = Color(0xFF1B193B)
                    )
                }
            }

            // Category Field "الصنف (مثال: إيجار)"
            CustomInputField(
                value = category,
                onValueChange = { category = it },
                placeholder = "الصنف (مثال: إيجار)",
                icon = Icons.Outlined.LocalOffer
            )

            // Amount Field "المبلغ"
            CustomInputField(
                value = amount,
                onValueChange = { amount = it },
                placeholder = "المبلغ",
                icon = Icons.Outlined.Payments,
                keyboardType = KeyboardType.Number
            )

            // Note Field "ملاحظات (اختياري)"
            CustomInputField(
                value = note,
                onValueChange = { note = it },
                placeholder = "ملاحظات (اختياري)",
                icon = Icons.Outlined.ReceiptLong
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
                                listOf(Color(0xFF8B5CF6), Color(0xFF6366F1), Color(0xFF3B82F6))
                            )
                        )
                        .clickable(enabled = !isProcessing) {
                            val amt = amount.toDoubleOrNull() ?: 0.0
                            if (amt <= 0 || category.isBlank()) {
                                Toast.makeText(context, "تأكد من إدخال التصنيف ومبلغ صحيح", Toast.LENGTH_SHORT).show()
                                return@clickable
                            }
                            isProcessing = true
                            onSave(expenseType, category, amt, note)
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

                // Cancel Button (White Card)
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
fun CustomInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFFE2E6FA), RoundedCornerShape(14.dp)),
        placeholder = {
            Text(
                placeholder,
                color = Color(0xFF9CA3AF),
                fontSize = 14.sp
            )
        },
        trailingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF6B7280),
                modifier = Modifier.size(20.dp)
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFF7F8FF),
            unfocusedContainerColor = Color(0xFFF7F8FF),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = Color(0xFF1B193B),
            unfocusedTextColor = Color(0xFF1B193B)
        )
    )
}

@Composable
fun ExpensesBottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White)
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val navItems = listOf(
                Triple(Screen.Dashboard.route, "الرئيسية", Icons.Rounded.Home),
                Triple(Screen.Expenses.route, "المصروفات", Icons.Rounded.FormatListBulleted),
                Triple(Screen.Sales.route, "المبيعات", Icons.Rounded.ShoppingCart),
                Triple(Screen.Reports.route, "التقارير", Icons.Rounded.CalendarToday),
                Triple(Screen.Settings.route, "الإعدادات", Icons.Rounded.Settings)
            )

            navItems.forEach { (route, title, icon) ->
                val isSelected = route == currentRoute

                if (isSelected) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))
                                )
                            )
                            .clickable { onNavigate(route) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .clickable { onNavigate(route) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = title,
                            color = Color(0xFF9CA3AF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
