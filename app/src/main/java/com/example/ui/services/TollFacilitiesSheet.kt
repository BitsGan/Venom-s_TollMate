package com.example.ui.services

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ServiceRequest
import com.example.data.model.TollPlaza
import com.example.data.repository.TollMateRepository
import kotlinx.coroutines.launch

data class FacilityCategoryItem(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val typicalCharge: Double
)

val AllFacilityCategories = listOf(
    FacilityCategoryItem("FUEL", "Fuel Station", Icons.Default.LocalGasStation, 0.0),
    FacilityCategoryItem("EV_CHARGING", "EV Fast Charging", Icons.Default.EvStation, 250.0),
    FacilityCategoryItem("TYRE_PUNCTURE", "Tyre & Puncture", Icons.Default.Build, 80.0),
    FacilityCategoryItem("REPAIR", "Vehicle Mechanics", Icons.Default.Build, 300.0),
    FacilityCategoryItem("FOOD", "Food Court / Dhaba", Icons.Default.Restaurant, 150.0),
    FacilityCategoryItem("MEDICAL", "Medical Aid / First Aid", Icons.Default.MedicalServices, 0.0),
    FacilityCategoryItem("RESTROOM", "Clean Restrooms", Icons.Default.Wc, 0.0),
    FacilityCategoryItem("WATER", "Drinking Water", Icons.Default.WaterDrop, 0.0),
    FacilityCategoryItem("PARKING", "Rest & Parking Area", Icons.Default.LocalParking, 0.0)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TollFacilitiesSheet(
    repository: TollMateRepository,
    journeyId: String,
    vehicleNumber: String,
    onBackClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val tolls by repository.observeTolls().collectAsState(initial = repository.getBundledTolls())
    val activeServices by repository.observeJourneyServices(journeyId).collectAsState(initial = emptyList())

    var selectedToll by remember { mutableStateOf<TollPlaza?>(null) }
    var selectedCategory by remember { mutableStateOf(AllFacilityCategories[0]) }
    var notesInput by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var successMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(tolls) {
        if (selectedToll == null && tolls.isNotEmpty()) {
            selectedToll = tolls[0]
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Toll Plaza Facilities & Services", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("facilities_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Select Toll Plaza Along Route",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tolls) { plaza ->
                        FilterChip(
                            selected = selectedToll?.tollId == plaza.tollId,
                            onClick = { selectedToll = plaza },
                            label = { Text(plaza.name) },
                            modifier = Modifier.testTag("chip_toll_${plaza.tollId}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Toll Info Card
                selectedToll?.let { plaza ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = plaza.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${plaza.highway} • Helpline: ${plaza.emergencyContact}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Available Amenities: " + plaza.facilities.joinToString(", "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Available Highway Facility Categories",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Facility Category Grid / List
            items(AllFacilityCategories) { cat ->
                val isSelected = selectedCategory.id == cat.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { selectedCategory = cat }
                        .testTag("facility_cat_${cat.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = cat.icon,
                                contentDescription = cat.name,
                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (cat.typicalCharge > 0) "Est. Charge: ₹${cat.typicalCharge.toInt()}" else "Complimentary / Direct Pay",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))

                // Request Submission Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Submit Service Request for ${selectedCategory.name}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = notesInput,
                            onValueChange = { notesInput = it },
                            label = { Text("Specific requirement or vehicle notes") },
                            placeholder = { Text("e.g. Left rear tyre slow leak, Need fast charger connector") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("facility_notes_input"),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 3
                        )

                        if (successMsg != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = successMsg ?: "",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                isSubmitting = true
                                successMsg = null
                                coroutineScope.launch {
                                    val res = repository.createServiceRequest(
                                        journeyId = journeyId,
                                        serviceCategory = selectedCategory.id,
                                        serviceName = selectedCategory.name,
                                        tollId = selectedToll?.tollId ?: "GEN-HIGHWAY",
                                        vehicleNumber = vehicleNumber,
                                        notes = notesInput,
                                        estimatedCharge = selectedCategory.typicalCharge
                                    )
                                    isSubmitting = false
                                    if (res.isSuccess) {
                                        successMsg = "Request ${res.getOrNull()} submitted with status: REQUESTED"
                                        notesInput = ""
                                    } else {
                                        successMsg = "Failed to submit request: ${res.exceptionOrNull()?.localizedMessage}"
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("submit_facility_request_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            enabled = !isSubmitting
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            } else {
                                Text("REQUEST ${selectedCategory.name.uppercase()}")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Active Requests for this journey
                if (activeServices.isNotEmpty()) {
                    Text(
                        text = "Requests Logged For This Journey (${activeServices.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            items(activeServices) { req ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${req.serviceName} (${req.requestId})",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            if (req.notes.isNotBlank()) {
                                Text(
                                    text = "Notes: ${req.notes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.secondary)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = req.status,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
