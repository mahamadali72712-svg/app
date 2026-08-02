with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    content = f.read()

import re

# Update SummaryCard height
content = re.sub(
    r'\.height\(110\.dp\)',
    '.height(90.dp)',
    content
)

# Update SummaryCard padding
# Find the padding inside the Box that holds Canvas and Column
content = re.sub(
    r'\.background\(bgGradient\)\s*\n\s*\.padding\(16\.dp\)',
    '.background(bgGradient)\n                .padding(12.dp)',
    content
)

# In SummaryCard Column layout, change padding bottom of currency if any
content = re.sub(
    r'modifier = Modifier\.padding\(bottom = 4\.dp\)',
    'modifier = Modifier.padding(bottom = 2.dp)',
    content
)

# Update DebtCard height to 90.dp and padding to 12.dp for consistency
content = re.sub(
    r'fun DebtCard\(.*?Card\(\s*modifier = modifier\.height\(90\.dp\).*?Column\(\s*modifier = Modifier\s*\.fillMaxSize\(\)\s*\.padding\(16\.dp\)',
    '''fun DebtCard(
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
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)''',
    content,
    flags=re.DOTALL
)

# Card Colors Enhancement
# Mabe slightly more vibrant colors for the first 4 cards? 
# They are passed from DashboardScreen.
content = content.replace(
    'Brush.linearGradient(listOf(Color(0xFF0D1943), Color(0xFF0B2556)))',
    'Brush.linearGradient(listOf(Color(0xFF142B6D), Color(0xFF0B193C)))'
)
content = content.replace(
    'Color(0xFF1851CB)',
    'Color(0xFF2667ED)'
)

content = content.replace(
    'Brush.linearGradient(listOf(Color(0xFF2E101B), Color(0xFF4D1523)))',
    'Brush.linearGradient(listOf(Color(0xFF421727), Color(0xFF2E0F1A)))'
)
content = content.replace(
    'Color(0xFFA51930)',
    'Color(0xFFCA2641)'
)

content = content.replace(
    'Brush.linearGradient(listOf(Color(0xFF0A2420), Color(0xFF0F4237)))',
    'Brush.linearGradient(listOf(Color(0xFF134537), Color(0xFF0B2B22)))'
)
content = content.replace(
    'Color(0xFF117B5B)',
    'Color(0xFF1C9E78)'
)

content = content.replace(
    'Brush.linearGradient(listOf(Color(0xFF281F0F), Color(0xFF403011)))',
    'Brush.linearGradient(listOf(Color(0xFF553D10), Color(0xFF33250A)))'
)
content = content.replace(
    'Color(0xFF977421)',
    'Color(0xFFC7982E)'
)

# Fix amounts to scale properly if needed
content = content.replace('fontSize = 24.sp,', 'fontSize = 20.sp,')

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
    f.write(content)

print("DashboardScreen.kt updated")
