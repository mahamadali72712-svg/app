with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
    f.write("""package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.ui.navigation.Screen
import com.example.ui.viewmodels.StoreViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import com.example.utils.formatCurrency
import androidx.compose.foundation.clickable

@Composable
fun DashboardScreen(viewModel: StoreViewModel, navController: NavController) {
    val totalSales by viewModel.totalSales.collectAsStateWithLifecycle()
    val totalProfit by viewModel.totalProfit.collectAsStateWithLifecycle()
    val cashBalance by viewModel.cashBalance.collectAsStateWithLifecycle()
    val operationalExpenses by viewModel.operationalExpenses.collectAsStateWithLifecycle()
    val totalSupplierDebts by viewModel.totalSupplierDebts.collectAsStateWithLifecycle()
    val totalCustomerDebts by viewModel.totalCustomerDebts.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item { TopBar() }
        
        item {
            SectionTitle(
                title = "ملخص اليوم",
                icon = Icons.Rounded.SsidChart,
                textColor = Color.White
            )
        }
        
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    SummaryCard(
                        title = "المبيعات",
                        amount = totalSales.formatCurrency().replace("ر.س", "").trim(),
                        currency = "ريال",
                        icon = Icons.Rounded.ShoppingCart,
                        iconBgColor = Color(0xFF1763E4),
                        iconColor = Color.White,
                        borderColor = Color(0xFF1851CB),
                        bgGradient = Brush.linearGradient(listOf(Color(0xFF0D1943), Color(0xFF0B2556))),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(12.dp))
                    SummaryCard(
                        title = "المصروفات",
                        amount = operationalExpenses.formatCurrency().replace("ر.س", "").trim(),
                        currency = "ريال",
                        icon = Icons.Rounded.AccountBalanceWallet,
                        iconBgColor = Color(0xFFD7283E),
                        iconColor = Color.White,
                        borderColor = Color(0xFFA51930),
                        bgGradient = Brush.linearGradient(listOf(Color(0xFF2E101B), Color(0xFF4D1523))),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    SummaryCard(
                        title = "الأرباح",
                        amount = totalProfit.formatCurrency().replace("ر.س", "").trim(),
                        currency = "ريال",
                        icon = Icons.Rounded.BarChart,
                        iconBgColor = Color(0xFF1CA778),
                        iconColor = Color.White,
                        borderColor = Color(0xFF117B5B),
                        bgGradient = Brush.linearGradient(listOf(Color(0xFF0A2420), Color(0xFF0F4237))),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(12.dp))
                    SummaryCard(
                        title = "الصندوق",
                        amount = cashBalance.formatCurrency().replace("ر.س", "").trim(),
                        currency = "ريال",
                        icon = Icons.Rounded.AccountBalance,
                        iconBgColor = Color(0xFFB88F20),
                        iconColor = Color.White,
                        borderColor = Color(0xFF977421),
                        bgGradient = Brush.linearGradient(listOf(Color(0xFF281F0F), Color(0xFF403011))),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        item { Spacer(Modifier.height(16.dp)) }
        
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                DebtCard(
                    title = "ديون الموردين",
                    amount = totalSupplierDebts.formatCurrency().replace("ر.س", "").trim(),
                    currency = "ريال",
                    icon = Icons.Rounded.Person,
                    iconBgColor = Color(0xFFF3C9C9),
                    iconColor = Color(0xFFDE6469),
                    bgColor = Color(0xFFF5D3D1),
                    textColor = Color(0xFFBC454A),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                DebtCard(
                    title = "ديون لي على العملاء",
                    amount = totalCustomerDebts.formatCurrency().replace("ر.س", "").trim(),
                    currency = "ريال",
                    icon = Icons.Rounded.Group,
                    iconBgColor = Color(0xFFD8CCF6),
                    iconColor = Color(0xFF8664D4),
                    bgColor = Color(0xFFE4DAF8),
                    textColor = Color(0xFF704EB8),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        item { Spacer(Modifier.height(24.dp)) }
        
        item {
            SectionTitle(
                title = "إجراءات سريعة",
                icon = Icons.Rounded.Bolt,
                textColor = Color(0xFF11133C)
            )
        }
        
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    QuickActionButton(
                        title = "التقارير",
                        icon = Icons.Rounded.Leaderboard,
                        iconBgColor = Color(0xFF234928),
                        iconColor = Color(0xFF78C03D),
                        onClick = { navController.navigate(Screen.Reports.route) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(12.dp))
                    QuickActionButton(
                        title = "المزامنة",
                        icon = Icons.Rounded.Sync,
                        iconBgColor = Color(0xFF0D4E61),
                        iconColor = Color(0xFF2FB9D8),
                        onClick = { navController.navigate(Screen.Settings.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    QuickActionButton(
                        title = "المنتجات",
                        icon = Icons.Rounded.Inventory2,
                        iconBgColor = Color(0xFF0A4C5D),
                        iconColor = Color(0xFF2CB6D4),
                        onClick = { navController.navigate(Screen.Products.route) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(12.dp))
                    QuickActionButton(
                        title = "نواقص المخزون",
                        icon = Icons.Rounded.Warning,
                        iconBgColor = Color(0xFF5A2218),
                        iconColor = Color(0xFFDE6127),
                        onClick = { navController.navigate(Screen.LowStock.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    QuickActionButton(
                        title = "المبيعات",
                        icon = Icons.Rounded.AttachMoney,
                        iconBgColor = Color(0xFF234928),
                        iconColor = Color(0xFF78C03D),
                        onClick = { navController.navigate(Screen.Sales.route) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(12.dp))
                    QuickActionButton(
                        title = "المشتريات",
                        icon = Icons.Rounded.ShoppingCart,
                        iconBgColor = Color(0xFF3D145A),
                        iconColor = Color(0xFF9C38D3),
                        onClick = { navController.navigate(Screen.Purchases.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    QuickActionButton(
                        title = "المصروفات",
                        icon = Icons.Rounded.AccountBalanceWallet,
                        iconBgColor = Color(0xFF58291A),
                        iconColor = Color(0xFFE66840),
                        onClick = { navController.navigate(Screen.Expenses.route) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(12.dp))
                    QuickActionButton(
                        title = "الموردون",
                        icon = Icons.Rounded.Business,
                        iconBgColor = Color(0xFF123663),
                        iconColor = Color(0xFF2F83DB),
                        onClick = { navController.navigate(Screen.Suppliers.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))
                QuickActionButton(
                    title = "العملاء",
                    icon = Icons.Rounded.Group,
                    iconBgColor = Color(0xFF2E195C),
                    iconColor = Color(0xFF754AE3),
                    onClick = { navController.navigate(Screen.Customers.route) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun TopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .padding(top = 16.dp), // Extra padding for status bar
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color(0xFF3F1C99), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Menu,
                contentDescription = "Menu",
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "مرحبا بك 👋",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "إدارة أعمالك بسهولة واحترافية",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun SectionTitle(title: String, icon: ImageVector, textColor: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = textColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun SummaryCard(
    title: String,
    amount: String,
    currency: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconColor: Color,
    borderColor: Color,
    bgGradient: Brush,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(110.dp)
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(16.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = Path()
                path.moveTo(0f, size.height * 0.8f)
                path.quadraticBezierTo(
                    size.width * 0.2f, size.height * 0.8f,
                    size.width * 0.4f, size.height * 0.6f
                )
                path.quadraticBezierTo(
                    size.width * 0.6f, size.height * 0.4f,
                    size.width * 0.8f, size.height * 0.5f
                )
                path.lineTo(size.width, size.height * 0.2f)
                
                val fillPath = Path().apply {
                    addPath(path)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(fillPath, color = iconBgColor.copy(alpha = 0.1f))
                
                drawPath(
                    path = path,
                    color = iconBgColor.copy(alpha = 0.3f),
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )
            }
            
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(iconBgColor, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = amount,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = currency,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DebtCard(
    title: String,
    amount: String,
    currency: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconColor: Color,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = title,
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(iconBgColor, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.align(Alignment.Start) // Right in RTL
            ) {
                Text(
                    text = amount,
                    color = textColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = currency,
                    color = textColor.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

@Composable
fun QuickActionButton(
    title: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(64.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF34367A)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF22245C))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconBgColor, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
""")
print("DashboardScreen.kt rewritten")
