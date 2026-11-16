package org.familyguard.child.youtube

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import org.familyguard.child.data.api.ChildApiClient
import org.familyguard.child.ui.theme.*

data class SafeTubeItem(
    val id: String,
    val type: String, // CHANNEL, VIDEO
    val externalId: String,
    val title: String,
    val description: String?,
    val thumbnailUrl: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeTubeScreen(
    onNavigateBack: () -> Unit
) {
    var approvedItems by remember { mutableStateOf<List<SafeTubeItem>>(emptyList()) }
    var selectedVideoId by remember { mutableStateOf<String?>(null) }
    var dailyLimitMinutes by remember { mutableStateOf(60) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val childId = ChildApiClient.deviceId ?: ""
        // Fetch curated feed from backend
        scope.launch {
            try {
                // Mock / fallback sample safe channels for testing
                approvedItems = listOf(
                    SafeTubeItem(
                        id = "1",
                        type = "CHANNEL",
                        externalId = "UCXVCgDuD_XCkHYn-v95i75Q",
                        title = "Crash Course Kids",
                        description = "Science and engineering exploration for curious minds.",
                        thumbnailUrl = null
                    ),
                    SafeTubeItem(
                        id = "2",
                        type = "VIDEO",
                        externalId = "dQw4w9WgXcQ",
                        title = "Approved Exploration Video",
                        description = "Parent-selected educational feature.",
                        thumbnailUrl = null
                    )
                )
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FamilyGuard SafeTube", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
        ) {
            // Player Box (Active when video is tapped)
            if (selectedVideoId != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .background(androidx.compose.ui.graphics.Color.Black)
                ) {
                    SafeYouTubeWebView(videoId = selectedVideoId!!)
                }
            }

            // Info Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Tv, contentDescription = null, tint = ChildPrimaryGreen)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Parent-Approved Library Only", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("No algorithm recommendations, ads, or comment sections.", fontSize = 12.sp, color = TextSecondary)
                    }
                    AssistChip(
                        onClick = {},
                        label = { Text("${dailyLimitMinutes}m cap", fontSize = 11.sp) }
                    )
                }
            }

            Text(
                text = "Available Channels & Features",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(approvedItems) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedVideoId = if (item.type == "VIDEO") item.externalId else "dQw4w9WgXcQ"
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ChildPrimaryGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = ChildPrimaryGreen)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                Text(
                                    item.description ?: "Approved by parent",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    maxLines = 2
                                )
                            }

                            AssistChip(
                                onClick = {},
                                label = { Text(item.type, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SafeYouTubeWebView(videoId: String) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()

                val embedHtml = """
                    <!DOCTYPE html>
                    <html>
                    <body style="margin:0;padding:0;background-color:black;">
                        <iframe width="100%" height="100%" 
                                src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&controls=1&rel=0&modestbranding=1" 
                                frameborder="0" 
                                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" 
                                allowfullscreen>
                        </iframe>
                    </body>
                    </html>
                """.trimIndent()

                loadDataWithBaseURL("https://www.youtube-nocookie.com", embedHtml, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            val embedHtml = """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background-color:black;">
                    <iframe width="100%" height="100%" 
                            src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&controls=1&rel=0&modestbranding=1" 
                            frameborder="0" 
                            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" 
                            allowfullscreen>
                    </iframe>
                </body>
                </html>
            """.trimIndent()
            webView.loadDataWithBaseURL("https://www.youtube-nocookie.com", embedHtml, "text/html", "UTF-8", null)
        }
    )
}
