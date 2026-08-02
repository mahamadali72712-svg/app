import os

content = """package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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

val CosmicBgBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF090616), // Deep Dark Purple/Black
        Color(0xFF1B0C3B), // Dark Purple
        Color(0xFF4A1E6D), // Purple
        Color(0xFFF9EAF3), // Light Pink transition
        Color(0xFFFDFDFD)  // White bottom
    ),
    startY = 0f,
    endY = 1800f
)

val GlowPurple = Color(0xFF9D4EDD)
val GlowPink = Color(0xFFF15BB5)
val GlowBlue = Color(0xFF00F2FE)
val TextGray = Color(0xFFA0A0B0)

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
    
    var selectedTab by remember { mutableStateOf(0) }
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

    Box(modifier = Modifier.fillMaxSize().background(CosmicBgBrush)) {
        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                if (selectedTab == 0) {
                    FloatingActionButton(
                        onClick = { 
                            productToEdit = null
                            showAddDialog = true 
                        },
                        containerColor = Color.Transparent,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp),
                        modifier = Modifier
                            .padding(16.dp)
                            .size(64.dp)
                            .background(
                                brush = Brush.linearGradient(listOf(GlowPurple, GlowPink)),
                                shape = CircleShape
                            )
                            .shadow(8.dp, CircleShape, spotColor = GlowPink)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "إضافة منتج", modifier = Modifier.size(32.dp), tint = Color.White)
                    }
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                
                // Top Bar
                LuxuryTopBar(onBackClick = { navController.popBackStack() })
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Tabs
                LuxuryTabs(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Search Bar
                LuxurySearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Categories
                LuxuryCategories(
                    categories = categories,
                    selectedCategoryId = selectedCategoryId,
                    onCategorySelected = { selectedCategoryId = it }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Products List
                if (filteredList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("لا توجد منتجات مطابقة للبحث.", color = Color.Gray, fontSize = 16.sp)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        items(filteredList, key = { it.id }) { product ->
                            val catName = categoriesMap[product.categoryId]?.name ?: "عام"
                            LuxuryProductCard(
                                product = product,
                                categoryName = catName,
                                onDetails = {
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
fun LuxuryTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Button
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
                .clickable { onBackClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Title Column
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "المنتجات",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = "تصفح جميع المنتجات المتاحة",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Bag Icon
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(GlowPurple, GlowPink)))
                .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = GlowPink),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.ShoppingBag, contentDescription = "السلة", tint = Color.White, modifier = Modifier.size(28.dp))
            Icon(Icons.Default.Star, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(12.dp).align(Alignment.TopEnd).padding(3.dp))
        }
    }
}

@Composable
fun LuxuryTabs(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LuxuryTabItem(
            text = "المنتجات النشطة",
            icon = Icons.Outlined.Inventory2,
            isSelected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            modifier = Modifier.weight(1f)
        )
        LuxuryTabItem(
            text = "المنتجات المؤرشفة",
            icon = Icons.Outlined.Archive,
            isSelected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun LuxuryTabItem(text: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = if (isSelected) Color.White else TextGray, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = if (isSelected) Color.White else TextGray,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 15.sp
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        // Glowing Underline
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(4.dp)
                .clip(CircleShape)
                .background(if (isSelected) Brush.horizontalGradient(listOf(GlowBlue, GlowPurple, GlowPink)) else Color.Transparent)
        )
    }
}

@Composable
fun LuxurySearchBar(query: String, onQueryChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(60.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(Color.White)
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Filter Button
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(GlowPurple, GlowPink))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Tune, contentDescription = "تصفية", tint = Color.White)
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("ابحث عن منتج...", color = Color.Gray, fontSize = 16.sp) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, color = Color.Black)
            )
            
            Icon(Icons.Outlined.Search, contentDescription = "بحث", tint = Color.Gray, modifier = Modifier.padding(end = 16.dp))
        }
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
        else -> Icons.Outlined.MoreHoriz
    }
}

fun getCategoryColor(name: String): Color {
    return when {
        name.contains("غرف") || name.contains("نوم") -> Color(0xFF1976D2) // Blue
        name.contains("طاول") -> Color(0xFFF57C00) // Orange
        name.contains("مرايا") -> Color(0xFF7B1FA2) // Purple
        name.contains("كتب") -> Color(0xFF388E3C) // Green
        name.contains("ساعات") -> Color(0xFFD32F2F) // Red
        name.contains("كرسي") -> Color(0xFF00796B) // Teal
        else -> Color(0xFF455A64) // Blue Grey
    }
}

@Composable
fun LuxuryCategories(
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
            LuxuryCategoryChip(
                text = "الكل",
                icon = Icons.Outlined.GridView,
                iconColor = Color.White,
                isSelected = selectedCategoryId == null,
                onClick = { onCategorySelected(null) }
            )
        }
        
        items(categories.distinctBy { it.name }) { category ->
            val icon = getCategoryIcon(category.name)
            val iconColor = getCategoryColor(category.name)
            LuxuryCategoryChip(
                text = category.name,
                icon = icon,
                iconColor = iconColor,
                isSelected = selectedCategoryId == category.id,
                onClick = { onCategorySelected(category.id) }
            )
        }
    }
}

@Composable
fun LuxuryCategoryChip(text: String, icon: ImageVector, iconColor: Color, isSelected: Boolean, onClick: () -> Unit) {
    val bgModifier = if (isSelected) {
        Modifier.background(Brush.linearGradient(listOf(Color(0xFF5324D6), Color(0xFF9D4EDD))))
    } else {
        Modifier.background(Color.White)
    }
    
    Column(
        modifier = Modifier
            .width(75.dp)
            .shadow(if (isSelected) 8.dp else 2.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .then(bgModifier)
            .clickable { onClick() }
            .padding(vertical = 16.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon, 
            contentDescription = null, 
            tint = if (isSelected) Color.White else iconColor,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = text,
            color = if (isSelected) Color.White else Color.Black,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun LuxuryProductCard(
    product: Product,
    categoryName: String,
    onDetails: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit
) {
    val cardBg = Color(0xFFFAFAFF) 
    
    // Gradients for the image box
    val imageBgColors = listOf(
        listOf(Color(0xFF23155B), Color(0xFF4A1F7A)), // Purple
        listOf(Color(0xFF0F3A4A), Color(0xFF1B6B7C)), // Teal
        listOf(Color(0xFF5A2A1A), Color(0xFF9E4B22))  // Warm/Orange
    )
    val colorIndex = product.id.hashCode().absoluteValue % imageBgColors.size
    val selectedGradient = imageBgColors[colorIndex]
    
    // Gradients for the main button
    val buttonGradients = listOf(
        listOf(Color(0xFF5B24D4), Color(0xFF9747FF)), // Purple
        listOf(Color(0xFF007A8A), Color(0xFF00C6FF)), // Teal
        listOf(Color(0xFFE65C00), Color(0xFFF9D423))  // Orange
    )
    val selectedButtonGradient = buttonGradients[colorIndex]

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Text Content (Right side in UI, left side in Row declaration for standard RTL)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                // Top Row: Title & Price
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = product.name,
                            color = Color(0xFF111122),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = product.description.takeIf { !it.isNullOrBlank() } ?: categoryName,
                            color = Color.Gray,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            text = product.suggestedPrice.formatCurrency().replace("ر.س", "").trim(),
                            color = Color(0xFF3F19A8),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "ريال",
                            color = Color(0xFF3F19A8),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Info badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stock badge
                    Row(
                        modifier = Modifier
                            .background(Color(0xFFEEF0F6), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("المخزون: ${product.stockQuantity.toInt()}", fontSize = 11.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
                    }
                    
                    // Alert badge
                    Row(
                        modifier = Modifier
                            .background(Color(0xFFFFEBEE), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تنبيه نقص: ${product.minStockAlert.toInt()}", fontSize = 11.sp, color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // View Details Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(selectedButtonGradient))
                        .clickable { onDetails() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("عرض تفاصيل المنتج", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Image Area (Left side in UI, Right side in Row declaration for standard RTL)
            Box(
                modifier = Modifier
                    .width(130.dp)
                    .height(160.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.radialGradient(selectedGradient, radius = 400f))
            ) {
                // Glow ring effect (simulated)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                )
                
                // Icon in center
                Icon(
                    imageVector = getCategoryIcon(categoryName),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.align(Alignment.Center).size(60.dp)
                )
                
                // Top Right (TopStart in RTL)
                val statusText = if (product.status == "ACTIVE") "نشط" else "مؤرشف"
                val statusColor = if (product.status == "ACTIVE") Color(0xFF00C853) else Color(0xFFD50000)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(statusColor))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(statusText, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                
                // Top Left (TopEnd in RTL)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { /* favorite */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.FavoriteBorder, contentDescription = null, tint = GlowPurple, modifier = Modifier.size(18.dp))
                }
                
                // Bottom Left (BottomEnd in RTL)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { if (product.status == "ACTIVE") onArchive() else onRestore() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (product.status == "ACTIVE") Icons.Outlined.VisibilityOff else Icons.Outlined.Restore, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (product.status == "ACTIVE") "إخفاء" else "استعادة", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
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
                        OutlinedTextField(value = stock, onValueChange = { stock = it }, label = { Text("الكمية") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = minStockAlert, onValueChange = { minStockAlert = it }, label = { Text("تنبيه النقص") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                }
                
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("سعر التكلفة") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("السعر المقترح") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
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

