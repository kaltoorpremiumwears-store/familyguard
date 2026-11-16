package org.familyguard.child.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.familyguard.child.data.api.ChildApiClient
import org.familyguard.child.data.model.ClaimPairingRequest
import org.familyguard.child.dpc.ParentalDpcReceiver
import org.familyguard.child.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairingClaimScreen(
    onNavigateBack: () -> Unit,
    onPairingSuccess: () -> Unit
) {
    val context = LocalContext.current
    var pairingCodeInput by remember { mutableStateOf("") }
    var serverUrl by remember { mutableStateOf(ChildApiClient.baseUrl) }
    var showServerConfig by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pair with Parent App", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Enter 6-Character Pairing Code",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "Ask your parent to tap 'Pair Device' in their Parent App to display the 6-character code.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
            )

            OutlinedTextField(
                value = pairingCodeInput,
                onValueChange = { if (it.length <= 6) pairingCodeInput = it.uppercase() },
                label = { Text("Pairing Code (e.g. X7K9P2)") },
                leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null) },
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 4.sp,
                    textAlign = TextAlign.Center
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(text = errorMessage!!, color = DangerRed, fontSize = 13.sp, textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    if (pairingCodeInput.length < 6) {
                        errorMessage = "Please enter the complete 6-character code."
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null
                    scope.launch {
                        try {
                            val isDO = ParentalDpcReceiver.isDeviceOwner(context)
                            val res = ChildApiClient.service.claimPairing(
                                ClaimPairingRequest(
                                    pairingKey = pairingCodeInput.trim(),
                                    deviceName = "${Build.MANUFACTURER.capitalize()} ${Build.MODEL}",
                                    hardwareModel = Build.DEVICE,
                                    osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                                    deviceOwnerEnrolled = isDO
                                )
                            )

                            if (res.isSuccessful && res.body() != null) {
                                val body = res.body()!!
                                ChildApiClient.deviceAccessToken = "Bearer ${body.deviceAccessToken}"
                                ChildApiClient.enrolledChildName = body.child.name
                                ChildApiClient.deviceId = body.device.id
                                org.familyguard.child.sync.PolicySyncService.start(context)
                                org.familyguard.child.location.LocationTrackerService.start(context)
                                onPairingSuccess()
                            } else {
                                errorMessage = "Pairing failed: ${res.errorBody()?.string() ?: "Invalid code"}"
                            }
                        } catch (e: Exception) {
                            errorMessage = "Connection error: ${e.localizedMessage}. Check server address."
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ChildPrimaryGreen),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = SurfaceWhite, modifier = Modifier.size(24.dp))
                } else {
                    Text("Connect Device to Family", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            TextButton(onClick = { showServerConfig = !showServerConfig }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextSecondary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Backend Server: ${ChildApiClient.baseUrl}", fontSize = 12.sp, color = TextSecondary)
                }
            }

            if (showServerConfig) {
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = {
                        serverUrl = it
                        ChildApiClient.baseUrl = it
                    },
                    label = { Text("Server URL (e.g. http://10.0.2.2:3000)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
    }
}
