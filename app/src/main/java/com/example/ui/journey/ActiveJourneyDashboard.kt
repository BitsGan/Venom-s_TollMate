package com.example.ui.journey

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Journey
import com.example.data.model.TollPlaza
import com.example.data.repository.TollMateRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveJourneyDashboard(
    repository: TollMateRepository,
    userId: String,
    journeyId: String,
    onBackClick: () -> Unit,
    onNavigateToLiveToll: () -> Unit,
    onOpenFacilitiesMenu: () -> Unit,
    onOpenEmergencySOS: (String, String) -> Unit,
    onJourneyCompleted: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val journeys by repository.observeUserJourneys(userId).collectAsState(initial = emptyList())
    val tolls by repository.observeTolls().collectAsState(initial = repository.getBundledTolls())
    val journey = journeys.find { it.journeyId == journeyId } ?: journeys.firstOrNull { it.status == "ACTIVE" }

    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    var showEndJourneyDialog by remember { mutableStateOf(false) }
    var isEndingJourney by remember { mutableStateOf(false) }
    var tollSimFeedback by remember { mutableStateOf<String?>(null) }

    // Live elapsed timer
    LaunchedEffect(journey) {
        val startSec = journey?.createdAt?.seconds ?: (System.currentTimeMillis() / 1000)
        while (journey?.status == "ACTIVE") {
            val nowSec = System.currentTimeMillis() / 1000
            elapsedSeconds = maxOf(0L, nowSec - startSec)
            delay(1000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Active Journey", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("dashboard_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // SOS Emergency Button in top bar
                    IconButton(
                        onClick = {
                            onOpenEmergencySOS(
                                journey?.journeyId ?: journeyId,
                                journey?.vehicleNumber ?: ""
                            )
                        },
                        modifier = Modifier.testTag("dashboard_sos_top_bar")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Emergency,
                            contentDescription = "SOS",
                            tint = Color(0xFFDC2626)
                        )
                    }
                }
            )
        },
        // Delivery-style Plus (+) floating button for highway amenities & services
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenFacilitiesMenu,
                icon = { Icon(Icons.Default.Add, contentDescription = "Request Services") },
                text = { Text("Highway Facilities (+)") },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = Color.White,
                modifier = Modifier.testTag("dashboard_plus_facilities_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Stage 3 Header: Unique Journey ID, Vehicle Number, Status
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "JOURNEY IDENTIFIER",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = journey?.journeyId ?: journeyId,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.secondary)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = journey?.status ?: "ACTIVE",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Vehicle Number",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = journey?.vehicleNumber ?: "N/A",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Elapsed Travel Time",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formatDuration(elapsedSeconds),
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Route Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Route Progression",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Start: " + (journey?.startLocation ?: "Origin"),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            if (journey?.startLatitude != null) {
                                Text(
                                    text = "Lat: ${journey.startLatitude}, Lng: ${journey.startLongitude}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Destination: " + (journey?.destination ?: "Destination"),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricPill(
                            title = "Tolls Passed",
                            value = "${journey?.tollsPassedCount ?: 0}"
                        )
                        MetricPill(
                            title = "Toll Deducted",
                            value = "₹${journey?.totalTollsDeducted?.toInt() ?: 0}"
                        )
                        MetricPill(
                            title = "Est. Distance",
                            value = "${journey?.totalDistanceKm?.toInt() ?: 150} km"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live Toll Detection Shortcut & Simulated Pass
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Upcoming Highway Tolls",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(onClick = onNavigateToLiveToll) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Map View")
                        }
                    }

                    if (tollSimFeedback != null) {
                        Text(
                            text = tollSimFeedback ?: "",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    // Display next 2 toll plazas with quick "Simulate Pass" button
                    tolls.take(2).forEach { plaza ->
                        TollPlazaRow(
                            plaza = plaza,
                            onSimulatePass = {
                                coroutineScope.launch {
                                    tollSimFeedback = "Deducting ₹${plaza.standardRate.toInt()} for ${plaza.name}..."
                                    val res = repository.recordSimulatedTollDeduction(
                                        journeyId = journey?.journeyId ?: journeyId,
                                        tollName = plaza.name,
                                        amount = plaza.standardRate
                                    )
                                    if (res.isSuccess) {
                                        tollSimFeedback = "Simulated pass recorded for ${plaza.name} (₹${plaza.standardRate.toInt()})"
                                    } else {
                                        tollSimFeedback = "Deduction failed: ${res.exceptionOrNull()?.localizedMessage}"
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons: End Journey
            Button(
                onClick = { showEndJourneyDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("dashboard_end_journey_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(imageVector = Icons.Default.StopCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "END JOURNEY & GET RECEIPT",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(72.dp)) // Space for FAB
        }
    }

    // End Journey Confirmation Dialog
    if (showEndJourneyDialog) {
        AlertDialog(
            onDismissRequest = { if (!isEndingJourney) showEndJourneyDialog = false },
            title = { Text("Complete Journey?") },
            text = {
                Text("This will mark journey ${journey?.journeyId} as COMPLETED, record the end timestamp, and generate your digital journey summary receipt.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        isEndingJourney = true
                        coroutineScope.launch {
                            val targetId = journey?.journeyId ?: journeyId
                            repository.endJourney(targetId)
                            isEndingJourney = false
                            showEndJourneyDialog = false
                            onJourneyCompleted(targetId)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = !isEndingJourney
                ) {
                    if (isEndingJourney) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                    } else {
                        Text("Confirm & End")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndJourneyDialog = false }, enabled = !isEndingJourney) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MetricPill(title: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, style = MaterialTheme.typography.labelSmall)
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun TollPlazaRow(
    plaza: TollPlaza,
    onSimulatePass: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = plaza.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "${plaza.highway} • Rate: ₹${plaza.standardRate.toInt()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        Button(
            onClick = onSimulatePass,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Pass Toll", style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) {
        String.format(Locale.US, "%02dh %02dm %02ds", hrs, mins, secs)
    } else {
        String.format(Locale.US, "%02dm %02ds", mins, secs)
    }
}
