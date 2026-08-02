import os

content = """package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.Product
import com.example.data.local.ProductCategory
import com.example.ui.viewmodels.StoreViewModel
import com.example.utils.formatCurrency
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

val AppBgColor = Color(0xFFF8F9FA)
val PrimaryPurple = Color(0xFF5A45D1)
val TextDark = Color(0xFF1A1A24)
val TextGray = Color(0xFF9E9E9E)
val CardBg = Color(0xFFFFFFFF)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProductsScreen(viewModel: StoreViewModel, navController: androidx.navigation.NavController) {
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val archivedProducts by viewModel.archivedProducts.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(categories) {
        if (categories.isEmpty()) {
            viewModel.seedCategories()
        }
    }
    
    var selectedTab by remember { mutableStateOf(0) } // 0: Active, 1: Archived
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    
    var showAddDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    
    val currentList = if (selectedTab == 0) allProducts else archivedProducts
    val filteredList = currentList.filter { product ->
        val matchesSearch = product.name.contains(searchQuery, ignoreCase = true) || 
                             product.description?.contains(searchQuery, ignoreCase = true) == true
        val matchesCategory = selectedCategoryId == null || product.categoryId == selectedCategoryId
        matchesSearch && matchesCategory
    }
    val categoriesMap = categories.associateBy { it.id }

    Scaffold(
        containerColor = AppBgColor,
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { 
                        productToEdit = null
                        showAddDialog = true 
                    },
                    containerColor = PrimaryPurple,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.padding(16.dp).size(64.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "إضافة منتج", modifier = Modifier.size(32.dp))
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Start // Bottom left in RTL
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            // Custom Top Bar
            CustomTopBar(onBackClick = { navController.popBackStack() })
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Custom Tabs
            CustomTabs(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Search Bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Categories
            CategoriesFilter(
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                onCategorySelected = { selectedCategoryId = it }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Products List
            if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا توجد منتجات مطابقة للبحث.", color = TextGray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredList, key = { it.id }) { product ->
                        val catName = categoriesMap[product.categoryId]?.name ?: "عام"
                        ProductCardLuxury(
                            product = product,
                            categoryName = catName,
                            onEdit = {
                                productToEdit = product
                                showAddDialog = true
                            },
                            onArchive = {
                                scope.launch {
                                    val canDelete = viewModel.canDeleteProduct(product.id)
                                    if (canDelete) {
                                        viewModel.deleteProduct(product.id)
                                        Toast.makeText(context, "تم حذف المنتج نهائياً", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.archiveProduct(product.id)
                                        Toast.makeText(context, "لا يمكن حذف منتج مستخدم، تم أرشفته", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            onRestore = {
                                viewModel.restoreProduct(product.id)
                                Toast.makeText(context, "تمت الاستعادة", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
    
    if (showAddDialog) {
        ProductFormDialog(
            product = productToEdit,
            categories = categories,
            onDismiss = { showAddDialog = false },
            onSave = { name, cost, price, stock, categoryId, minStockAlert, desc, color, size ->
                if (productToEdit == null) {
                    viewModel.addProduct(
                        name = name,
                        cost = cost,
                        suggestedPrice = price,
                        stock = stock,
                        categoryId = categoryId,
                        minStockAlert = minStockAlert,
                        description = desc,
                        color = color,
                        size = size,
                        onSuccess = {
                            showAddDialog = false
                            Toast.makeText(context, "تمت إضافة المنتج بنجاح", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    val updated = productToEdit!!.copy(
                        name = name,
                        costPrice = cost,
                        suggestedPrice = price,
                        stockQuantity = stock,
                        categoryId = categoryId,
                        minStockAlert = minStockAlert,
                        description = desc,
                        color = color,
                        size = size
                    )
                    viewModel.updateProduct(
                        product = updated,
                        onSuccess = {
                            showAddDialog = false
                            Toast.makeText(context, "تم تحديث المنتج", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        )
    }
}

@Composable
fun CustomTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Button
        Box(
            modifier = Modifier
                .size(48.dp)
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .clickable { onBackClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = TextDark)
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Title Column
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "المنتجات",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Text(
                text = "تصفح جميع المنتجات المتاحة",
                fontSize = 12.sp,
                color = TextGray
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Bag Icon with Badge
        Box(
            modifier = Modifier
                .size(48.dp)
                .shadow(elevation = 4.dp, shape = RoundedCornerShape(14.dp), spotColor = PrimaryPurple)
                .clip(RoundedCornerShape(14.dp))
                .background(PrimaryPurple),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.ShoppingBag, contentDescription = "السلة", tint = Color.White)
            
            // Badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-6).dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF7E69FF))
                    .border(2.dp, AppBgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("3", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CustomTabs(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(Color.Transparent),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        TabItem(
            text = "المنتجات النشطة",
            icon = Icons.Outlined.Inventory2,
            isSelected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            modifier = Modifier.weight(1f)
        )
        TabItem(
            text = "المنتجات المؤرشفة",
            icon = Icons.Outlined.Archive,
            isSelected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            modifier = Modifier.weight(1f)
        )
    }
    // Bottom border for tabs
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(Color(0xFFE0E0E0))
    )
}

@Composable
fun TabItem(text: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = if (isSelected) PrimaryPurple else TextGray
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = color,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        // Indicator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                .background(if (isSelected) PrimaryPurple else Color.Transparent)
        )
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Tune, contentDescription = "تصفية", tint = TextDark)
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(24.dp)
                .background(Color(0xFFE0E0E0))
        )
        Spacer(modifier = Modifier.width(8.dp))
        
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("ابحث عن منتج...", color = TextGray, fontSize = 14.sp) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = TextDark)
        )
        
        Icon(Icons.Outlined.Search, contentDescription = "بحث", tint = TextGray)
    }
}

@Composable
fun CategoriesFilter(
    categories: List<ProductCategory>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            CategoryChip(
                text = "الكل",
                icon = Icons.Outlined.GridView,
                isSelected = selectedCategoryId == null,
                onClick = { onCategorySelected(null) }
            )
        }
        
        items(categories.distinctBy { it.name }) { category ->
            val icon = getCategoryIcon(category.name)
            CategoryChip(
                text = category.name,
                icon = icon,
                isSelected = selectedCategoryId == category.id,
                onClick = { onCategorySelected(category.id) }
            )
        }
    }
}

@Composable
fun CategoryChip(text: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) PrimaryPurple else Color.White
    val contentColor = if (isSelected) Color.White else TextDark
    val shadow = if (isSelected) 4.dp else 1.dp
    
    Column(
        modifier = Modifier
            .width(80.dp)
            .shadow(elevation = shadow, shape = RoundedCornerShape(16.dp), spotColor = if (isSelected) PrimaryPurple else Color.Gray)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = text,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

fun getCategoryIcon(name: String): ImageVector {
    return when {
        name.contains("غرف") || name.contains("نوم") -> Icons.Outlined.Bed
        name.contains("طاول") -> Icons.Outlined.TableBar
        name.contains("مرايا") || name.contains("ديكور") || name.contains("زينة") -> Icons.Outlined.Checkroom
        name.contains("كتب") || name.contains("مكتبة") -> Icons.Outlined.MenuBook
        name.contains("ساعات") || name.contains("ساعة") -> Icons.Outlined.Watch
        name.contains("كرسي") || name.contains("جلوس") -> Icons.Outlined.Chair
        else -> Icons.Outlined.Category
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductCardLuxury(
    product: Product,
    categoryName: String,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    val bgColors = listOf(
        Color(0xFFEAE7FF), // Light purple
        Color(0xFFE0F4EE), // Light green
        Color(0xFFFFEFE0), // Light orange
        Color(0xFFE3F2FD), // Light blue
        Color(0xFFFCE4EC)  // Light pink
    )
    val colorIndex = product.id.hashCode().absoluteValue % bgColors.size
    val imageBgColor = bgColors[colorIndex]

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { /* open details */ },
                onLongClick = { showMenu = true }
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Text Content (Weight 1f) -> In RTL this is on the RIGHT
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                // Top Row: Status badge and Price
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status Badge (Rightmost in this sub-row)
                    val statusBg = if (product.status == "ACTIVE") Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    val statusColor = if (product.status == "ACTIVE") Color(0xFF2E7D32) else Color(0xFFC62828)
                    val statusText = if (product.status == "ACTIVE") "نشط" else "مؤرشف"
                    
                    Row(
                        modifier = Modifier
                            .background(statusBg, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(6.dp).background(statusColor, CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(statusText, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    // Price (Leftmost in this sub-row)
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = product.suggestedPrice.formatCurrency().replace("ر.س", "").trim(),
                            color = PrimaryPurple,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ريال",
                            color = PrimaryPurple,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Title and Subtitle
                Text(
                    text = product.name,
                    color = TextDark,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = product.description.takeIf { !it.isNullOrBlank() } ?: categoryName,
                    color = TextGray,
                    fontSize = 12.sp,
                    maxLines = 1
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Stock and Code
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("المخزون: ${product.stockQuantity.toInt()}", fontSize = 11.sp, color = TextGray)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Tag, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("رقم المنتج: ${product.id.take(4).uppercase()}", fontSize = 11.sp, color = TextGray)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Actions: 3-dots and Add to cart
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(36.dp).border(1.dp, Color(0xFFEEEEEE), CircleShape)
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "خيارات", tint = TextDark, modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (product.status == "ACTIVE") {
                                DropdownMenuItem(
                                    text = { Text("تعديل") },
                                    onClick = {
                                        showMenu = false
                                        onEdit()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("أرشفة / حذف") },
                                    onClick = {
                                        showMenu = false
                                        onArchive()
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("استعادة") },
                                    onClick = {
                                        showMenu = false
                                        onRestore()
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = { /* Add to cart - implement later */ },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Text("إضافة للسلة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Outlined.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            
            // Image Section (Last child -> In RTL this is on the LEFT)
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(imageBgColor)
            ) {
                // Heart icon top-end
                IconButton(
                    onClick = { /* toggle favorite */ },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(28.dp)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(Icons.Outlined.FavoriteBorder, contentDescription = "مفضلة", tint = PrimaryPurple, modifier = Modifier.size(16.dp))
                }
                
                // Category icon in center
                Icon(
                    imageVector = getCategoryIcon(categoryName),
                    contentDescription = null,
                    tint = PrimaryPurple.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.Center).size(56.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormDialog(
    product: Product?,
    categories: List<ProductCategory>,
    onDismiss: () -> Unit,
    onSave: (String, Double, Double, Double, String?, Double, String?, String?, String?) -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var cost by remember { mutableStateOf(product?.costPrice?.toString() ?: "") }
    var price by remember { mutableStateOf(product?.suggestedPrice?.toString() ?: "") }
    var stock by remember { mutableStateOf(product?.stockQuantity?.toString() ?: "") }
    var categoryId by remember { mutableStateOf(product?.categoryId) }
    
    var minStockAlert by remember { mutableStateOf(product?.minStockAlert?.toString() ?: "0") }
    var description by remember { mutableStateOf(product?.description ?: "") }
    var color by remember { mutableStateOf(product?.color ?: "") }
    var size by remember { mutableStateOf(product?.size ?: "") }
    
    var showMore by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        title = { Text(if (product == null) "منتج جديد" else "تعديل المنتج") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم المنتج") }, modifier = Modifier.fillMaxWidth())
                }
                
                if (categories.isNotEmpty()) {
                    item {
                        var expanded by remember { mutableStateOf(false) }
                        val selectedCategory = categories.find { it.id == categoryId }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedCategory?.name ?: "اختر الفئة",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("الفئة") },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("بدون فئة") },
                                    onClick = { categoryId = null; expanded = false }
                                )
                                categories.distinctBy { it.name }.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.name) },
                                        onClick = { categoryId = cat.id; expanded = false }
                                    )
                                }
                            }
                        }
                    }
                }
                
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = stock, onValueChange = { stock = it }, label = { Text("الكمية") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = minStockAlert, onValueChange = { minStockAlert = it }, label = { Text("تنبيه النقص") }, modifier = Modifier.weight(1f))
                    }
                }
                
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("سعر التكلفة") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("السعر المقترح") }, modifier = Modifier.weight(1f))
                    }
                }
                
                item {
                    TextButton(onClick = { showMore = !showMore }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (showMore) "إخفاء التفاصيل الإضافية" else "عرض التفاصيل الإضافية")
                        Icon(if (showMore) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = null)
                    }
                }
                
                if (showMore) {
                    item {
                        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("الوصف") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = color, onValueChange = { color = it }, label = { Text("اللون") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = size, onValueChange = { size = it }, label = { Text("المقاس") }, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isProcessing,
                onClick = {
                    if (name.isBlank()) {
                        Toast.makeText(context, "الرجاء إدخال اسم المنتج", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isProcessing = true
                    onSave(
                        name,
                        cost.toDoubleOrNull() ?: 0.0,
                        price.toDoubleOrNull() ?: 0.0,
                        stock.toDoubleOrNull() ?: 0.0,
                        categoryId,
                        minStockAlert.toDoubleOrNull() ?: 0.0,
                        description.takeIf { it.isNotBlank() },
                        color.takeIf { it.isNotBlank() },
                        size.takeIf { it.isNotBlank() }
                    )
                }
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("حفظ")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isProcessing) { Text("إلغاء") }
        }
    )
}
"""

with open("app/src/main/java/com/example/ui/screens/ProductsScreen.kt", "w") as f:
    f.write(content)

