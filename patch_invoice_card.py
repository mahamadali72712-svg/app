import re

with open('app/src/main/java/com/example/ui/screens/SalesHistoryScreen.kt', 'r') as f:
    content = f.read()

card_search = '''        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(invoice.invoiceNumber, fontWeight = FontWeight.Bold)'''

card_replace = '''        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(invoice.invoiceNumber, fontWeight = FontWeight.Bold)
                    if (invoice.status == "RETURN") {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(color = MaterialTheme.colorScheme.error, shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)) {
                            Text("مرتجع", color = androidx.compose.ui.graphics.Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                }'''

content = content.replace(card_search, card_replace)

with open('app/src/main/java/com/example/ui/screens/SalesHistoryScreen.kt', 'w') as f:
    f.write(content)
