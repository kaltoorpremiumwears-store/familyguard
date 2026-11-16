package org.familyguard.parent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.familyguard.parent.data.api.ApiClient
import org.familyguard.parent.data.model.ChildDto
import org.familyguard.parent.data.model.UpdateScreenTimeRequest
import org.familyguard.parent.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToCreateChild: () -> Unit,
    onNavigateToPairing: (childId: String) -> Unit,
    onNavigateToProvisioningGuide: () -> Unit,
    onNavigateToAppManagement: (childId: String) -> Unit,
    onNavigateToGamingControl: (childId: String) -> Unit,
    onNavigateToReports: (childId: String) -> Unit,
    onNavigateToWebFiltering: (childId: String) -> Unit,
    onNavigateToYouTubeStudio: (childId: String) -> Unit,
    onNavigateToLocation: (childId: String) -> Unit,
    onNavigateToNotifications: () -> Unit,
    onLogout: () -> Unit
) {
    var children by remember { mutableStateOf<List<ChildDto>>(emptyList()) }
    var selectedChildId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val fetchChildren = {
        scope.launch {
            isLoading = true
            try {
                val token = ApiClient.currentAuthToken ?: return@launch
                val res = ApiClient.service.getChildren(token)
                if (res.isSuccessful && res.body() != null) {
                    children = res.body()!!
                    if (selectedChildId == null && children.isNotEmpty()) {
                        selectedChildId = children.first().id
                    }
                }
            } catch (e: Exception) {
                actionMessage = "Error loading profiles: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchChildren()
    }

    val selectedChild = children.find { it.id == selectedChildId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(ApiClient.currentParentName ?: "Guardian Portal", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Family: ${ApiClient.currentFamilyCode ?: "FAM-CODE"}", fontSize = 12.sp, color = TextSecondary)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToNotifications) {
                        Icon(Icons.Default.Notifications, contentDescription = "Alerts", tint = PrimaryBlue)
                    }
                    IconButton(onClick = onNavigateToProvisioningGuide) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = "Device Owner Guide", tint = PrimaryBlue)
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Sign Out")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Child Switcher Carousel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Children",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onNavigateToCreateChild) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Child", fontSize = 13.sp)
                }
            }

            if (children.isEmpty() && !isLoading) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.ChildCare, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No Child Profiles Registered", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Create your first child profile to configure screen time rules and pair devices.", color = TextSecondary, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onNavigateToCreateChild,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text("Create Profile Now")
                        }
                    }
                }
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(children) { child ->
                        val isSelected = child.id == selectedChildId
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedChildId = child.id },
                            label = { Text(child.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) PrimaryBlue else PrimaryBlueVariant)
                                ) {
                                    Text(
                                        text = child.name.take(1).uppercase(),
                                        color = SurfaceWhite,
                                        fontSize = 12.sp,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }
                        )
                    }
                }

                if (selectedChild != null) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Screen Time Hero Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Today's Screen Time", fontSize = 14.sp, color = TextSecondary)
                                    val limit = selectedChild.screenTimeSchedule?.dailyLimitMinutes ?: 180
                                    val bonus = selectedChild.screenTimeSchedule?.bonusMinutesToday ?: 0
                                    Text(
                                        text = "${(limit + bonus) / 60}h ${(limit + bonus) % 60}m Daily Allowance",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }

                                val isPaused = selectedChild.screenTimeSchedule?.isInstantPause ?: false
                                AssistChip(
                                    onClick = {},
                                    label = { Text(if (isPaused) "PAUSED" else "ACTIVE") },
                                    colors = AssistChipDefaults.assistChipColors(
                                        labelColor = if (isPaused) DangerRed else SuccessGreen
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            LinearProgressIndicator(
                                progress = { 0.45f }, // 45% of daily allowance consumed
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = PrimaryBlue,
                                trackColor = BackgroundLight
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Quick Actions Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val isPaused = selectedChild.screenTimeSchedule?.isInstantPause ?: false
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            val token = ApiClient.currentAuthToken ?: return@launch
                                            val newPauseState = !isPaused
                                            ApiClient.service.updateScreenTime(
                                                token = token,
                                                childId = selectedChild.id,
                                                request = UpdateScreenTimeRequest(isInstantPause = newPauseState)
                                            )
                                            fetchChildren()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        contentDescription = null,
                                        tint = if (isPaused) SuccessGreen else DangerRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isPaused) "Resume" else "Pause Device")
                                }

                                Button(
                                    onClick = {
                                        scope.launch {
                                            val token = ApiClient.currentAuthToken ?: return@launch
                                            val currentBonus = selectedChild.screenTimeSchedule?.bonusMinutesToday ?: 0
                                            ApiClient.service.updateScreenTime(
                                                token = token,
                                                childId = selectedChild.id,
                                                request = UpdateScreenTimeRequest(bonusMinutesToday = currentBonus + 15)
                                            )
                                            fetchChildren()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                ) {
                                    Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("+15m Bonus")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Managed Devices Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Enrolled Devices", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.weight(1f))
                                TextButton(onClick = { onNavigateToPairing(selectedChild.id) }) {
                                    Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pair Device", fontSize = 13.sp)
                                }
                            }

                            val devices = selectedChild.devices ?: emptyList()
                            if (devices.isEmpty()) {
                                Text(
                                    text = "No device paired yet. Click 'Pair Device' to generate pairing QR code.",
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                devices.forEach { dev ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = PrimaryBlue)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(dev.deviceName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text(
                                                "${dev.hardwareModel} • ${if (dev.deviceOwnerEnrolled) "Device Owner (DPC)" else "Standard"}",
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                        }
                                        AssistChip(
                                            onClick = {},
                                            label = { Text(if (dev.isOnline) "Online" else "Offline") }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Parental Modules Action Hub
                    Text(
                        text = "Parental Management Modules",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedCard(
                            onClick = { onNavigateToAppManagement(selectedChild.id) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Apps, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Apps", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Allow / Block", fontSize = 11.sp, color = TextSecondary)
                            }
                        }

                        OutlinedCard(
                            onClick = { onNavigateToGamingControl(selectedChild.id) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.SportsEsports, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Gaming", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Vault & Curfew", fontSize = 11.sp, color = TextSecondary)
                            }
                        }

                        OutlinedCard(
                            onClick = { onNavigateToReports(selectedChild.id) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.BarChart, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Reports", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Daily & Trends", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedCard(
                            onClick = { onNavigateToWebFiltering(selectedChild.id) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Web Filter", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("SafeSearch & URLs", fontSize = 11.sp, color = TextSecondary)
                            }
                        }

                        OutlinedCard(
                            onClick = { onNavigateToYouTubeStudio(selectedChild.id) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.PlayCircle, contentDescription = null, tint = DangerRed, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("YouTube", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("SafeTube Library", fontSize = 11.sp, color = TextSecondary)
                            }
                        }

                        OutlinedCard(
                            onClick = { onNavigateToLocation(selectedChild.id) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Radar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Map & SafeZones", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
