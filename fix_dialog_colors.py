import os

with open("app/src/main/java/com/example/ui/screens/ProductsScreen.kt", "r") as f:
    content = f.read()

# Find the start of ProductFormDialog
start_idx = content.find("@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nfun ProductFormDialog(")

if start_idx != -1:
    old_code = content[start_idx:]
    
    new_code = """@OptIn(ExperimentalMaterial3Api::class)
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
"""
    content = content.replace(old_code, new_code)

    with open("app/src/main/java/com/example/ui/screens/ProductsScreen.kt", "w") as f:
        f.write(content)
else:
    print("Could not find start index")
