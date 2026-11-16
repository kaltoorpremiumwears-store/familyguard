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
import org.familyguard.parent.data.model.AddWebsiteRuleRequest
import org.familyguard.parent.data.model.UpdateWebSafetyRequest
import org.familyguard.parent.data.model.WebSafetyOverviewDto
import org.familyguard.parent.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebFilteringScreen(
    childId: String,
    onNavigateBack: () -> Unit
) {
    var webData by remember { mutableStateOf<WebSafetyOverviewDto?>(null) }
    var safeSearch by remember { mutableStateOf(true) }
    var blockAdult by remember { mutableStateOf(true) }
    var blockGambling by remember { mutableStateOf(true) }
    var blockViolence by remember { mutableStateOf(true) }
    var blockMalicious by remember { mutableStateOf(true) }
    var blockUnapprovedBrowsers by remember { mutableStateOf(true) }

    var newDomainInput by remember { mutableStateOf("") }
    var isNewDomainBlocked by remember { mutableStateOf(true) }

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val loadWebSafety = {
        scope.launch {
            isLoading = true
            try {
                val token = ApiClient.currentAuthToken ?: return@launch
                val res = ApiClient.service.getWebSafety(token, childId)
                if (res.isSuccessful && res.body() != null) {
                    val data = res.body()!!
                    webData = data
                    safeSearch = data.config.safeSearchEnabled
                    blockAdult = data.config.blockAdultContent
                    blockGambling = data.config.blockGambling
                    blockViolence = data.config.blockViolence
                    blockMalicious = data.config.blockMalicious
                    blockUnapprovedBrowsers = data.config.unapprovedBrowsersBlocked
                }
            } catch (e: Exception) {
                // Ignore transient error
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(childId) {
        loadWebSafety()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Web & Browser Filtering", fontWeight = FontWeight.Bold) },
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundLight)
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Global Search & Protection Toggles
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Automated Search & Domain Filters", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(10.dp))

                            FilterToggleRow(
                                title = "Force SafeSearch",
                                subtitle = "Enforces strict SafeSearch on Google, Bing & Yahoo",
                                checked = safeSearch,
                                onCheckedChange = { safeSearch = it }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            FilterToggleRow(
                                title = "Block Adult Websites",
                                subtitle = "Filters explicit, pornographic, and 18+ content",
                                checked = blockAdult,
                                onCheckedChange = { blockAdult = it }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            FilterToggleRow(
                                title = "Block Gambling & Betting",
                                subtitle = "Prevents access to casinos, betting, and lotteries",
                                checked = blockGambling,
                                onCheckedChange = { blockGambling = it }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            FilterToggleRow(
                                title = "Block Malware & Phishing",
                                subtitle = "Shields against known malicious and scam domains",
                                checked = blockMalicious,
                                onCheckedChange = { blockMalicious = it }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            FilterToggleRow(
                                title = "Lock Down Unmanaged Browsers",
                                subtitle = "Suspends Firefox, Opera, Tor so filtering cannot be bypassed",
                                checked = blockUnapprovedBrowsers,
                                onCheckedChange = { blockUnapprovedBrowsers = it }
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    scope.launch {
                                        isSaving = true
                                        try {
                                            val token = ApiClient.currentAuthToken ?: return@launch
                                            val res = ApiClient.service.updateWebSafety(
                                                token = token,
                                                childId = childId,
                                                request = UpdateWebSafetyRequest(
                                                    safeSearchEnabled = safeSearch,
                                                    blockAdultContent = blockAdult,
                                                    blockGambling = blockGambling,
                                                    blockViolence = blockViolence,
                                                    blockMalicious = blockMalicious,
                                                    unapprovedBrowsersBlocked = blockUnapprovedBrowsers
                                                )
                                            )
                                            if (res.isSuccessful) {
                                                Toast.makeText(context, "Web safety settings pushed to device!", Toast.LENGTH_SHORT).show()
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
                                    .height(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                enabled = !isSaving
                            ) {
                                Text("Apply Web Safety Policies", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Custom Domain Whitelist / Blacklist Manager
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Custom Domain Rules", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Explicitly allow or block specific websites", fontSize = 12.sp, color = TextSecondary)

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newDomainInput,
                                    onValueChange = { newDomainInput = it },
                                    placeholder = { Text("e.g. reddit.com") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = {
                                        if (newDomainInput.isNotBlank()) {
                                            scope.launch {
                                                val token = ApiClient.currentAuthToken ?: return@launch
                                                ApiClient.service.addWebsiteRule(
                                                    token = token,
                                                    childId = childId,
                                                    request = AddWebsiteRuleRequest(
                                                        domain = newDomainInput.trim(),
                                                        isBlocked = isNewDomainBlocked
                                                    )
                                                )
                                                newDomainInput = ""
                                                loadWebSafety()
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isNewDomainBlocked) DangerRed else SuccessGreen
                                    )
                                ) {
                                    Text(if (isNewDomainBlocked) "Block" else "Allow")
                                }
                            }

                            Row(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = isNewDomainBlocked,
                                    onClick = { isNewDomainBlocked = true },
                                    label = { Text("Block Rule") }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                FilterChip(
                                    selected = !isNewDomainBlocked,
                                    onClick = { isNewDomainBlocked = false },
                                    label = { Text("Whitelist Rule") }
                                )
                            }
                        }
                    }
                }

                // Domain Rules List
                val rules = webData?.websiteRules ?: emptyList()
                items(rules) { rule ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (rule.isBlocked) Icons.Default.Block else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (rule.isBlocked) DangerRed else SuccessGreen
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rule.domain, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(if (rule.isBlocked) "Blocked Domain" else "Always Allowed Whitelist", fontSize = 11.sp, color = TextSecondary)
                            }
                            IconButton(onClick = {
                                scope.launch {
                                    val token = ApiClient.currentAuthToken ?: return@launch
                                    ApiClient.service.deleteWebsiteRule(token, childId, rule.id)
                                    loadWebSafety()
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
            Text(subtitle, fontSize = 12.sp, color = TextSecondary)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
