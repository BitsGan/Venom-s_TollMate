package com.example.ui.receipt

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Journey
import com.example.data.repository.TollMateRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyReceiptScreen(
    repository: TollMateRepository,
    userId: String,
    highlightJourneyId: String? = null,
    onBackClick: () -> Unit
) {
    val journeys by repository.observeUserJourneys(userId).collectAsState(initial = emptyList())
    val completedJourneys = journeys.filter { it.status == "COMPLETED" }

    var selectedJourney by remember {
        mutableStateOf(
            journeys.find { it.journeyId == highlightJourneyId } ?: completedJourneys.firstOrNull() ?: journeys.firstOrNull()
        )
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(journeys) {
        if (selectedJourney == null && journeys.isNotEmpty()) {
            selectedJourney = journeys.find { it.journeyId == highlightJourneyId } ?: journeys.firstOrNull()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Digital Journey Receipts", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("receipt_back_button")) {
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
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Receipt View") },
                    icon = { Icon(Icons.Default.Receipt, contentDescription = null) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("History (${completedJourneys.size})") },
                    icon = { Icon(Icons.Default.History, contentDescription = null) }
                )
            }

            if (selectedTabIndex == 0) {
                if (selectedJourney != null) {
                    ReceiptCardView(
                        journey = selectedJourney!!,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No journey receipts found yet. Complete a journey to view digital receipts.")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    items(completedJourneys) { jrn ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable {
                                    selectedJourney = jrn
                                    selectedTabIndex = 0
                                }
                                .testTag("receipt_item_${jrn.journeyId}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = jrn.journeyId,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "₹${jrn.totalTollsDeducted.toInt()}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${jrn.startLocation} → ${jrn.destination}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Vehicle: ${jrn.vehicleNumber} • Tolls: ${jrn.tollsPassedCount} • Amenities: ${jrn.servicesUsedCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptCardView(
    journey: Journey,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "HIGHWAY JOURNEY SUMMARY",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "TollMate Digital Receipt",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Completed",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(14.dp))

            // Details
            ReceiptItemRow(label = "Journey ID", value = journey.journeyId, isCode = true)
            ReceiptItemRow(label = "Vehicle Registration", value = journey.vehicleNumber)
            ReceiptItemRow(label = "Route Origin", value = journey.startLocation)
            ReceiptItemRow(label = "Route Destination", value = journey.destination)
            ReceiptItemRow(label = "Estimated Distance", value = "${journey.totalDistanceKm.toInt()} KM")
            ReceiptItemRow(label = "Toll Plazas Crossed", value = "${journey.tollsPassedCount}")
            ReceiptItemRow(label = "Highway Services Used", value = "${journey.servicesUsedCount}")

            val dateStr = journey.createdAt?.toDate()?.let {
                SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(it)
            } ?: SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
            ReceiptItemRow(label = "Issued Date & Time", value = dateStr)

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(14.dp))

            // Charges Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total Recorded Tolls",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "₹${journey.totalTollsDeducted.toInt()}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mandatory Disclaimer Box
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(10.dp)
            ) {
                Text(
                    text = "Notice: App-recorded wallet deductions are for journey tracking and estimation only. Not an official FASTag or NHAI toll receipt.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ReceiptItemRow(label: String, value: String, isCode: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontFamily = if (isCode) FontFamily.Monospace else FontFamily.Default
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
