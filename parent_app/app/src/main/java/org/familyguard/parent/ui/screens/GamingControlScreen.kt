package org.familyguard.parent.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.familyguard.parent.data.api.ApiClient
import org.familyguard.parent.data.model.GamingOverviewDto
import org.familyguard.parent.data.model.UpdateGamingScheduleRequest
import org.familyguard.parent.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamingControlScreen(
    childId: String,
    onNavigateBack: () -> Unit
) {
    var gamingData by remember { mutableStateOf<GamingOverviewDto?>(null) }
    var gamingVaultMinutes by remember { mutableStateOf(60f) }
    var blockSchool by remember { mutableStateOf(true) }
    var blockBedtime by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val loadGamingOverview = {
        scope.launch {
            isLoading = true
            try {
                val token = ApiClient.currentAuthToken ?: return@launch
                val res = ApiClient.service.getGamingOverview(token, childId)
                if (res.isSuccessful && res.body() != null) {
                    val data = res.body()!!
                    gamingData = data
                    gamingVaultMinutes = data.dailyGamingLimitMinutes.toFloat()
                    blockSchool = data.blockGamesDuringSchool
                    blockBedtime = data.blockGamesDuringBedtime
                }
            } catch (e: Exception) {
                // Ignore transient error
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(childId) {
        loadGamingOverview()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gaming Control Center", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundLight)
                    .padding(padding)
                    .padding(16.dp)
            ) {
                // Shared Gaming Vault Hero Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SportsEsports, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Shared Gaming Vault", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Shared daily allowance across all games", fontSize = 12.sp, color = TextSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "${gamingVaultMinutes.toInt()} Minutes Daily Cap",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryBlue
                        )

                        Slider(
                            value = gamingVaultMinutes,
                            onValueChange = { gamingVaultMinutes = it },
                            valueRange = 0f..240f,
                            steps = 47,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Curfew Switches Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Gaming Curfew Rules", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Block during School Hours", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Auto-suspends games between 08:30 and 15:00", fontSize = 12.sp, color = TextSecondary)
                            }
                            Switch(checked = blockSchool, onCheckedChange = { blockSchool = it })
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Block during Bedtime", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Auto-suspends games between 21:00 and 07:00", fontSize = 12.sp, color = TextSecondary)
                            }
                            Switch(checked = blockBedtime, onCheckedChange = { blockBedtime = it })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        scope.launch {
                            isSaving = true
                            try {
                                val token = ApiClient.currentAuthToken ?: return@launch
                                val res = ApiClient.service.updateGamingSchedule(
                                    token = token,
                                    childId = childId,
                                    request = UpdateGamingScheduleRequest(
                                        dailyGamingLimitMinutes = gamingVaultMinutes.toInt(),
                                        blockGamesDuringSchool = blockSchool,
                                        blockGamesDuringBedtime = blockBedtime
                                    )
                                )
                                if (res.isSuccessful) {
                                    Toast.makeText(context, "Gaming policies synchronized!", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = SurfaceWhite, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Apply Gaming Policies", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Installed Games List
                Text("Installed Games (${gamingData?.games?.size ?: 0})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(8.dp))

                val games = gamingData?.games ?: emptyList()
                if (games.isEmpty()) {
                    Text("No games detected on the child's device yet.", color = TextSecondary, fontSize = 13.sp)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(games) { game ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.SportsEsports, contentDescription = null, tint = PrimaryBlue)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(game.appName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text(game.packageName, fontSize = 11.sp, color = TextSecondary)
                                    }
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(game.mode, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
