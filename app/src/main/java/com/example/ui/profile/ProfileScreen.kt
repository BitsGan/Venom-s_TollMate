package com.example.ui.profile

import android.content.Context
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.data.repository.TollMateRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    repository: TollMateRepository,
    userId: String,
    onBackClick: () -> Unit,
    onSignOutComplete: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val userProfile by repository.observeUserProfile(userId).collectAsState(initial = null)

    var vehicleInput by remember { mutableStateOf("") }
    var isSavingVehicle by remember { mutableStateOf(false) }
    var saveFeedback by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userProfile) {
        if (vehicleInput.isBlank() && !userProfile?.vehicleNumber.isNullOrBlank()) {
            vehicleInput = userProfile?.vehicleNumber ?: ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("profile_back_button")) {
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = userProfile?.displayName?.ifBlank { "TollMate Traveler" } ?: "TollMate Traveler",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = userProfile?.email?.ifBlank { Firebase.auth.currentUser?.email ?: "" } ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // User Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Account Identity",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ProfileInfoRow(icon = Icons.Default.Fingerprint, label = "UID", value = userId.take(16) + "...")
                    ProfileInfoRow(icon = Icons.Default.Email, label = "Email", value = userProfile?.email ?: "N/A")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Vehicle Number Card (Saved in Firestore profile per requirements)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Registered Vehicle Details",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Stored in your Firestore cloud profile and prefilled when starting journeys.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = vehicleInput,
                        onValueChange = {
                            vehicleInput = it.uppercase(Locale.US).replace(" ", "").replace("-", "")
                        },
                        label = { Text("Default Vehicle Number") },
                        placeholder = { Text("e.g. MH12AB1234") },
                        leadingIcon = {
                            Icon(Icons.Default.DirectionsCar, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_vehicle_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (saveFeedback != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = saveFeedback ?: "",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            isSavingVehicle = true
                            saveFeedback = null
                            coroutineScope.launch {
                                try {
                                    repository.updateVehicleNumber(vehicleInput)
                                    saveFeedback = "Vehicle number updated successfully!"
                                } catch (e: Exception) {
                                    saveFeedback = "Update failed: ${e.localizedMessage}"
                                } finally {
                                    isSavingVehicle = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_save_vehicle_button"),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isSavingVehicle
                    ) {
                        if (isSavingVehicle) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Vehicle Number to Cloud")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Logout Button
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        Firebase.auth.signOut()
                        try {
                            credentialManager.clearCredentialState(ClearCredentialStateRequest())
                        } catch (e: Exception) {
                            // ignore
                        }
                        onSignOutComplete()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("profile_logout_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("LOG OUT OF TOLLMATE", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
        Text(text = value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
    }
}
