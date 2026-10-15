package com.example.ui.home

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Journey
import com.example.data.model.UserProfile
import com.example.data.repository.TollMateRepository

@Composable
fun HomeScreen(
    repository: TollMateRepository,
    userId: String,
    onNavigateToCreateJourney: () -> Unit,
    onNavigateToActiveJourney: (String) -> Unit,
    onNavigateToLiveToll: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAssistant: () -> Unit,
    onNavigateToReceipts: () -> Unit,
    onOpenEmergencyDialog: () -> Unit
) {
    val userProfile by repository.observeUserProfile(userId).collectAsState(initial = null)
    val activeJourney by repository.observeActiveJourney(userId).collectAsState(initial = null)

    Scaffold(
        topBar = {
            HomeTopBar(
                userProfile = userProfile,
                onProfileClick = onNavigateToProfile,
                onWalletClick = onNavigateToWallet
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
            // Hero Visual Banner
            HeroBannerCard(activeJourney = activeJourney, onResumeJourney = { journeyId ->
                onNavigateToActiveJourney(journeyId)
            })

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Actions: LET'S START & Live Toll Detection
            Text(
                text = "Journey Actions",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            // LET'S START Primary Action Button
            Button(
                onClick = {
                    if (activeJourney != null) {
                        onNavigateToActiveJourney(activeJourney?.journeyId ?: "")
                    } else {
                        onNavigateToCreateJourney()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("home_lets_start_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    imageVector = if (activeJourney != null) Icons.Default.DirectionsCar else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (activeJourney != null) "CONTINUE ACTIVE JOURNEY" else stringResource(R.string.start_journey),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Live Toll Detection Secondary Action Button
            OutlinedButton(
                onClick = onNavigateToLiveToll,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("home_live_toll_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.live_toll_detection),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Grid Features Section
            Text(
                text = "Highway Services & Tools",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    title = "Digital Wallet",
                    subtitle = "₹${userProfile?.walletBalance?.toInt() ?: 500}",
                    icon = Icons.Default.AccountBalanceWallet,
                    iconBg = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    testTag = "home_card_wallet",
                    onClick = onNavigateToWallet
                )
                QuickActionCard(
                    title = "AI Assistant",
                    subtitle = "Ask Route & Tolls",
                    icon = Icons.Default.SmartToy,
                    iconBg = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                    testTag = "home_card_assistant",
                    onClick = onNavigateToAssistant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    title = "Journey History",
                    subtitle = "Receipts & Logs",
                    icon = Icons.Default.Receipt,
                    iconBg = Color(0xFFFEF3C7),
                    iconTint = Color(0xFFB45309),
                    modifier = Modifier.weight(1f),
                    testTag = "home_card_receipts",
                    onClick = onNavigateToReceipts
                )
                QuickActionCard(
                    title = "Emergency SOS",
                    subtitle = "Dial 1033 / 112",
                    icon = Icons.Default.Emergency,
                    iconBg = Color(0xFFFEE2E2),
                    iconTint = Color(0xFFDC2626),
                    modifier = Modifier.weight(1f),
                    testTag = "home_card_emergency",
                    onClick = onOpenEmergencyDialog
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Mandatory Wallet Disclaimer Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Toll,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.wallet_disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    userProfile: UserProfile?,
    onProfileClick: () -> Unit,
    onWalletClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = userProfile?.displayName?.ifBlank { "Traveler" } ?: "Welcome, Traveler",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Wallet balance chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { onWalletClick() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("top_bar_wallet_chip")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = "Wallet",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "₹${userProfile?.walletBalance?.toInt() ?: 500}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onProfileClick,
                modifier = Modifier.testTag("top_bar_profile_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun HeroBannerCard(
    activeJourney: Journey?,
    onResumeJourney: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.highway_hero_1792075267416),
                contentDescription = "Highway Expressways",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                if (activeJourney != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.tertiary)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "ACTIVE: ${activeJourney.journeyId}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${activeJourney.startLocation} → ${activeJourney.destination}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Vehicle: ${activeJourney.vehicleNumber} • Tap to view dashboard",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                } else {
                    Text(
                        text = "NHAI Smart Expressway Assistant",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Live toll passage alerts, digital wallet & emergency SOS",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
        }
    }
}
