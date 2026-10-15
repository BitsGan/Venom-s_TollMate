package com.example.ui.map

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TollPlaza
import com.example.data.repository.TollMateRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTollMapScreen(
    repository: TollMateRepository,
    userId: String,
    onBackClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val tolls by repository.observeTolls().collectAsState(initial = repository.getBundledTolls())
    val activeJourney by repository.observeActiveJourney(userId).collectAsState(initial = null)

    var simulatedProgress by remember { mutableFloatStateOf(0.2f) }
    var isSimulatingDrive by remember { mutableStateOf(true) }
    var detectedToll by remember { mutableStateOf<TollPlaza?>(null) }
    var passFeedback by remember { mutableStateOf<String?>(null) }

    // Highway progress simulation
    LaunchedEffect(isSimulatingDrive) {
        while (isSimulatingDrive) {
            delay(1200)
            simulatedProgress += 0.05f
            if (simulatedProgress > 1.0f) simulatedProgress = 0.05f

            // Trigger detection when near 0.35 or 0.7
            detectedToll = when {
                simulatedProgress in 0.30f..0.45f && tolls.isNotEmpty() -> tolls[0]
                simulatedProgress in 0.65f..0.80f && tolls.size > 1 -> tolls[1]
                else -> null
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Toll Detection", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("map_back_button")) {
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
            // Live Radar / Toll Highway Visual Canvas
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val roadColor = Color(0xFF334155)
                    val lineColor = Color(0xFFFBBF24)
                    val carColor = MaterialTheme.colorScheme.primary

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Draw expressway lanes
                        drawLine(
                            color = roadColor,
                            start = Offset(40f, h * 0.5f),
                            end = Offset(w - 40f, h * 0.5f),
                            strokeWidth = 40f
                        )

                        // Center dash line
                        drawLine(
                            color = lineColor,
                            start = Offset(50f, h * 0.5f),
                            end = Offset(w - 50f, h * 0.5f),
                            strokeWidth = 4f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 15f), 0f)
                        )

                        // Toll plaza checkpoints
                        val plaza1X = w * 0.38f
                        val plaza2X = w * 0.72f

                        drawCircle(color = Color(0xFFDC2626), radius = 16f, center = Offset(plaza1X, h * 0.5f))
                        drawCircle(color = Color(0xFFDC2626), radius = 16f, center = Offset(plaza2X, h * 0.5f))

                        // Vehicle position along expressway
                        val carX = 50f + (w - 100f) * simulatedProgress
                        drawCircle(color = carColor, radius = 20f, center = Offset(carX, h * 0.5f))
                        drawCircle(color = Color.White, radius = 8f, center = Offset(carX, h * 0.5f))
                    }

                    // Status Overlays
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "GPS TELEMETRY ACTIVE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Speed / Vehicle badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Vehicle: ${activeJourney?.vehicleNumber ?: "MH12AB1234"} • 85 km/h",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Toll Proximity Detection Card
            if (detectedToll != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Toll,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "APPROACHING TOLL PLAZA",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = detectedToll?.name ?: "",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${detectedToll?.highway} • Est. Rate: ₹${detectedToll?.standardRate?.toInt()} • ${detectedToll?.availableLanes} Lanes Open",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                detectedToll?.let { plaza ->
                                    coroutineScope.launch {
                                        val jId = activeJourney?.journeyId ?: "SIM-JRN-99"
                                        val res = repository.recordSimulatedTollDeduction(
                                            journeyId = jId,
                                            tollName = plaza.name,
                                            amount = plaza.standardRate
                                        )
                                        passFeedback = if (res.isSuccess) {
                                            "Simulated pass confirmed! Deducted ₹${plaza.standardRate.toInt()}"
                                        } else {
                                            "Deduction error: ${res.exceptionOrNull()?.localizedMessage}"
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("map_simulate_toll_pass_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Simulate Toll Barrier Passage (₹${detectedToll?.standardRate?.toInt()})")
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Cruising Highway Route",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Next plaza: ${tolls.firstOrNull()?.name ?: "Khalapur"} (approx 12 km)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            if (passFeedback != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = passFeedback ?: "",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Toll Plazas on Highway Database (${tolls.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(tolls) { plaza ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = plaza.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "₹${plaza.standardRate.toInt()}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "${plaza.highway} • Coordinates: ${plaza.latitude}, ${plaza.longitude}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "Facilities: " + plaza.facilities.take(3).joinToString(", "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}
