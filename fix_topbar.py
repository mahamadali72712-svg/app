import os

with open("app/src/main/java/com/example/ui/screens/ProductsScreen.kt", "r") as f:
    content = f.read()

old_topbar = """@Composable
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
}"""

new_topbar = """@Composable
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
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "رجوع", tint = Color.White)
        }
    }
}"""

content = content.replace(old_topbar, new_topbar)

# Make sure to import AutoMirrored icons if not already there, actually standard Icons.AutoMirrored is in compose 1.5+
# If AutoMirrored fails, we can just use Icons.Default.ArrowForward since it's RTL
content = content.replace("Icons.AutoMirrored.Filled.ArrowForward", "Icons.Default.ArrowForward")

with open("app/src/main/java/com/example/ui/screens/ProductsScreen.kt", "w") as f:
    f.write(content)

