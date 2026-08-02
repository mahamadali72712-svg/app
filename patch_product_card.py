import re

with open('app/src/main/java/com/example/ui/screens/ProductsScreen.kt', 'r') as f:
    content = f.read()

# Add needed imports
imports_to_add = '''import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.Info
'''
if 'import androidx.compose.animation.animateContentSize' not in content:
    content = content.replace('import androidx.compose.foundation.background', imports_to_add + 'import androidx.compose.foundation.background')

card_old_pattern = r'@OptIn\(ExperimentalFoundationApi::class\)\s*@Composable\s*fun ProductCardEnhanced[\s\S]*?fun BadgeText'
card_new = '''@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ProductCardEnhanced(
    product: Product,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    
    val containerColor = if (product.stockQuantity == 0.0) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
            .combinedClickable(
                onClick = { expanded = !expanded },
                onLongClick = { showMenu = true }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(product.name, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "السعر: ${product.suggestedPrice.formatCurrency()}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "خيارات")
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
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stock Badge
                val stockColor = if (product.stockQuantity == 0.0) MaterialTheme.colorScheme.error
                                 else if (product.stockQuantity <= product.minStockAlert) Color(0xFFE65100)
                                 else MaterialTheme.colorScheme.secondary
                                 
                val stockText = if (product.stockQuantity == 0.0) "نفذت الكمية"
                                else "المخزون: ${product.stockQuantity.formatQty()}"
                
                BadgeText(text = stockText, containerColor = stockColor)
                
                Text(
                    text = if (expanded) "إخفاء التفاصيل" else "عرض التفاصيل",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailItem(label = "التكلفة", value = product.costPrice.formatCurrency())
                    DetailItem(label = "تنبيه النقص", value = product.minStockAlert.formatQty())
                    if (!product.color.isNullOrBlank()) {
                        DetailItem(label = "اللون", value = product.color)
                    }
                    if (!product.size.isNullOrBlank()) {
                        DetailItem(label = "المقاس", value = product.size)
                    }
                }
                
                if (!product.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("الوصف:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(product.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Column {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun BadgeText'''
content = re.sub(card_old_pattern, card_new, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screens/ProductsScreen.kt', 'w') as f:
    f.write(content)
