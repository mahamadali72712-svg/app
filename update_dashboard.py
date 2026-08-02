with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
    f.write("""package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.BorderStroke

val glowingWhite = Color.White
val glowingShadow = Shadow(color = Color(0xAAFFFFFF), blurRadius = 12f)
val glowingTextStyle = TextStyle(shadow = glowingShadow)

@Composable
fun DashboardScreen(viewModel: StoreViewModel, navController: NavController) {
    val totalSales by viewModel.totalSales.collectAsStateWithLifecycle()
    val totalProfit by viewModel.totalProfit.collectAsStateWithLifecycle()
    val cashBalance by viewModel.cashBalance.collectAsStateWithLifecycle()
    val operationalExpenses by viewModel.operationalExpenses.collectAsStateWithLifecycle()
    val totalSupplierDebts by viewModel.totalSupplierDebts.collectAsStateWithLifecycle()
    val totalCustomerDebts by viewModel.totalCustomerDebts.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        SectionTitle(
            title = "ملخص اليوم",
            icon = Icons.Rounded.SsidChart,
            textColor = glowingWhite
        )
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryCard(
                    title = "المبيعات",
                    amount = totalSales.formatCurrency().replace("ر.س", "").trim(),
                    currency = "ريال",
                    icon = Icons.Rounded.ShoppingCart,
                    iconBgColor = Color(0xFF2667ED),
                    iconColor = glowingWhite,
                    borderColor = Color(0xFF2667ED).copy(alpha = 0.5f),
                    bgGradient = Brush.linearGradient(listOf(Color(0xFF1B3B8A), Color(0xFF10214D))),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                SummaryCard(
                    title = "المصروفات",
                    amount = operationalExpenses.formatCurrency().replace("ر.س", "").trim(),
                    currency = "ريال",
                    icon = Icons.Rounded.AccountBalanceWallet,
                    iconBgColor = Color(0xFFCA2641),
                    iconColor = glowingWhite,
                    borderColor = Color(0xFFCA2641).copy(alpha = 0.5f),
                    bgGradient = Brush.linearGradient(listOf(Color(0xFF7A1D2E), Color(0xFF45111B))),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryCard(
                    title = "الأرباح",
                    amount = totalProfit.formatCurrency().replace("ر.س", "").trim(),
                    currency = "ريال",
                    icon = Icons.Rounded.BarChart,
                    iconBgColor = Color(0xFF1C9E78),
                    iconColor = glowingWhite,
                    borderColor = Color(0xFF1C9E78).copy(alpha = 0.5f),
                    bgGradient = Brush.linearGradient(listOf(Color(0xFF1A5A46), Color(0xFF103328))),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                SummaryCard(
                    title = "الصندوق",
                    amount = cashBalance.formatCurrency().replace("ر.س", "").trim(),
                    currency = "ريال",
                    icon = Icons.Rounded.AccountBalance,
                    iconBgColor = Color(0xFFC7982E),
                    iconColor = glowingWhite,
                    borderColor = Color(0xFFC7982E).copy(alpha = 0.5f),
                    bgGradient = Brush.linearGradient(listOf(Color(0xFF755417), Color(0xFF422F0C))),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Spacer(Modifier.height(4.dp))
        
        Row(modifier = Modifier.fillMaxWidth()) {
            DebtCard(
                title = "ديون الموردين",
                amount = totalSupplierDebts.formatCurrency().replace("ر.س", "").trim(),
                currency = "ريال",
                icon = Icons.Rounded.Person,
                iconBgColor = Color(0xFFF3C9C9),
                iconColor = Color(0xFFDE6469),
                bgColor = Color(0xFFE8D0D0),
                textColor = Color(0xFF94252A),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(10.dp))
            DebtCard(
                title = "ديون للعملاء",
                amount = totalCustomerDebts.formatCurrency().replace("ر.س", "").trim(),
                currency = "ريال",
                icon = Icons.Rounded.Group,
                iconBgColor = Color(0xFFD8CCF6),
                iconColor = Color(0xFF8664D4),
                bgColor = Color(0xFFD8CFEC),
                textColor = Color(0xFF472A80),
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(Modifier.height(4.dp))
        
        SectionTitle(
            title = "إجراءات سريعة",
            icon = Icons.Rounded.Bolt,
            textColor = glowingWhite
        )
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                QuickActionButton(
                    title = "التقارير",
                    icon = Icons.Rounded.Leaderboard,
                    iconBgColor = Color(0xFF386840),
                    iconColor = Color(0xFFA1EC60),
                    onClick = { navController.navigate(Screen.Reports.route) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                QuickActionButton(
                    title = "المزامنة",
                    icon = Icons.Rounded.Sync,
                    iconBgColor = Color(0xFF176E87),
                    iconColor = Color(0xFF55E5FF),
                    onClick = { navController.navigate(Screen.Settings.route) },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                QuickActionButton(
                    title = "المنتجات",
                    icon = Icons.Rounded.Inventory2,
                    iconBgColor = Color(0xFF14758F),
                    iconColor = Color(0xFF56DEFC),
                    onClick = { navController.navigate(Screen.Products.route) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                QuickActionButton(
                    title = "نواقص",
                    icon = Icons.Rounded.Warning,
                    iconBgColor = Color(0xFF7A3325),
                    iconColor = Color(0xFFFF8B57),
                    onClick = { navController.navigate(Screen.LowStock.route) },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                QuickActionButton(
                    title = "المبيعات",
                    icon = Icons.Rounded.AttachMoney,
                    iconBgColor = Color(0xFF386840),
                    iconColor = Color(0xFFA1EC60),
                    onClick = { navController.navigate(Screen.Sales.route) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                QuickActionButton(
                    title = "المشتريات",
                    icon = Icons.Rounded.ShoppingCart,
                    iconBgColor = Color(0xFF5B2285),
                    iconColor = Color(0xFFCD64FF),
                    onClick = { navController.navigate(Screen.Purchases.route) },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                QuickActionButton(
                    title = "المصروفات",
                    icon = Icons.Rounded.AccountBalanceWallet,
                    iconBgColor = Color(0xFF793B26),
                    iconColor = Color(0xFFFF926B),
                    onClick = { navController.navigate(Screen.Expenses.route) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                QuickActionButton(
                    title = "الموردون",
                    icon = Icons.Rounded.Business,
                    iconBgColor = Color(0xFF1D5194),
                    iconColor = Color(0xFF67B5FF),
                    onClick = { navController.navigate(Screen.Suppliers.route) },
                    modifier = Modifier.weight(1f)
                )
            }
            QuickActionButton(
                title = "العملاء",
                icon = Icons.Rounded.Group,
                iconBgColor = Color(0xFF422582),
                iconColor = Color(0xFF9D6FFF),
                onClick = { navController.navigate(Screen.Customers.route) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SectionTitle(title: String, icon: ImageVector, textColor: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = textColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            style = glowingTextStyle
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
            .height(80.dp)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(10.dp)
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
                drawPath(fillPath, color = iconBgColor.copy(alpha = 0.15f))
                
                drawPath(
                    path = path,
                    color = iconBgColor.copy(alpha = 0.4f),
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
                        color = glowingWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        style = glowingTextStyle
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(iconBgColor, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = amount,
                        color = glowingWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        style = glowingTextStyle
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = currency,
                        color = glowingWhite.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 2.dp),
                        style = glowingTextStyle
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBgColor: Color,
    iconColor: Color,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(75.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, iconBgColor),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
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
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(iconBgColor, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text(
                    text = amount,
                    color = textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = currency,
                    color = textColor.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 1.dp)
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
            .height(56.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, Color(0xFF45498A)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF22245C))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = glowingWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                style = glowingTextStyle
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
    }
}
""")
