package com.example.detector.presentation.screens.map

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.Color as AndroidColor
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.detector.domain.model.Issue
import com.example.detector.presentation.navigation.Screen
import com.example.detector.presentation.screens.home.BottomNavigationBar
import com.example.detector.ui.theme.DeepTeal
import com.example.detector.ui.theme.LightBackground
import com.example.detector.ui.theme.StatusInProgress
import com.example.detector.ui.theme.StatusPending
import com.example.detector.ui.theme.StatusResolved
import com.example.detector.ui.theme.SurfaceLight
import com.example.detector.ui.theme.TextDark
import com.example.detector.ui.theme.TextMuted
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(navController: NavController, viewModel: MapViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var selectedIssue by remember { mutableStateOf<Issue?>(null) }

    // Runtime location permission request
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Permission result received — load map data regardless
        viewModel.loadMapData(context)
    }

    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        // Request location permissions before loading map data
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Manage MapView lifecycle to prevent memory leaks and unnecessary fetching
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView?.onPause()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Map Heatmap", fontWeight = FontWeight.Bold, color = TextDark) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightBackground)
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController, activeRoute = Screen.Map.route)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.loadMapData(context) },
                containerColor = DeepTeal,
                contentColor = Color.White,
                modifier = Modifier.padding(bottom = 80.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh Map")
            }
        },
        containerColor = LightBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        // Hide native zoom buttons to keep UI clean and prevent overlapping with Compose elements
                        zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                        controller.setZoom(13.0)
                        controller.setCenter(GeoPoint(23.4118443, 85.2304231))
                        mapView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            MapLegend(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .fillMaxWidth(0.9f)
            )

            // Update overlays when map is ready and data is successfully loaded
            val currentState = uiState
            LaunchedEffect(mapView, currentState) {
                val mv = mapView ?: return@LaunchedEffect
                if (currentState !is MapUiState.Success) return@LaunchedEffect

                mv.overlays.clear()

                // 1. Draw user location marker
                currentState.userLocation?.let { loc ->
                    if (!(loc.latitude == 0.0 && loc.longitude == 0.0)) {
                        val userPoint = GeoPoint(loc.latitude, loc.longitude)
                        val userMarker = Marker(mv).apply {
                            position = userPoint
                            icon = createCircleMarkerDrawable(context, AndroidColor.rgb(0, 176, 255), 12)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            title = "My Location"
                            infoWindow = null
                        }
                        mv.overlays.add(userMarker)
                    }
                }

                // 2. Draw issue locations as custom markers and heat circles
                val usableIssues = currentState.issues.filter {
                    it.latitude in -90.0..90.0 &&
                        it.longitude in -180.0..180.0 &&
                        !(it.latitude == 0.0 && it.longitude == 0.0)
                }

                val geoPoints = mutableListOf<GeoPoint>()

                usableIssues.forEach { issue ->
                    val issuePoint = GeoPoint(issue.latitude, issue.longitude)
                    geoPoints.add(issuePoint)

                    val status = (issue.status ?: "pending").lowercase()
                    val color = when (status) {
                        "in_progress" -> AndroidColor.rgb(41, 128, 185) // Blue
                        "resolved" -> AndroidColor.rgb(39, 174, 96)   // Green
                        else -> AndroidColor.rgb(230, 126, 34)       // Orange
                    }

                    // Render semi-transparent heat circle overlay (140 meters radius)
                    val circlePoints = Polygon.pointsAsCircle(issuePoint, 140.0)
                    val heatCircle = Polygon(mv).apply {
                        points = circlePoints
                        fillPaint.color = AndroidColor.argb((0.20 * 255).toInt(), AndroidColor.red(color), AndroidColor.green(color), AndroidColor.blue(color))
                        outlinePaint.color = AndroidColor.argb((0.35 * 255).toInt(), AndroidColor.red(color), AndroidColor.green(color), AndroidColor.blue(color))
                        outlinePaint.strokeWidth = 1.5f
                    }
                    mv.overlays.add(heatCircle)

                    // Render solid status marker overlay
                    val issueMarker = Marker(mv).apply {
                        position = issuePoint
                        icon = createCircleMarkerDrawable(context, color, 14)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "Report #${issue.id}"
                        infoWindow = null
                        setOnMarkerClickListener { _, _ ->
                            selectedIssue = issue
                            mv.controller.animateTo(issuePoint)
                            true
                        }
                    }
                    mv.overlays.add(issueMarker)
                }

                // 3. Zoom / adjust bounds to fit all data points dynamically
                if (geoPoints.isNotEmpty()) {
                    if (geoPoints.size == 1) {
                        mv.controller.setZoom(16.0)
                        mv.controller.setCenter(geoPoints[0])
                    } else {
                        var minLat = Double.MAX_VALUE
                        var maxLat = -Double.MAX_VALUE
                        var minLon = Double.MAX_VALUE
                        var maxLon = -Double.MAX_VALUE
                        for (gp in geoPoints) {
                            if (gp.latitude < minLat) minLat = gp.latitude
                            if (gp.latitude > maxLat) maxLat = gp.latitude
                            if (gp.longitude < minLon) minLon = gp.longitude
                            if (gp.longitude > maxLon) maxLon = gp.longitude
                        }
                        // Add coordinate padding to prevent markers from clipping the screen edges
                        val latPadding = (maxLat - minLat) * 0.15
                        val lonPadding = (maxLon - minLon) * 0.15
                        val box = BoundingBox(
                            maxLat + latPadding,
                            maxLon + lonPadding,
                            minLat - latPadding,
                            minLon - lonPadding
                        )
                        mv.post {
                            try {
                                mv.zoomToBoundingBox(box, true, 80)
                            } catch (e: Exception) {
                                mv.controller.setCenter(geoPoints[0])
                            }
                        }
                    }
                }
                mv.invalidate()
            }

            // State-based overlay screens (Loading, Error, Empty list states)
            when (val state = uiState) {
                is MapUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = DeepTeal)
                    }
                }
                is MapUiState.Error -> {
                    MapMessage(
                        message = state.message,
                        actionText = "Retry",
                        onAction = { viewModel.loadMapData(context) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is MapUiState.Success -> {
                    val usableIssues = state.issues.filter {
                        it.latitude in -90.0..90.0 &&
                            it.longitude in -180.0..180.0 &&
                            !(it.latitude == 0.0 && it.longitude == 0.0)
                    }
                    if (state.issues.isEmpty()) {
                        MapMessage(
                            message = "No live reports found yet.",
                            actionText = "Refresh",
                            onAction = { viewModel.loadMapData(context) },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else if (usableIssues.isEmpty()) {
                        MapMessage(
                            message = "Reports loaded, but none have valid GPS coordinates yet.",
                            actionText = "Refresh",
                            onAction = { viewModel.loadMapData(context) },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }

            // Beautiful floating bottom card detailing the selected issue
            selectedIssue?.let { issue ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .padding(bottom = 76.dp) // Lifted above the bottom navigation bar
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Report #${issue.id}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextDark
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val status = (issue.status ?: "pending").lowercase()
                                val (statusLabel, statusColor) = when (status) {
                                    "in_progress" -> "IN PROGRESS" to StatusInProgress
                                    "resolved" -> "RESOLVED" to StatusResolved
                                    else -> "PENDING" to StatusPending
                                }
                                Box(
                                    modifier = Modifier
                                        .background(statusColor, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = statusLabel,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            IconButton(onClick = { selectedIssue = null }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close details"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = issue.description ?: "No description provided.",
                            fontSize = 14.sp,
                            color = TextMuted,
                            modifier = Modifier.fillMaxWidth()
                        )

                        issue.imageUrl?.let { url ->
                            if (url.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Report Image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Programmatic helper to create nice status circles with white stroke
private fun createCircleMarkerDrawable(context: android.content.Context, color: Int, sizeDp: Int): Drawable {
    val density = context.resources.displayMetrics.density
    val sizeInPx = (sizeDp * density).toInt()
    return GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        setStroke((2 * density).toInt(), android.graphics.Color.WHITE)
        setSize(sizeInPx, sizeInPx)
    }
}

@Composable
private fun MapLegend(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight.copy(alpha = 0.94f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            LegendItem("Pending", StatusPending)
            Spacer(modifier = Modifier.width(12.dp))
            LegendItem("In Progress", StatusInProgress)
            Spacer(modifier = Modifier.width(12.dp))
            LegendItem("Resolved", StatusResolved)
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
    }
}

@Composable
private fun MapMessage(
    message: String,
    actionText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(24.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(message, color = TextMuted, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = DeepTeal),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(actionText, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
