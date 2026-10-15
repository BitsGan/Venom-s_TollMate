package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.repository.TollMateRepository
import com.example.ui.assistant.TravelAssistantScreen
import com.example.ui.auth.SignInScreen
import com.example.ui.emergency.EmergencyDialog
import com.example.ui.home.HomeScreen
import com.example.ui.journey.ActiveJourneyDashboard
import com.example.ui.journey.CreateJourneyScreen
import com.example.ui.map.LiveTollMapScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.receipt.JourneyReceiptScreen
import com.example.ui.services.TollFacilitiesSheet
import com.example.ui.theme.TollMateTheme
import com.example.ui.wallet.WalletScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = TollMateRepository(this)

        setContent {
            TollMateTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRoot(repository = repository)
                }
            }
        }
    }
}

@Composable
fun AppRoot(repository: TollMateRepository) {
    val coroutineScope = rememberCoroutineScope()
    var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    LaunchedEffect(currentUser) {
        val user = currentUser
        if (user != null) {
            coroutineScope.launch {
                repository.createOrUpdateProfileOnLogin(
                    userId = user.uid,
                    email = user.email ?: "",
                    displayName = user.displayName ?: "Traveler"
                )
            }
        }
    }

    if (currentUser == null) {
        SignInScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        AppNavigation(
            repository = repository,
            userId = currentUser!!.uid
        )
    }
}

@Composable
fun AppNavigation(
    repository: TollMateRepository,
    userId: String
) {
    var currentScreen by remember { mutableStateOf("home") }
    var selectedJourneyId by remember { mutableStateOf("") }
    var selectedVehicleNumber by remember { mutableStateOf("") }
    var showEmergencyDialog by remember { mutableStateOf(false) }

    // Screen Router
    when (currentScreen) {
        "home" -> {
            HomeScreen(
                repository = repository,
                userId = userId,
                onNavigateToCreateJourney = { currentScreen = "create_journey" },
                onNavigateToActiveJourney = { jId ->
                    selectedJourneyId = jId
                    currentScreen = "active_journey"
                },
                onNavigateToLiveToll = { currentScreen = "live_toll" },
                onNavigateToWallet = { currentScreen = "wallet" },
                onNavigateToProfile = { currentScreen = "profile" },
                onNavigateToAssistant = { currentScreen = "assistant" },
                onNavigateToReceipts = { currentScreen = "receipts" },
                onOpenEmergencyDialog = {
                    showEmergencyDialog = true
                }
            )
        }

        "create_journey" -> {
            BackHandler { currentScreen = "home" }
            CreateJourneyScreen(
                repository = repository,
                userId = userId,
                onBackClick = { currentScreen = "home" },
                onJourneyCreated = { newJourneyId ->
                    selectedJourneyId = newJourneyId
                    currentScreen = "active_journey"
                }
            )
        }

        "active_journey" -> {
            BackHandler { currentScreen = "home" }
            ActiveJourneyDashboard(
                repository = repository,
                userId = userId,
                journeyId = selectedJourneyId,
                onBackClick = { currentScreen = "home" },
                onNavigateToLiveToll = { currentScreen = "live_toll" },
                onOpenFacilitiesMenu = { currentScreen = "facilities" },
                onOpenEmergencySOS = { jId, vehicle ->
                    selectedJourneyId = jId
                    selectedVehicleNumber = vehicle
                    showEmergencyDialog = true
                },
                onJourneyCompleted = { jId ->
                    selectedJourneyId = jId
                    currentScreen = "receipts"
                }
            )
        }

        "facilities" -> {
            BackHandler { currentScreen = "active_journey" }
            TollFacilitiesSheet(
                repository = repository,
                journeyId = selectedJourneyId,
                vehicleNumber = selectedVehicleNumber,
                onBackClick = { currentScreen = "active_journey" }
            )
        }

        "live_toll" -> {
            BackHandler { currentScreen = "home" }
            LiveTollMapScreen(
                repository = repository,
                userId = userId,
                onBackClick = { currentScreen = "home" }
            )
        }

        "wallet" -> {
            BackHandler { currentScreen = "home" }
            WalletScreen(
                repository = repository,
                userId = userId,
                onBackClick = { currentScreen = "home" }
            )
        }

        "profile" -> {
            BackHandler { currentScreen = "home" }
            ProfileScreen(
                repository = repository,
                userId = userId,
                onBackClick = { currentScreen = "home" },
                onSignOutComplete = {
                    currentScreen = "home"
                }
            )
        }

        "assistant" -> {
            BackHandler { currentScreen = "home" }
            TravelAssistantScreen(
                currentRouteContext = "Active Journey: $selectedJourneyId",
                onBackClick = { currentScreen = "home" }
            )
        }

        "receipts" -> {
            BackHandler { currentScreen = "home" }
            JourneyReceiptScreen(
                repository = repository,
                userId = userId,
                highlightJourneyId = selectedJourneyId,
                onBackClick = { currentScreen = "home" }
            )
        }
    }

    // Global Emergency Dialog
    if (showEmergencyDialog) {
        EmergencyDialog(
            repository = repository,
            journeyId = selectedJourneyId,
            vehicleNumber = selectedVehicleNumber,
            onDismiss = { showEmergencyDialog = false }
        )
    }
}
