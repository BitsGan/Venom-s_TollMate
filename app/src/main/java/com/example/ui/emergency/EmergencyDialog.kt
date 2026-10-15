package com.example.ui.emergency

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.TollMateRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyDialog(
    repository: TollMateRepository,
    journeyId: String,
    vehicleNumber: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val emergencyTypes = listOf(
        "ACCIDENT" to "Vehicle Accident / Crash",
        "BREAKDOWN" to "Mechanical Breakdown / Flat Tyre",
        "MEDICAL" to "Medical Emergency / Ambulance",
        "CRIME_SAFETY" to "Highway Crime / Threat",
        "OTHER" to "Other Highway Hazard"
    )

    var selectedType by remember { mutableStateOf(emergencyTypes[0].first) }
    var locationHint by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var ticketNumber by remember { mutableStateOf<String?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Emergency,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Highway Emergency SOS",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDC2626)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Critical Warning & Immediate Dial Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "CRITICAL: For life-threatening emergencies, dial directly. Do not wait for in-app responses.",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF991B1B)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { dialHelpline(context, "112") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dial_112_button")
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("DIAL 112", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = { dialHelpline(context, "1033") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB45309)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dial_1033_button")
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("NHAI 1033", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (ticketNumber != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Emergency Request Logged: $ticketNumber",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF166534)
                            )
                            Text(
                                text = "Linked to Journey: $journeyId. Highway patrol assistance records updated.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF166534)
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Log Incident Ticket",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Dropdown for emergency type
                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = emergencyTypes.find { it.first == selectedType }?.second ?: selectedType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Emergency Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .testTag("emergency_type_dropdown"),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            emergencyTypes.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item.second) },
                                    onClick = {
                                        selectedType = item.first
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = locationHint,
                        onValueChange = { locationHint = it },
                        label = { Text("Milestone / Nearby Landmark") },
                        placeholder = { Text("e.g. Near KM Stone 84, Khalapur Food Mall") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("emergency_location_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Details for Assistance Team") },
                        placeholder = { Text("Describe injuries or vehicle status") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("emergency_notes_input"),
                        shape = RoundedCornerShape(10.dp),
                        maxLines = 3
                    )
                }
            }
        },
        confirmButton = {
            if (ticketNumber == null) {
                Button(
                    onClick = {
                        isSubmitting = true
                        coroutineScope.launch {
                            val res = repository.reportEmergency(
                                journeyId = journeyId.ifBlank { "HIGHWAY-SOS" },
                                vehicleNumber = vehicleNumber.ifBlank { "UNKNOWN" },
                                emergencyType = selectedType,
                                locationDescription = locationHint,
                                notes = notes
                            )
                            isSubmitting = false
                            if (res.isSuccess) {
                                ticketNumber = res.getOrNull()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    enabled = !isSubmitting,
                    modifier = Modifier.testTag("emergency_submit_ticket_button")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                    } else {
                        Text("Log In-App Ticket")
                    }
                }
            } else {
                Button(onClick = onDismiss) {
                    Text("Close")
                }
            }
        },
        dismissButton = {
            if (ticketNumber == null) {
                TextButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
            }
        }
    )
}

private fun dialHelpline(context: Context, number: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback
    }
}
