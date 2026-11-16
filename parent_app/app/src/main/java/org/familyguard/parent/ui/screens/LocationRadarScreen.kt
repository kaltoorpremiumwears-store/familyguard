package org.familyguard.parent.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import org.familyguard.parent.data.api.ApiClient
import org.familyguard.parent.data.model.CreateGeofenceRequest
import org.familyguard.parent.data.model.LocationOverviewDto
import org.familyguard.parent.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationRadarScreen(
    childId: String,
    onNavigateBack: () -> Unit
) {
    var locationData by remember { mutableStateOf<LocationOverviewDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddGeofence by remember { mutableStateOf(false) }

    var geofenceName by remember { mutableStateOf("Home") }
    var geofenceRadius by remember { mutableStateOf(200f) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val loadLocation = {
        scope.launch {
            try {
                val token = ApiClient.currentAuthToken ?: return@launch
                val res = ApiClient.service.getLatestLocation(token, childId)
                if (res.isSuccessful && res.body() != null) {
                    locationData = res.body()!!
                }
            } catch (e: Exception) {
                // Ignore transient error
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(childId) {
        loadLocation()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transparent Location Radar", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { loadLocation() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Location")
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
        ) {
            val fix = locationData?.latestFix
            val lat = fix?.latitude ?: 37.7749
            val lon = fix?.longitude ?: -122.4194
            val accuracy = fix?.accuracyMeters ?: 20f
            val childName = locationData?.childName ?: "Child"
            val geofences = locationData?.geofences ?: emptyList()

            // OpenStreetMap (OSM) Interactive Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(SurfaceWhite)
            ) {
                OpenStreetMapWebView(
                    latitude = lat,
                    longitude = lon,
                    accuracyMeters = accuracy,
                    childName = childName,
                    geofences = geofences
                )

                // Floating Status Pill on Map
                Card(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite.copy(alpha = 0.92f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.BatteryChargingFull,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${fix?.batteryPercentage ?: 85}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.width(8.dp))
                        Text("•", color = TextSecondary)
                        Spacer(modifier = Modifier.width(8.dp))

                        Icon(
                            Icons.Default.GpsFixed,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "±${accuracy.toInt()}m",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Location Metrics & Geofence Section
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(if (fix?.isOnline == true) SuccessGreen else DangerRed)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (fix != null) "Live GPS Coordinates" else "Location Pending Connection",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = fix?.recordedAt?.take(16)?.replace("T", " ") ?: "Awaiting fix",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Lat: ${"%.5f".format(lat)}, Lon: ${"%.5f".format(lon)}",
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Rendered using OpenStreetMap (100% Free & Open-Source)",
                                fontSize = 11.sp,
                                color = PrimaryBlue
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Safe Zones & Geofences (${geofences.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = { showAddGeofence = !showAddGeofence }) {
                            Icon(Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Safe Zone")
                        }
                    }
                }

                if (showAddGeofence) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("New Geofence at Current Child Location", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = geofenceName,
                                    onValueChange = { geofenceName = it },
                                    label = { Text("Zone Name (e.g. Home, School)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Text("Safe Radius: ${geofenceRadius.toInt()} Meters", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Slider(
                                    value = geofenceRadius,
                                    onValueChange = { geofenceRadius = it },
                                    valueRange = 50f..1000f,
                                    steps = 18,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = {
                                        scope.launch {
                                            val token = ApiClient.currentAuthToken ?: return@launch
                                            val res = ApiClient.service.createGeofence(
                                                token = token,
                                                childId = childId,
                                                request = CreateGeofenceRequest(
                                                    name = geofenceName.trim(),
                                                    latitude = lat,
                                                    longitude = lon,
                                                    radiusMeters = geofenceRadius
                                                )
                                            )
                                            if (res.isSuccessful) {
                                                Toast.makeText(context, "Safe zone created!", Toast.LENGTH_SHORT).show()
                                                showAddGeofence = false
                                                loadLocation()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                ) {
                                    Text("Save Safe Zone")
                                }
                            }
                        }
                    }
                }

                items(geofences) { zone ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = PrimaryBlue)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(zone.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Radius: ${zone.radiusMeters.toInt()}m • Alerts on arrival & exit", fontSize = 12.sp, color = TextSecondary)
                            }
                            IconButton(onClick = {
                                scope.launch {
                                    val token = ApiClient.currentAuthToken ?: return@launch
                                    ApiClient.service.deleteGeofence(token, childId, zone.id)
                                    loadLocation()
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun OpenStreetMapWebView(
    latitude: Double,
    longitude: Double,
    accuracyMeters: Float,
    childName: String,
    geofences: List<org.familyguard.parent.data.model.GeofenceZoneDto>
) {
    val geofencesJson = geofences.joinToString(prefix = "[", postfix = "]") {
        """{"name": "${it.name}", "lat": ${it.latitude}, "lon": ${it.longitude}, "radius": ${it.radiusMeters}}"""
    }

    val html = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                body { margin: 0; padding: 0; }
                #map { width: 100vw; height: 100vh; }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map', { zoomControl: false }).setView([$latitude, $longitude], 15);
                
                // 100% Free OpenStreetMap Standard Tiles
                L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    maxZoom: 19,
                    attribution: '© OpenStreetMap'
                }).addTo(map);

                // Accuracy circle
                L.circle([$latitude, $longitude], {
                    radius: $accuracyMeters,
                    color: '#1E56A0',
                    fillColor: '#1E56A0',
                    fillOpacity: 0.15
                }).addTo(map);

                // Child Pin
                var marker = L.marker([$latitude, $longitude]).addTo(map);
                marker.bindPopup("<b>$childName</b><br>Live GPS Location").openPopup();

                // Geofences
                var geofences = $geofencesJson;
                geofences.forEach(function(g) {
                    L.circle([g.lat, g.lon], {
                        radius: g.radius,
                        color: '#10B981',
                        fillColor: '#10B981',
                        fillOpacity: 0.2
                    }).addTo(map).bindPopup("<b>Safe Zone:</b> " + g.name);
                });
            </script>
        </body>
        </html>
    """.trimIndent()

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()
                loadDataWithBaseURL("https://openstreetmap.org", html, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://openstreetmap.org", html, "text/html", "UTF-8", null)
        }
    )
}
