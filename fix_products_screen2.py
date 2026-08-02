import re

with open('app/src/main/java/com/example/ui/screens/ProductsScreen.kt', 'r') as f:
    content = f.read()

# LuxuryTopBar
top_bar_old = """@Composable
fun LuxuryTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bag Icon (Will be on the Right in RTL)
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(GlowPurple, GlowPink)))
                .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = GlowPink),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.ShoppingBag, contentDescription = "السلة", tint = Color.White, modifier = Modifier.size(28.dp))
            Icon(Icons.Default.Star, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(12.dp).align(Alignment.TopStart).padding(3.dp))
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
        
        // Back Button (Will be on the Left in RTL)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
                .clickable { onBackClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ArrowForward, contentDescription = "رجوع", tint = Color.White)
        }
    }
}"""

top_bar_new = """@Composable
fun LuxuryTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bag Icon
        Box(
            modifier = Modifier
                .size(48.dp)
                .shadow(4.dp, RoundedCornerShape(14.dp), spotColor = GlowPink)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(GlowPurple, GlowPink))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.ShoppingBag, contentDescription = "السلة", tint = Color.White, modifier = Modifier.size(24.dp))
            Icon(Icons.Default.Star, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(10.dp).align(Alignment.TopStart).padding(2.dp))
        }
        Spacer(modifier = Modifier.weight(1f))
        
        // Title Column
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "المنتجات",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF111122)
            )
            Text(
                text = "تصفح جميع المنتجات المتاحة",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Back Button
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.White)
                .shadow(2.dp, CircleShape)
                .clickable { onBackClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ArrowForward, contentDescription = "رجوع", tint = Color(0xFF111122))
        }
    }
}"""
content = content.replace(top_bar_old, top_bar_new)

# LuxuryTabItem text colors
tab_item_old = """@Composable
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
        val bgMod = if (isSelected) Modifier.background(Brush.horizontalGradient(listOf(GlowBlue, GlowPurple, GlowPink))) else Modifier.background(Color.Transparent)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(4.dp)
                .clip(CircleShape)
                .then(bgMod)
        )
    }
}"""
tab_item_new = """@Composable
fun LuxuryTabItem(text: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = if (isSelected) Color(0xFF111122) else TextGray, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = if (isSelected) Color(0xFF111122) else TextGray,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 15.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Glowing Underline
        val bgMod = if (isSelected) Modifier.background(Brush.horizontalGradient(listOf(GlowBlue, GlowPurple, GlowPink))) else Modifier.background(Color.Transparent)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(3.dp)
                .clip(CircleShape)
                .then(bgMod)
        )
    }
}"""
content = content.replace(tab_item_old, tab_item_new)

# LuxuryCategoryChip update to Row
chip_old = """@Composable
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
}"""
chip_new = """@Composable
fun LuxuryCategoryChip(text: String, icon: ImageVector, iconColor: Color, isSelected: Boolean, onClick: () -> Unit) {
    val bgModifier = if (isSelected) {
        Modifier.background(Brush.linearGradient(listOf(Color(0xFF5324D6), Color(0xFF9D4EDD))))
    } else {
        Modifier.background(Color.White)
    }
    
    Row(
        modifier = Modifier
            .shadow(if (isSelected) 8.dp else 2.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .then(bgModifier)
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (isSelected) Color.White.copy(alpha = 0.2f) else iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, 
                contentDescription = null, 
                tint = if (isSelected) Color.White else iconColor,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = if (isSelected) Color.White else Color(0xFF333333),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}"""
content = content.replace(chip_old, chip_new)

# Reduce card sizes
card_regex = r'modifier = Modifier\.width\(130\.dp\)\n\s*\.height\(160\.dp\)'
content = re.sub(card_regex, 'modifier = Modifier.width(100.dp).height(120.dp)', content)

# Change card styling
card_top_regex = r'Card\(\n\s*modifier = Modifier\.fillMaxWidth\(\),\n\s*shape = RoundedCornerShape\(24\.dp\),\n\s*colors = CardDefaults\.cardColors\(containerColor = cardBg\),\n\s*elevation = CardDefaults\.cardElevation\(defaultElevation = 8\.dp\)\n\s*\)'
card_top_new = """Card(
        modifier = Modifier.fillMaxWidth().border(1.5.dp, Color(0xFFEBEBEB), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    )"""
content = re.sub(card_top_regex, card_top_new, content)

# Button in card
button_regex = r'\.height\(42\.dp\)'
content = re.sub(button_regex, '.height(36.dp)', content)

# Price font size
price_regex = r'fontSize = 22\.sp'
content = re.sub(price_regex, 'fontSize = 18.sp', content)

# Badges padding and font
badge_pad = r'\.padding\(horizontal = 8\.dp, vertical = 6\.dp\)'
content = re.sub(badge_pad, '.padding(horizontal = 6.dp, vertical = 4.dp)', content)

with open('app/src/main/java/com/example/ui/screens/ProductsScreen.kt', 'w') as f:
    f.write(content)

