import re

with open('app/src/main/java/com/example/ui/navigation/Navigation.kt', 'r') as f:
    nav_content = f.read()

nav_content = nav_content.replace('import androidx.compose.material3.*', '''import androidx.compose.material3.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.rounded.*''')

app_nav_start = nav_content.find('@Composable\nfun AppNavigation(viewModel: StoreViewModel) {')
if app_nav_start != -1:
    new_app_nav = '''val MainBgGradient = Brush.verticalGradient(
    0.0f to Color(0xFF0A0920),
    0.3f to Color(0xFF2A2152),
    0.4f to Color(0xFFE6E1F4),
    0.75f to Color(0xFFE6E1F4),
    0.9f to Color(0xFF1F1F4D),
    1.0f to Color(0xFF14143D)
)

@Composable
fun AppNavigation(viewModel: StoreViewModel) {
    val navController = rememberNavController()
    val bottomBarItems = listOf(Screen.Dashboard, Screen.Products, Screen.Sales, Screen.Reports, Screen.Settings)
    
    Box(modifier = Modifier.fillMaxSize().background(MainBgGradient)) {
        // Glow at top left
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .fillMaxHeight(0.4f)
                .align(Alignment.TopStart)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x884A229E), Color.Transparent),
                        radius = 600f
                    )
                )
        )
        
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(72.dp)
                        .background(Color(0xFF22245C), RoundedCornerShape(24.dp))
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bottomBarItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(4.dp)
                                .background(
                                    if (isSelected) Color(0xFF6832CC) else Color.Transparent,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val iconVector = when (screen.route) {
                                "dashboard" -> Icons.Rounded.Home
                                "products" -> Icons.Rounded.List
                                "sales" -> Icons.Rounded.ShoppingCart
                                "reports" -> Icons.Rounded.DateRange
                                "settings" -> Icons.Rounded.Settings
                                else -> Icons.Rounded.Home
                            }
                            Icon(
                                imageVector = iconVector,
                                contentDescription = screen.title,
                                tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = screen.title,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.padding(innerPadding)
            ) {
'''
    nav_content = nav_content[:app_nav_start] + new_app_nav + nav_content[nav_content.find('            composable(Screen.Dashboard.route)'):]
    # Need to add a closing brace for the Box
    nav_content += "\n    }\n}"

with open('app/src/main/java/com/example/ui/navigation/Navigation.kt', 'w') as f:
    f.write(nav_content)

print("Navigation.kt updated")
