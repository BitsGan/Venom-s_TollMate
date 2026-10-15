package com.example.ui.journey

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Route
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.UserProfile
import com.example.data.repository.TollMateRepository
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateJourneyScreen(
    repository: TollMateRepository,
    userId: String,
    onBackClick: () -> Unit,
    onJourneyCreated: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userProfile by repository.observeUserProfile(userId).collectAsState(initial = null)

    // Form fields
    var vehicleNumber by remember { mutableStateOf("") }
    var startLocation by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var startLat by remember { mutableStateOf<Double?>(null) }
    var startLng by remember { mutableStateOf<Double?>(null) }

    // Validation & submission states
    var vehicleError by remember { mutableStateOf<String?>(null) }
    var startError by remember { mutableStateOf<String?>(null) }
    var destError by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var isFetchingLocation by remember { mutableStateOf(false) }
    var locationFeedback by remember { mutableStateOf<String?>(null) }

    // Generated Journey ID (Stage 2: Persistent ID for current attempt)
    var pendingJourneyId by remember { mutableStateOf(repository.generateUniqueJourneyId()) }

    // Prefill vehicle number from profile when available
    LaunchedEffect(userProfile) {
        if (vehicleNumber.isBlank() && !userProfile?.vehicleNumber.isNullOrBlank()) {
            vehicleNumber = userProfile?.vehicleNumber ?: ""
        }
    }

    // Location Permission Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchDeviceLocation(
                context = context,
                onLocationFound = { loc ->
                    isFetchingLocation = false
                    startLat = loc.latitude
                    startLng = loc.longitude
                    startLocation = "GPS (${String.format(Locale.US, "%.4f", loc.latitude)}, ${String.format(Locale.US, "%.4f", loc.longitude)}) - Highway Access Point"
                    startError = null
                    locationFeedback = "Current location detected successfully"
                },
                onError = { err ->
                    isFetchingLocation = false
                    locationFeedback = err
                }
            )
        } else {
            isFetchingLocation = false
            locationFeedback = "Location permission denied. Please enter starting location manually."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create New Journey", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("create_journey_back_button")) {
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Journey ID Badge
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Route,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "ASSIGNED JOURNEY ID",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = pendingJourneyId,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Vehicle Registration Field
            OutlinedTextField(
                value = vehicleNumber,
                onValueChange = { input ->
                    val clean = input.uppercase(Locale.US).replace(" ", "").replace("-", "")
                    vehicleNumber = clean
                    vehicleError = validateVehicleNumber(clean)
                },
                label = { Text("Vehicle Registration Number *") },
                placeholder = { Text("e.g. MH12AB1234 or DL01A1234") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.DirectionsCar, contentDescription = null)
                },
                isError = vehicleError != null,
                supportingText = {
                    if (vehicleError != null) {
                        Text(text = vehicleError ?: "", color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("Standard Indian registration format (State code + RTO + Series + 4 digits)")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("journey_vehicle_input"),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Next
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Starting Location Field with GPS Button
            OutlinedTextField(
                value = startLocation,
                onValueChange = {
                    startLocation = it
                    if (it.isNotBlank()) startError = null
                },
                label = { Text("Starting Location / Highway Entry *") },
                placeholder = { Text("e.g. Mumbai (Kalamboli Toll), Pune, Delhi") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            isFetchingLocation = true
                            locationFeedback = "Acquiring GPS location..."
                            val hasFine = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            val hasCoarse = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasFine || hasCoarse) {
                                fetchDeviceLocation(
                                    context = context,
                                    onLocationFound = { loc ->
                                        isFetchingLocation = false
                                        startLat = loc.latitude
                                        startLng = loc.longitude
                                        startLocation = "Near Lat ${String.format(Locale.US, "%.4f", loc.latitude)}, Lng ${String.format(Locale.US, "%.4f", loc.longitude)}"
                                        startError = null
                                        locationFeedback = "Current location updated"
                                    },
                                    onError = { err ->
                                        isFetchingLocation = false
                                        locationFeedback = err
                                    }
                                )
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        modifier = Modifier.testTag("journey_gps_button")
                    ) {
                        if (isFetchingLocation) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Use Current Location",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                isError = startError != null,
                supportingText = {
                    if (startError != null) {
                        Text(text = startError ?: "", color = MaterialTheme.colorScheme.error)
                    } else if (locationFeedback != null) {
                        Text(text = locationFeedback ?: "", color = MaterialTheme.colorScheme.primary)
                    } else {
                        Text("Tap the GPS crosshair to use device current location")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("journey_start_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Destination Field
            OutlinedTextField(
                value = destination,
                onValueChange = {
                    destination = it
                    if (it.isNotBlank()) destError = null
                },
                label = { Text("Destination / Highway Exit *") },
                placeholder = { Text("e.g. Pune, Jaipur, Bangalore, Hyderabad") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Flag, contentDescription = null)
                },
                isError = destError != null,
                supportingText = {
                    if (destError != null) {
                        Text(text = destError ?: "", color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("Destination city or toll exit")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("journey_dest_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Journey Details Review Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Journey Review",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ReviewRow(label = "Vehicle:", value = vehicleNumber.ifBlank { "Not specified" })
                    ReviewRow(label = "From:", value = startLocation.ifBlank { "Not specified" })
                    ReviewRow(label = "To:", value = destination.ifBlank { "Not specified" })
                    ReviewRow(label = "Toll Tracking:", value = "Automatic live plaza detection enabled")
                    ReviewRow(label = "Status:", value = "Will be saved as ACTIVE on start")
                }
            }

            if (generalError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = generalError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Start Journey Button with repeated-submission safeguards
            Button(
                onClick = {
                    // Comprehensive validation
                    val vClean = vehicleNumber.uppercase(Locale.US).replace(" ", "").replace("-", "")
                    val vErr = validateVehicleNumber(vClean)
                    val sErr = if (startLocation.isBlank()) "Starting location is required" else null
                    val dErr = if (destination.isBlank()) "Destination is required" else null
                    val sameErr = if (startLocation.isNotBlank() && destination.isNotBlank() &&
                        startLocation.trim().equals(destination.trim(), ignoreCase = true)
                    ) {
                        "Destination must differ from starting location"
                    } else null

                    vehicleError = vErr
                    startError = sErr
                    destError = dErr ?: sameErr

                    if (vErr == null && sErr == null && dErr == null && sameErr == null) {
                        isSubmitting = true
                        generalError = null
                        coroutineScope.launch {
                            val result = repository.createJourney(
                                journeyId = pendingJourneyId,
                                vehicleNumber = vClean,
                                startLocation = startLocation.trim(),
                                destination = destination.trim(),
                                startLat = startLat,
                                startLng = startLng
                            )
                            isSubmitting = false
                            if (result.isSuccess) {
                                // Save default vehicle to user profile if not set
                                if (userProfile?.vehicleNumber.isNullOrBlank()) {
                                    repository.updateVehicleNumber(vClean)
                                }
                                onJourneyCreated(pendingJourneyId)
                            } else {
                                generalError = "Failed to save journey: ${result.exceptionOrNull()?.localizedMessage ?: "Unknown error"}. You may retry."
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("start_journey_submit_button"),
                shape = RoundedCornerShape(14.dp),
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Saving Journey to Cloud...")
                } else {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CONFIRM & START JOURNEY",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun validateVehicleNumber(clean: String): String? {
    if (clean.isBlank()) return "Vehicle number is required"
    if (clean.length < 6 || clean.length > 11) return "Must be 6 to 10 alphanumeric characters (e.g. MH12AB1234)"
    // Standard Indian vehicle format: 2 letters (state) + 1-2 digits + optional 1-3 letters + 4 digits
    val regex = Regex("^[A-Z]{2}[0-9]{1,2}[A-Z]{0,3}[0-9]{4}$")
    val bhRegex = Regex("^[0-9]{2}BH[0-9]{4}[A-Z]{1,2}$") // Bharat series
    if (!clean.matches(regex) && !clean.matches(bhRegex)) {
        return "Invalid format. e.g. MH12AB1234, DL01A1234, 22BH1234AA"
    }
    return null
}

@SuppressLint("MissingPermission")
private fun fetchDeviceLocation(
    context: Context,
    onLocationFound: (Location) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        fusedClient.lastLocation
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    onLocationFound(loc)
                } else {
                    onError("Location unavailable. Please ensure GPS is active.")
                }
            }
            .addOnFailureListener { e ->
                onError("Failed to acquire GPS: ${e.localizedMessage}")
            }
    } catch (e: Exception) {
        onError("Location error: ${e.localizedMessage}")
    }
}
