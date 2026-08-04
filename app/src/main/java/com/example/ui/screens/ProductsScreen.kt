package com.example.ui.screens

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

private val GlowPurple = Color(0xFF9D4EDD)
private val GlowPink = Color(0xFFF15BB5)
private val GlowBlue = Color(0xFF00F2FE)
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
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showStoreDomainDialog by remember { mutableStateOf(false) }
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
                            .size(60.dp)
                            .background(
                                brush = Brush.linearGradient(listOf(GlowPurple, GlowPink)),
                                shape = CircleShape
                            )
                            .shadow(8.dp, CircleShape, spotColor = GlowPink)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "إضافة منتج", modifier = Modifier.size(30.dp), tint = Color.White)
                    }
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                
                // Top Bar
                LuxuryTopBar(
                    onBackClick = { navController.popBackStack() },
                    onStoreDomainClick = { showStoreDomainDialog = true }
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Search Bar (Lifted directly to the top area)
                LuxurySearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it }
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Flexible Medium-Sized Category Chips with Add Category Option
                LuxuryCategories(
                    categories = categories,
                    selectedCategoryId = selectedCategoryId,
                    onCategorySelected = { selectedCategoryId = it },
                    onAddCategoryClick = { showAddCategoryDialog = true }
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Compact Active / Archived Tabs
                LuxuryTabs(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Main Products List (Expanded Viewport)
                if (filteredList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("لا توجد منتجات مطابقة للبحث.", color = Color.Gray, fontSize = 15.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
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
    
    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onSave = { categoryName ->
                viewModel.addCategory(categoryName) {
                    showAddCategoryDialog = false
                    Toast.makeText(context, "تمت إضافة التصنيف بنجاح", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showStoreDomainDialog) {
        StoreDomainSelectionDialog(
            onDismiss = { showStoreDomainDialog = false },
            onApplyPreset = { domainTitle, sampleCategories ->
                viewModel.applyDomainCategories(sampleCategories) {
                    showStoreDomainDialog = false
                    Toast.makeText(context, "تم تطبيق تصنيفات نشاط: $domainTitle", Toast.LENGTH_SHORT).show()
                }
            }
        )
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
fun LuxuryTopBar(
    onBackClick: () -> Unit,
    onStoreDomainClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Store Domain Selector Icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(GlowPurple, GlowPink)))
                .shadow(6.dp, RoundedCornerShape(14.dp), spotColor = GlowPink)
                .clickable { onStoreDomainClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Storefront, contentDescription = "نوع النشاط التجاري", tint = Color.White, modifier = Modifier.size(22.dp))
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Store Domain Quick Action Chip
        Surface(
            onClick = onStoreDomainClick,
            shape = RoundedCornerShape(50),
            color = Color.White.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Outlined.Category, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                Text(
                    text = "نوع النشاط",
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        
        // Title Column
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "المنتجات",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Back Button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
                .clickable { onBackClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ArrowForward, contentDescription = "رجوع", tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

private data class StoreDomainPreset(
    val title: String,
    val icon: ImageVector,
    val description: String,
    val sampleCategories: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreDomainSelectionDialog(
    onDismiss: () -> Unit,
    onApplyPreset: (String, List<String>) -> Unit
) {
    val presets = remember {
        listOf(
            StoreDomainPreset(
                title = "ملابس وموضة",
                icon = Icons.Outlined.Checkroom,
                description = "قمصان، بنطال، فساتين، أحذية، إكسسوارات",
                sampleCategories = listOf("قمصان", "بنطال", "فساتين", "أحذية", "إكسسوارات")
            ),
            StoreDomainPreset(
                title = "إلكترونيات وهواتف",
                icon = Icons.Outlined.Smartphone,
                description = "هواتف، لابتوب، سماعات، شواحن، إكسسوارات",
                sampleCategories = listOf("هواتف", "لابتوب", "سماعات", "شواحن", "إلكترونيات")
            ),
            StoreDomainPreset(
                title = "سوبرماركت وأغذية",
                icon = Icons.Outlined.LocalGroceryStore,
                description = "مأكولات، مشروبات، حلويات، معلبات، منظفات",
                sampleCategories = listOf("مأكولات", "مشروبات", "حلويات", "معلبات", "منظفات")
            ),
            StoreDomainPreset(
                title = "عطور وتجميل",
                icon = Icons.Outlined.LocalFlorist,
                description = "عطور رجالية، نسائية، مكياج، عناية بالبشرة",
                sampleCategories = listOf("عطور رجالية", "عطور نسائية", "مكياج", "عناية بالبشرة")
            ),
            StoreDomainPreset(
                title = "أثاث وديكور",
                icon = Icons.Outlined.Bed,
                description = "غرف نوم، كنب، طاولات، ستائر، سجاد",
                sampleCategories = listOf("غرف نوم", "كنب", "طاولات", "ستائر", "سجاد")
            ),
            StoreDomainPreset(
                title = "متجر عام / نشاط مخصص",
                icon = Icons.Outlined.Storefront,
                description = "جميع أنواع المنتجات والبضائع المتنوعة",
                sampleCategories = listOf("عام", "بضائع متنوعة", "أخرى")
            )
        )
    }

    var selectedPreset by remember { mutableStateOf(presets.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(GlowPurple.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Storefront, contentDescription = null, tint = GlowPurple, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text("تخصيص نوع النشاط التجاري", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                    Text("لتخصيص الأيقونات والتصنيفات حسب متجرك", fontSize = 11.5.sp, color = Color.Gray)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presets) { preset ->
                        val isSelected = selectedPreset.title == preset.title
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPreset = preset },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) GlowPurple.copy(alpha = 0.12f) else Color(0xFFF8FAFC)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) GlowPurple else Color(0xFFE2E8F0)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) GlowPurple else Color(0xFFCBD5E1)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        preset.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else Color.DarkGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        preset.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        preset.description,
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = GlowPurple,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApplyPreset(selectedPreset.title, selectedPreset.sampleCategories)
                },
                colors = ButtonDefaults.buttonColors(containerColor = GlowPurple),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("تطبيق تصنيفات النشاط", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", fontSize = 12.5.sp)
            }
        }
    )
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
            .padding(bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = if (isSelected) Color.White else TextGray, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = if (isSelected) Color.White else TextGray,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        // Glowing Underline
        val bgMod = if (isSelected) Modifier.background(Brush.horizontalGradient(listOf(GlowBlue, GlowPurple, GlowPink))) else Modifier.background(Color.Transparent)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(3.dp)
                .clip(CircleShape)
                .then(bgMod)
        )
    }
}

@Composable
fun LuxurySearchBar(query: String, onQueryChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(Color.White)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Search, 
                contentDescription = "بحث", 
                tint = Color(0xFF8C93BD), 
                modifier = Modifier.padding(start = 12.dp, end = 6.dp).size(20.dp)
            )
            
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                singleLine = true,
                textStyle = TextStyle(fontSize = 14.sp, color = Color.Black),
                cursorBrush = SolidColor(GlowPurple),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text("ابحث عن اسم المنتج، الوصف...", color = Color(0xFF8C93BD), fontSize = 14.sp)
                        }
                        innerTextField()
                    }
                }
            )
            
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close, 
                        contentDescription = "مسح", 
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(GlowPurple, GlowPink))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Tune, contentDescription = "تصفية", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

fun getCategoryIcon(name: String): ImageVector {
    val clean = name.lowercase()
    return when {
        // Clothing & Fashion
        clean.contains("قميص") || clean.contains("قمصان") || clean.contains("ملابس") || clean.contains("ثوب") || clean.contains("فستان") || clean.contains("فساتين") || clean.contains("جاكيت") || clean.contains("عباية") || clean.contains("موضة") -> Icons.Outlined.Checkroom
        clean.contains("بنطال") || clean.contains("جينز") || clean.contains("بناطيل") -> Icons.Outlined.DryCleaning
        clean.contains("حذاء") || clean.contains("أحذية") || clean.contains("جزمة") || clean.contains("شنط") || clean.contains("حقائب") -> Icons.Outlined.ShoppingBag
        // Electronics
        clean.contains("هاتف") || clean.contains("جوال") || clean.contains("موبايل") || clean.contains("آيفون") || clean.contains("سامسونج") -> Icons.Outlined.Smartphone
        clean.contains("لابتوب") || clean.contains("كمبيوتر") || clean.contains("حاسوب") -> Icons.Outlined.Laptop
        clean.contains("سماعة") || clean.contains("سماعات") || clean.contains("صوت") -> Icons.Outlined.Headphones
        clean.contains("شاشة") || clean.contains("تلفزيون") -> Icons.Outlined.Tv
        clean.contains("إلكتروني") || clean.contains("أجهزة") || clean.contains("شواحن") || clean.contains("كابل") -> Icons.Outlined.Devices
        // Food & Supermarket
        clean.contains("أغذية") || clean.contains("طعام") || clean.contains("سوبرماركت") || clean.contains("بقالة") || clean.contains("معلبات") || clean.contains("مأكولات") -> Icons.Outlined.LocalGroceryStore
        clean.contains("مشروب") || clean.contains("عصير") || clean.contains("ماء") -> Icons.Outlined.Liquor
        clean.contains("حلويات") || clean.contains("حلوى") || clean.contains("كيك") -> Icons.Outlined.Fastfood
        clean.contains("منظف") || clean.contains("غسيل") -> Icons.Outlined.CleaningServices
        // Cosmetics & Perfumes
        clean.contains("عطر") || clean.contains("عطور") || clean.contains("بخور") -> Icons.Outlined.LocalFlorist
        clean.contains("تجميل") || clean.contains("مكياج") || clean.contains("بشرة") || clean.contains("عناية") -> Icons.Outlined.AutoAwesome
        // Furniture & Decor
        clean.contains("غرف") || clean.contains("نوم") || clean.contains("سرير") || clean.contains("مراتب") -> Icons.Outlined.Bed
        clean.contains("طاول") -> Icons.Outlined.TableBar
        clean.contains("كنب") || clean.contains("أريكة") || clean.contains("مجلس") -> Icons.Outlined.Weekend
        clean.contains("كرسي") || clean.contains("جلوس") -> Icons.Outlined.Chair
        clean.contains("مرايا") || clean.contains("ديكور") || clean.contains("زينة") || clean.contains("ستائر") || clean.contains("سجاد") -> Icons.Outlined.SingleBed
        clean.contains("كتب") || clean.contains("مكتبة") -> Icons.Outlined.MenuBook
        clean.contains("ساعات") || clean.contains("ساعة") -> Icons.Outlined.Watch
        else -> Icons.Outlined.Category
    }
}

fun getCategoryColor(name: String): Color {
    val clean = name.lowercase()
    return when {
        clean.contains("قميص") || clean.contains("ملابس") || clean.contains("فستان") -> Color(0xFF8B5CF6)
        clean.contains("بنطال") || clean.contains("حذاء") -> Color(0xFFEC4899)
        clean.contains("هاتف") || clean.contains("لابتوب") || clean.contains("إلكتروني") -> Color(0xFF3B82F6)
        clean.contains("أغذية") || clean.contains("سوبرماركت") || clean.contains("طعام") -> Color(0xFF10B981)
        clean.contains("عطر") || clean.contains("تجميل") -> Color(0xFFF59E0B)
        clean.contains("غرف") || clean.contains("نوم") || clean.contains("كنب") || clean.contains("أثاث") -> Color(0xFF6366F1)
        else -> Color(0xFF64748B)
    }
}

@Composable
fun LuxuryCategories(
    categories: List<ProductCategory>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
    onAddCategoryClick: () -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Add Category Button Chip
        item {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF7C3AED))))
                    .clickable { onAddCategoryClick() }
                    .padding(vertical = 7.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "إضافة تصنيف",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "إضافة تصنيف",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // All Chip
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
fun LuxuryCategoryChip(
    text: String, 
    icon: ImageVector, 
    iconColor: Color, 
    isSelected: Boolean, 
    onClick: () -> Unit
) {
    val bgModifier = if (isSelected) {
        Modifier.background(Brush.linearGradient(listOf(Color(0xFF5324D6), Color(0xFF9D4EDD))))
    } else {
        Modifier.background(Color(0x33FFFFFF))
    }
    
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .then(bgModifier)
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isSelected) Color.White.copy(alpha = 0.25f) else iconColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, 
                contentDescription = null, 
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val quickSuggestions = remember {
        listOf("قمصان", "بنطال", "فساتين", "أحذية", "هواتف", "لابتوب", "عطور", "أغذية", "طاولات", "أثاث", "عام")
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة تصنيف جديد", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("أدخل اسم التصنيف أو اختر من الاقتراحات السريعة:", fontSize = 13.sp, color = Color.Gray)
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(quickSuggestions) { suggestion ->
                        FilterChip(
                            selected = name == suggestion,
                            onClick = { name = suggestion },
                            label = { Text(suggestion, fontSize = 11.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GlowPurple,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم التصنيف") },
                    placeholder = { Text("مثال: قمصان، جوالات، عطور...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GlowPurple)
            ) {
                Text("إضافة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
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
        modifier = Modifier.fillMaxWidth().border(1.5.dp, Color(0xFFEBEBEB), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                            fontSize = 18.sp,
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
                            .padding(horizontal = 6.dp, vertical = 4.dp),
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
                            .padding(horizontal = 6.dp, vertical = 4.dp),
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
                        .height(36.dp)
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

    Dialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(32.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFFAFAFF))))
                .border(1.dp, Brush.linearGradient(listOf(GlowPurple, GlowPink)), RoundedCornerShape(32.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Text(
                    text = if (product == null) "إضافة منتج جديد" else "تعديل المنتج",
                    color = Color(0xFF1B0C3B),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Form Fields
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        LuxuryTextField(value = name, onValueChange = { name = it }, label = "اسم المنتج")
                    }
                    
                    if (categories.isNotEmpty()) {
                        item {
                            var expanded by remember { mutableStateOf(false) }
                            val selectedCategory = categories.find { it.id == categoryId }
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = it }
                            ) {
                                LuxuryTextField(
                                    value = selectedCategory?.name ?: "اختر الفئة",
                                    onValueChange = {},
                                    label = "الفئة",
                                    readOnly = true,
                                    modifier = Modifier.menuAnchor(),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded, 
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.background(Color.White)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("بدون فئة", color = Color.Black) },
                                        onClick = { categoryId = null; expanded = false }
                                    )
                                    categories.distinctBy { it.name }.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat.name, color = Color.Black) },
                                            onClick = { categoryId = cat.id; expanded = false }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            LuxuryTextField(value = stock, onValueChange = { stock = it }, label = "الكمية", modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            LuxuryTextField(value = minStockAlert, onValueChange = { minStockAlert = it }, label = "تنبيه النقص", modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        }
                    }
                    
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            LuxuryTextField(value = cost, onValueChange = { cost = it }, label = "سعر التكلفة", modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            LuxuryTextField(value = price, onValueChange = { price = it }, label = "السعر المقترح", modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        }
                    }
                    
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF3F0FA))
                                .clickable { showMore = !showMore }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (showMore) "إخفاء التفاصيل الإضافية" else "عرض التفاصيل الإضافية",
                                    color = GlowPurple,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    if (showMore) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, 
                                    contentDescription = null,
                                    tint = GlowPurple
                                )
                            }
                        }
                    }
                    
                    if (showMore) {
                        item {
                            LuxuryTextField(value = description, onValueChange = { description = it }, label = "الوصف", minLines = 2)
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                LuxuryTextField(value = color, onValueChange = { color = it }, label = "اللون", modifier = Modifier.weight(1f))
                                LuxuryTextField(value = size, onValueChange = { size = it }, label = "المقاس", modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                
                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Cancel Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(25.dp))
                            .background(Color(0xFFF5F5FA))
                            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(25.dp))
                            .clickable(enabled = !isProcessing) { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("إلغاء", color = Color.DarkGray, fontWeight = FontWeight.Bold)
                    }
                    
                    // Save Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(25.dp))
                            .background(Brush.linearGradient(listOf(GlowPurple, GlowPink)))
                            .clickable(enabled = !isProcessing) {
                                if (name.isBlank()) {
                                    Toast.makeText(context, "الرجاء إدخال اسم المنتج", Toast.LENGTH_SHORT).show()
                                    return@clickable
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
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Text("حفظ المنتج", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LuxuryTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    minLines: Int = 1,
    readOnly: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = label, color = Color.DarkGray, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFFAFAFF))
                .border(1.dp, Color(0xFFE5E5EA), RoundedCornerShape(16.dp))
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color(0xFF111122),
                    unfocusedTextColor = Color(0xFF111122),
                    cursorColor = GlowPink
                ),
                keyboardOptions = keyboardOptions,
                minLines = minLines,
                readOnly = readOnly,
                trailingIcon = trailingIcon
            )
        }
    }
}

@Composable
fun BadgeText(text: String, containerColor: Color) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
