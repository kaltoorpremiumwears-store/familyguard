package org.familyguard.child.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.familyguard.child.dpc.ParentalDpcReceiver
import org.familyguard.child.ui.theme.*

@Composable
fun EnrollmentScreen(
    onProceedToPairing: () -> Unit
) {
    val context = LocalContext.current
    val isDeviceOwner = ParentalDpcReceiver.isDeviceOwner(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = ChildPrimaryGreen,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Welcome to FamilyGuard",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Text(
            text = "A safe, transparent space for your digital day",
            fontSize = 14.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Device Management Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Device Owner Status", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        if (isDeviceOwner) "Android Enterprise Fully Managed" else "Standard Device Admin Mode",
                        fontSize = 12.sp,
                        color = if (isDeviceOwner) ChildPrimaryGreen else WarningOrange
                    )
                }
                AssistChip(
                    onClick = {},
                    label = { Text(if (isDeviceOwner) "ENROLLED" else "UNPROVISIONED") }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Transparency Charter
        Text(
            text = "Your Privacy & Transparency Charter",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = TextPrimary,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("What your parents can see:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ChildPrimaryGreen)
                Spacer(modifier = Modifier.height(6.dp))
                CharterItem(icon = Icons.Default.Check, text = "Daily screen time & game limits", isPositive = true)
                CharterItem(icon = Icons.Default.Check, text = "Bedtime and school hour schedules", isPositive = true)
                CharterItem(icon = Icons.Default.Check, text = "Location to make sure you are safe", isPositive = true)

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text("What your parents CANNOT do:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DangerRed)
                Spacer(modifier = Modifier.height(6.dp))
                CharterItem(icon = Icons.Default.Close, text = "Cannot secretly view your camera", isPositive = false)
                CharterItem(icon = Icons.Default.Close, text = "Cannot secretly record your microphone", isPositive = false)
                CharterItem(icon = Icons.Default.Close, text = "Cannot read private chat messages or passwords", isPositive = false)
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = onProceedToPairing,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ChildPrimaryGreen)
        ) {
            Text("Proceed to Device Pairing", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun CharterItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    isPositive: Boolean
) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isPositive) ChildPrimaryGreen else DangerRed,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 12.sp, color = TextPrimary)
    }
}
