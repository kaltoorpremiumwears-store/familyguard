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
import org.familyguard.parent.data.model.AddYouTubeItemRequest
import org.familyguard.parent.data.model.UpdateWebSafetyRequest
import org.familyguard.parent.data.model.YouTubeLibraryOverviewDto
import org.familyguard.parent.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeStudioScreen(
    childId: String,
    onNavigateBack: () -> Unit
) {
    var ytData by remember { mutableStateOf<YouTubeLibraryOverviewDto?>(null) }
    var youtubeMode by remember { mutableStateOf("SAFETUBE") }
    var dailyLimitMinutes by remember { mutableStateOf(60f) }

    var customExternalId by remember { mutableStateOf("") }
    var customTitle by remember { mutableStateOf("") }
    var customType by remember { mutableStateOf("CHANNEL") }

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val loadLibrary = {
        scope.launch {
            isLoading = true
            try {
                val token = ApiClient.currentAuthToken ?: return@launch
                val res = ApiClient.service.getYouTubeLibrary(token, childId)
                if (res.isSuccessful && res.body() != null) {
                    val data = res.body()!!
                    ytData = data
                    youtubeMode = data.youtubeMode
                    dailyLimitMinutes = data.dailyLimitMinutes.toFloat()
                }
            } catch (e: Exception) {
                // Ignore transient error
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(childId) {
        loadLibrary()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("YouTube Content Studio", fontWeight = FontWeight.Bold) },
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
                // Mode & Time Budget Hero Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text("YouTube Experience & Limits", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Select how your child accesses approved YouTube content", fontSize = 12.sp, color = TextSecondary)

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = youtubeMode == "SAFETUBE",
                                    onClick = { youtubeMode = "SAFETUBE" },
                                    label = { Text("SafeTube Player (Recommended)") },
                                    leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text("Daily YouTube Allowance: ${dailyLimitMinutes.toInt()} Minutes", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Slider(
                                value = dailyLimitMinutes,
                                onValueChange = { dailyLimitMinutes = it },
                                valueRange = 0f..180f,
                                steps = 35,
                                modifier = Modifier.fillMaxWidth()
                            )

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
                                                    youtubeMode = youtubeMode,
                                                    dailyYouTubeLimitMinutes = dailyLimitMinutes.toInt()
                                                )
                                            )
                                            if (res.isSuccessful) {
                                                Toast.makeText(context, "YouTube settings updated & pushed!", Toast.LENGTH_SHORT).show()
                                            }
                                        } finally {
                                            isSaving = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) {
                                Text("Save YouTube Rules")
                            }
                        }
                    }
                }

                // Curated Safe Educational Channels
                item {
                    Text("Curated Educational Channels (1-Click Add)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    val suggestions = ytData?.curatedSuggestions ?: emptyList()
                    suggestions.forEach { channel ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.School, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(channel.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(channel.description, fontSize = 11.sp, color = TextSecondary, maxLines = 2)
                                }
                                TextButton(onClick = {
                                    scope.launch {
                                        val token = ApiClient.currentAuthToken ?: return@launch
                                        ApiClient.service.addYouTubeItem(
                                            token = token,
                                            childId = childId,
                                            request = AddYouTubeItemRequest(
                                                type = "CHANNEL",
                                                externalId = channel.channelId,
                                                title = channel.title,
                                                description = channel.description
                                            )
                                        )
                                        loadLibrary()
                                    }
                                }) {
                                    Text("+ Add")
                                }
                            }
                        }
                    }
                }

                // Custom Channel / Video Form
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Add Custom Channel or Video", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = customTitle,
                                onValueChange = { customTitle = it },
                                label = { Text("Title / Label (e.g. Science Max)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = customExternalId,
                                onValueChange = { customExternalId = it },
                                label = { Text("Channel ID (UC...) or Video ID (11 chars)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilterChip(
                                    selected = customType == "CHANNEL",
                                    onClick = { customType = "CHANNEL" },
                                    label = { Text("Channel") }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                FilterChip(
                                    selected = customType == "VIDEO",
                                    onClick = { customType = "VIDEO" },
                                    label = { Text("Single Video") }
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Button(
                                    onClick = {
                                        if (customTitle.isNotBlank() && customExternalId.isNotBlank()) {
                                            scope.launch {
                                                val token = ApiClient.currentAuthToken ?: return@launch
                                                ApiClient.service.addYouTubeItem(
                                                    token = token,
                                                    childId = childId,
                                                    request = AddYouTubeItemRequest(
                                                        type = customType,
                                                        externalId = customExternalId.trim(),
                                                        title = customTitle.trim()
                                                    )
                                                )
                                                customTitle = ""
                                                customExternalId = ""
                                                loadLibrary()
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                ) {
                                    Text("Approve")
                                }
                            }
                        }
                    }
                }

                // Approved Whitelist Library
                item {
                    Text("Approved Library (${ytData?.approvedItems?.size ?: 0})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                val approved = ytData?.approvedItems ?: emptyList()
                items(approved) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${item.type} • ${item.externalId}", fontSize = 11.sp, color = TextSecondary)
                            }
                            IconButton(onClick = {
                                scope.launch {
                                    val token = ApiClient.currentAuthToken ?: return@launch
                                    ApiClient.service.removeApprovedItem(token, childId, item.externalId)
                                    loadLibrary()
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}
