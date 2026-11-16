package org.familyguard.parent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.familyguard.parent.data.api.ApiClient
import org.familyguard.parent.data.model.GeneratePairingRequest
import org.familyguard.parent.data.model.PairingResponse
import org.familyguard.parent.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairingScreen(
    childId: String,
    onNavigateBack: () -> Unit,
    onPairingCompleted: () -> Unit
) {
    var pairingData by remember { mutableStateOf<PairingResponse?>(null) }
    var isPairedSuccessfully by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(childId) {
        try {
            val token = ApiClient.currentAuthToken ?: return@LaunchedEffect
            val res = ApiClient.service.generatePairing(token, GeneratePairingRequest(childId))
            if (res.isSuccessful && res.body() != null) {
                pairingData = res.body()
            } else {
                errorMessage = "Could not generate pairing token: ${res.errorBody()?.string()}"
            }
        } catch (e: Exception) {
            errorMessage = "Network error: ${e.localizedMessage}"
        } finally {
            isLoading = false
        }
    }

    // Auto-polling status listener
    LaunchedEffect(pairingData) {
        val pairing = pairingData ?: return@LaunchedEffect
        val token = ApiClient.currentAuthToken ?: return@LaunchedEffect

        while (!isPairedSuccessfully) {
            delay(3000)
            try {
                val statusRes = ApiClient.service.getPairingStatus(token, pairing.pairingId)
                if (statusRes.isSuccessful && statusRes.body() != null) {
                    val status = statusRes.body()!!
                    if (status.status == "ACTIVE") {
                        isPairedSuccessfully = true
                        delay(2000)
                        onPairingCompleted()
                        break
                    }
                }
            } catch (e: Exception) {
                // Ignore transient polling failure
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pair Child Device", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(padding)
                .padding(24.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = PrimaryBlue)
            } else if (errorMessage != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(errorMessage!!, color = DangerRed, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onNavigateBack) { Text("Go Back") }
                }
            } else if (isPairedSuccessfully) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Device Paired Successfully!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        "The device is now enrolled and connected to FamilyGuard.",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (pairingData != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Pair with ${pairingData!!.child.name}'s Device",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Text(
                        text = "Open FamilyGuard Child App on the target device and scan this QR code or enter the 6-character code.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                    )

                    // QR Visual Simulation Container
                    Card(
                        modifier = Modifier
                            .size(220.dp)
                            .border(2.dp, PrimaryBlue, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Icon(
                                Icons.Default.QrCode,
                                contentDescription = "Pairing QR",
                                modifier = Modifier
                                    .size(160.dp)
                                    .align(Alignment.Center),
                                tint = PrimaryBlue
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("OR ENTER PAIRING CODE:", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Text(
                            text = pairingData!!.pairingCode.chunked(3).joinToString(" "),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = PrimaryBlue,
                            letterSpacing = 4.sp,
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PrimaryBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Waiting for child device handshake... (Valid for 15 mins)",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}
