package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.CashMovement
import com.example.ui.viewmodels.StoreViewModel
import com.example.utils.formatCurrency
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashboxScreen(viewModel: StoreViewModel, navController: androidx.navigation.NavController) {
    val cashBalance by viewModel.cashBalance.collectAsStateWithLifecycle()
    val cashMovements by viewModel.cashMovements.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الصندوق") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("الرصيد الحالي", color = MaterialTheme.colorScheme.onSecondary, fontSize = 16.sp)
                    Text(cashBalance.formatCurrency(), color = MaterialTheme.colorScheme.onSecondary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("حركات الصندوق", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            if (cashMovements.isEmpty()) {
                Text("لا توجد حركات حتى الآن.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(cashMovements) { movement ->
                        CashMovementCard(movement)
                    }
                }
            }
        }
    }
}

@Composable
fun CashMovementCard(movement: CashMovement) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                Text(dateFormat.format(Date(movement.movementDate)), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(movement.movementType, fontWeight = FontWeight.Bold)
                if (!movement.note.isNullOrBlank()) {
                    Text(movement.note, fontSize = 14.sp)
                }
            }
            Text(
                text = "${if (movement.direction == "IN") "+" else "-"}${movement.amount.formatCurrency()}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = if (movement.direction == "IN") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
            )
        }
    }
}
