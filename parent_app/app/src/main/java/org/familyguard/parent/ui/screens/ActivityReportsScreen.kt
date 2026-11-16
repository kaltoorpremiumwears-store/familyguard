package org.familyguard.parent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
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
import org.familyguard.parent.data.model.DailyReportDto
import org.familyguard.parent.data.model.WeeklyTrendsDto
import org.familyguard.parent.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityReportsScreen(
    childId: String,
    onNavigateBack: () -> Unit
) {
    var dailyReport by remember { mutableStateOf<DailyReportDto?>(null) }
    var weeklyTrends by remember { mutableStateOf<WeeklyTrendsDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    val loadReports = {
        scope.launch {
            isLoading = true
            try {
                val token = ApiClient.currentAuthToken ?: return@launch
                val dailyRes = ApiClient.service.getDailyReport(token, childId)
                if (dailyRes.isSuccessful) {
                    dailyReport = dailyRes.body()
                }

                val weeklyRes = ApiClient.service.getWeeklyTrends(token, childId)
                if (weeklyRes.isSuccessful) {
                    weeklyTrends = weeklyRes.body()
                }
            } catch (e: Exception) {
                // Ignore transient error
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(childId) {
        loadReports()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity Reports & Insights", fontWeight = FontWeight.Bold) },
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
                    .verticalScroll(rememberScrollState())
            ) {
                // Today Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = PrimaryBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Today's Overview", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(dailyReport?.date ?: "", fontSize = 13.sp, color = TextSecondary)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val usedMinutes = dailyReport?.totalScreenTimeMinutes ?: 0
                        val capMinutes = dailyReport?.dailyAllowanceMinutes ?: 180
                        val percent = dailyReport?.percentUsed ?: 0

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${usedMinutes / 60}h ${usedMinutes % 60}m",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PrimaryBlue
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "of ${capMinutes / 60}h ${capMinutes % 60}m limit ($percent%)",
                                fontSize = 14.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { percent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = if (percent >= 100) DangerRed else PrimaryBlue,
                            trackColor = BackgroundLight
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Category Usage Breakdown
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("Category Breakdown", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        val cats = dailyReport?.categories
                        CategoryRow(label = "Games", minutes = cats?.gamesMinutes ?: 0, color = WarningAmber)
                        CategoryRow(label = "Education", minutes = cats?.educationMinutes ?: 0, color = SuccessGreen)
                        CategoryRow(label = "Social & Messaging", minutes = cats?.socialMinutes ?: 0, color = SecondaryTeal)
                        CategoryRow(label = "Entertainment & Video", minutes = cats?.entertainmentMinutes ?: 0, color = PrimaryBlue)
                        CategoryRow(label = "Utility & System", minutes = cats?.utilityMinutes ?: 0, color = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Top Apps Leaderboard
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("Top Used Applications", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        val topApps = dailyReport?.topApps ?: emptyList()
                        if (topApps.isEmpty()) {
                            Text("No application usage recorded for today yet.", color = TextSecondary, fontSize = 13.sp)
                        } else {
                            topApps.take(5).forEach { app ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(app.appName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text(app.category, fontSize = 11.sp, color = TextSecondary)
                                    }
                                    Text(
                                        "${app.durationMinutes} min",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = PrimaryBlue
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 7-Day Trend Chart
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("7-Day Screen Time Trend", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        val trends = weeklyTrends?.trends ?: emptyList()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val maxMinutes = (trends.maxOfOrNull { it.screenTimeMinutes } ?: 180).coerceAtLeast(60)

                            trends.forEach { item ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val heightFraction = (item.screenTimeMinutes.toFloat() / maxMinutes.toFloat()).coerceIn(0.05f, 1f)
                                    Box(
                                        modifier = Modifier
                                            .width(24.dp)
                                            .height((heightFraction * 80).dp)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(PrimaryBlue)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = item.date.takeLast(2), // day of month
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun CategoryRow(label: String, minutes: Int, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text("${minutes}m", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}
