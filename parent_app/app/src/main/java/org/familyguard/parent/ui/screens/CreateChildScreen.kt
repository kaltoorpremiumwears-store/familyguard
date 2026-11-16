package org.familyguard.parent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.familyguard.parent.data.api.ApiClient
import org.familyguard.parent.data.model.CreateChildRequest
import org.familyguard.parent.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChildScreen(
    onNavigateBack: () -> Unit,
    onChildCreated: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("2014-05-12") }
    var selectedPreset by remember { mutableStateOf("TWEEN") } // EARLY, TWEEN, TEEN
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Child Profile", fontWeight = FontWeight.Bold) },
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
                .padding(24.dp)
        ) {
            Text(
                text = "Child Information",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "Creating a profile allows you to configure individual screen time rules, app limits, and pair devices.",
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Child Name / Nickname") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = birthDate,
                onValueChange = { birthDate = it },
                label = { Text("Birth Date (YYYY-MM-DD)") },
                leadingIcon = { Icon(Icons.Default.Cake, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Age Group Preset:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedPreset == "EARLY",
                    onClick = { selectedPreset = "EARLY" },
                    label = { Text("Early (4-8)") }
                )
                FilterChip(
                    selected = selectedPreset == "TWEEN",
                    onClick = { selectedPreset = "TWEEN" },
                    label = { Text("Tween (9-12)") }
                )
                FilterChip(
                    selected = selectedPreset == "TEEN",
                    onClick = { selectedPreset = "TEEN" },
                    label = { Text("Teen (13-17)") }
                )
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = errorMessage!!, color = DangerRed, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Please enter the child's name."
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null
                    scope.launch {
                        try {
                            val token = ApiClient.currentAuthToken ?: return@launch
                            val res = ApiClient.service.createChild(
                                token = token,
                                request = CreateChildRequest(name = name.trim(), birthDate = birthDate.trim())
                            )
                            if (res.isSuccessful) {
                                onChildCreated()
                            } else {
                                errorMessage = "Failed to create profile: ${res.errorBody()?.string()}"
                            }
                        } catch (e: Exception) {
                            errorMessage = "Error: ${e.localizedMessage}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = SurfaceWhite, modifier = Modifier.size(24.dp))
                } else {
                    Text("Save Child Profile", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
