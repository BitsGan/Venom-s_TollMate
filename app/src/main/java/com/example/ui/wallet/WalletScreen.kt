package com.example.ui.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Transaction
import com.example.data.repository.TollMateRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    repository: TollMateRepository,
    userId: String,
    onBackClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val userProfile by repository.observeUserProfile(userId).collectAsState(initial = null)
    val transactions by repository.observeTransactions(userId).collectAsState(initial = emptyList())

    var isRecharging by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Digital Toll Wallet", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("wallet_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Balance Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "AVAILABLE DIGITAL WALLET BALANCE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "₹${String.format(Locale.US, "%.2f", userProfile?.walletBalance ?: 500.0)}",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick Top-up buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(200.0, 500.0, 1000.0).forEach { amount ->
                            Button(
                                onClick = {
                                    isRecharging = true
                                    statusMsg = null
                                    coroutineScope.launch {
                                        val res = repository.rechargeWallet(amount)
                                        isRecharging = false
                                        if (res.isSuccess) {
                                            statusMsg = "Successfully added ₹${amount.toInt()} to wallet!"
                                        } else {
                                            statusMsg = "Recharge failed: ${res.exceptionOrNull()?.localizedMessage}"
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("topup_${amount.toInt()}"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                enabled = !isRecharging
                            ) {
                                Text("+₹${amount.toInt()}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (statusMsg != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = statusMsg ?: "",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mandatory Disclaimer Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Notice: App-recorded wallet deductions are for trip simulation and in-app expense tracking. Not verified FASTag or official toll payments.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Transaction History (${transactions.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No transactions recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(transactions) { tx ->
                        TransactionRow(tx = tx)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(tx: Transaction) {
    val isTopup = tx.type == "WALLET_TOPUP"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isTopup) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isTopup) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (isTopup) Color(0xFF166534) else Color(0xFFDC2626),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = tx.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = tx.description.ifBlank { tx.type },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
            }

            Text(
                text = "${if (isTopup) "+" else "-"}₹${tx.amount.toInt()}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isTopup) Color(0xFF166534) else Color(0xFFDC2626)
            )
        }
    }
}
