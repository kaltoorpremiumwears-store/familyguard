package org.familyguard.child.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.familyguard.child.data.api.ChildApiClient
import org.familyguard.child.data.model.ActivePolicyResponse
import org.familyguard.child.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildStatusScreen(
    onNavigateToSafeTube: () -> Unit,
    onUnpairOrReset: () -> Unit
) {
    val context = LocalContext.current
    var policy by remember { mutableStateOf<ActivePolicyResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var requestTimeSent by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val fetchPolicy = {
        scope.launch {
            isLoading = true
            try {
                val token = ChildApiClient.deviceAccessToken ?: return@launch
                val res = ChildApiClient.service.syncPolicies(token)
                if (res.isSuccessful && res.body() != null) {
                    policy = res.body()
                }
            } catch (e: Exception) {
                // Ignore transient sync failure
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchPolicy()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FamilyGuard Active", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { fetchPolicy() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync Policy")
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
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Protection Active Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(ChildPrimaryGreen.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = ChildPrimaryGreen,
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.Center)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Hi, ${ChildApiClient.enrolledChildName ?: "there"}!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Parental Management Active & Protecting",
                            fontSize = 12.sp,
                            color = ChildPrimaryGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Remaining Time Gauge
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Remaining Screen Time Today",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val schedule = policy?.screenTime
                    val limitMinutes = schedule?.dailyLimitMinutes ?: 180
                    val bonusMinutes = schedule?.bonusMinutesToday ?: 0
                    val totalMinutes = limitMinutes + bonusMinutes
                    val isPaused = schedule?.isInstantPause ?: false

                    Text(
                        text = if (isPaused) "PAUSED" else "${totalMinutes / 60}h ${totalMinutes % 60}m",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isPaused) DangerRed else ChildPrimaryGreen
                    )

                    Text(
                        text = if (isPaused) "Your parent paused screen time" else "Daily cap + bonus included",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (requestTimeSent) {
                        Text(
                            "Request sent to parent's phone!",
                            color = ChildPrimaryGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    } else {
                        OutlinedButton(
                            onClick = { requestTimeSent = true },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.HourglassTop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ask Mom / Dad for More Time", fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SafeTube Player Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = ChildPrimaryGreen,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("FamilyGuard SafeTube", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Watch parent-approved channels and educational videos", fontSize = 12.sp, color = TextSecondary)
                    }
                    Button(
                        onClick = onNavigateToSafeTube,
                        colors = ButtonDefaults.buttonColors(containerColor = ChildPrimaryGreen)
                    ) {
                        Text("Watch")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Safety & Emergency SOS (Never blocked)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Emergency & Safety", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        "Essential safety functions can NEVER be restricted or locked.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                    )

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:911"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Emergency Call (911 / 112)", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
