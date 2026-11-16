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
import org.familyguard.parent.data.model.AppRestrictionDto
import org.familyguard.parent.data.model.BulkUpdateAppRestrictionsRequest
import org.familyguard.parent.data.model.UpdateAppRestrictionDto
import org.familyguard.parent.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppManagementScreen(
    childId: String,
    onNavigateBack: () -> Unit
) {
    var apps by remember { mutableStateOf<List<AppRestrictionDto>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val loadApps = {
        scope.launch {
            isLoading = true
            try {
                val token = ApiClient.currentAuthToken ?: return@launch
                val res = ApiClient.service.getAppRestrictions(token, childId)
                if (res.isSuccessful && res.body() != null) {
                    apps = res.body()!!
                }
            } catch (e: Exception) {
                // Ignore transient error
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(childId) {
        loadApps()
    }

    val filteredApps = apps.filter { app ->
        (selectedCategory == "ALL" || app.category == selectedCategory) &&
                (searchQuery.isBlank() || app.appName.contains(searchQuery, ignoreCase = true) || app.packageName.contains(searchQuery, ignoreCase = true))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Management", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    scope.launch {
                        isSaving = true
                        try {
                            val token = ApiClient.currentAuthToken ?: return@launch
                            val request = BulkUpdateAppRestrictionsRequest(
                                restrictions = apps.map {
                                    UpdateAppRestrictionDto(
                                        packageName = it.packageName,
                                        appName = it.appName,
                                        category = it.category,
                                        mode = it.mode,
                                        dailyLimitMinutes = it.dailyLimitMinutes
                                    )
                                }
                            )
                            val res = ApiClient.service.updateAppRestrictions(token, childId, request)
                            if (res.isSuccessful) {
                                Toast.makeText(context, "App restrictions updated & synced!", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error saving: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isSaving = false
                        }
                    }
                },
                containerColor = PrimaryBlue,
                contentColor = SurfaceWhite
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = SurfaceWhite, modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save & Sync Policies")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search apps...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            )

            // Category Chips Row
            ScrollableTabRow(
                selectedTabIndex = listOf("ALL", "GAMES", "SOCIAL", "EDUCATION", "ENTERTAINMENT", "UTILITY").indexOf(selectedCategory),
                edgePadding = 0.dp,
                containerColor = BackgroundLight
            ) {
                listOf("ALL", "GAMES", "SOCIAL", "EDUCATION", "ENTERTAINMENT", "UTILITY").forEach { cat ->
                    Tab(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        text = { Text(cat, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else if (filteredApps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No applications found in this category.", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredApps) { app ->
                        AppRestrictionCard(
                            app = app,
                            onModeChanged = { newMode ->
                                apps = apps.map {
                                    if (it.packageName == app.packageName) it.copy(mode = newMode) else it
                                }
                            },
                            onLimitChanged = { newLimit ->
                                apps = apps.map {
                                    if (it.packageName == app.packageName) it.copy(dailyLimitMinutes = newLimit) else it
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRestrictionCard(
    app: AppRestrictionDto,
    onModeChanged: (String) -> Unit,
    onLimitChanged: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (app.category) {
                        "GAMES" -> Icons.Default.SportsEsports
                        "EDUCATION" -> Icons.Default.School
                        "SOCIAL" -> Icons.Default.Forum
                        "ENTERTAINMENT" -> Icons.Default.Movie
                        else -> Icons.Default.Apps
                    },
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(36.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(app.appName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    Text(app.packageName, fontSize = 11.sp, color = TextSecondary)
                }

                AssistChip(
                    onClick = {},
                    label = { Text(app.category, fontSize = 10.sp) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mode Selector: Allowed / Blocked / Time-Limited
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = app.mode == "ALLOWED",
                    onClick = { onModeChanged("ALLOWED") },
                    label = { Text("Allow") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SuccessGreen.copy(alpha = 0.2f))
                )
                FilterChip(
                    selected = app.mode == "BLOCKED",
                    onClick = { onModeChanged("BLOCKED") },
                    label = { Text("Block") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = DangerRed.copy(alpha = 0.2f))
                )
                FilterChip(
                    selected = app.mode == "TIME_LIMITED",
                    onClick = { onModeChanged("TIME_LIMITED") },
                    label = { Text("Limit") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = WarningAmber.copy(alpha = 0.2f))
                )
            }

            if (app.mode == "TIME_LIMITED") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Daily Cap: ${app.dailyLimitMinutes} min", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = app.dailyLimitMinutes.toFloat(),
                        onValueChange = { onLimitChanged(it.toInt()) },
                        valueRange = 5f..180f,
                        steps = 34,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    )
                }
            }
        }
    }
}
