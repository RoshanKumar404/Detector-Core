package com.example.detector.presentation.screens.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
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
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.circleBlur
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeOpacity
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.sources.GeoJsonSource

private const val OSM_RASTER_STYLE = """
{
  "version": 8,
  "sources": {
    "osm": {
      "type": "raster",
      "tiles": ["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],
      "tileSize": 256,
      "attribution": "© OpenStreetMap contributors"
    }
  },
  "layers": [
    {
      "id": "osm",
      "type": "raster",
      "source": "osm"
    }
  ]
}
"""

private val HEAT_SOURCES = listOf(
    "reports-pending-source",
    "reports-progress-source",
    "reports-resolved-source",
    "user-location-source"
)

private val HEAT_LAYERS = listOf(
    "reports-pending-heat",
    "reports-progress-heat",
    "reports-resolved-heat",
    "user-location-heat"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(navController: NavController, viewModel: MapViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleReady by remember { mutableStateOf(false) }

    LaunchedEffect(context) {
        MapLibre.getInstance(context.applicationContext)
    }

    LaunchedEffect(Unit) {
        viewModel.loadMapData(context)
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
                    MapLibre.getInstance(ctx.applicationContext)
                    MapView(ctx).apply {
                        onCreate(null)
                        getMapAsync { loadedMap ->
                            mapLibreMap = loadedMap
                            loadedMap.uiSettings.isAttributionEnabled = true
                            loadedMap.uiSettings.isLogoEnabled = false
                            loadedMap.cameraPosition = CameraPosition.Builder()
                                .target(LatLng(27.7172, 85.3240))
                                .zoom(12.5)
                                .build()
                            loadedMap.setStyle(
                                Style.Builder().fromJson(OSM_RASTER_STYLE)
                            ) {
                                styleReady = true
                            }
                        }
                        onStart()
                        onResume()
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
                    if (state.issues.isEmpty()) {
                        MapMessage(
                            message = "No live reports found yet.",
                            actionText = "Refresh",
                            onAction = { viewModel.loadMapData(context) },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else if (state.issues.none { it.hasUsableMapCoordinates() }) {
                        MapMessage(
                            message = "Reports loaded, but none have valid GPS coordinates yet.",
                            actionText = "Refresh",
                            onAction = { viewModel.loadMapData(context) },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }

    val currentState = uiState
    LaunchedEffect(mapLibreMap, styleReady, currentState) {
        val loadedMap = mapLibreMap ?: return@LaunchedEffect
        if (styleReady && currentState is MapUiState.Success) {
            renderMapOverlays(
                map = loadedMap,
                issues = currentState.issues,
                userLocation = currentState.userLocation?.let {
                    LatLng(it.latitude, it.longitude)
                }
            )
        }
    }

    DisposableEffect(mapView) {
        onDispose {
            mapView?.onPause()
            mapView?.onStop()
            mapView?.onDestroy()
        }
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

private fun renderMapOverlays(
    map: MapLibreMap,
    issues: List<Issue>,
    userLocation: LatLng?
) {
    map.clear()
    val style = map.style ?: return
    clearHeatLayers(style)

    val reportPoints = issues.mapNotNull { issue ->
        if (issue.hasUsableMapCoordinates()) {
            issue to LatLng(issue.latitude, issue.longitude)
        } else {
            null
        }
    }

    addHeatLayer(
        style = style,
        sourceId = "reports-pending-source",
        layerId = "reports-pending-heat",
        points = reportPoints.filter { it.first.status.lowercase() == "pending" }.map { it.second },
        color = StatusPending,
        radius = 28f
    )
    addHeatLayer(
        style = style,
        sourceId = "reports-progress-source",
        layerId = "reports-progress-heat",
        points = reportPoints.filter { it.first.status.lowercase() == "in_progress" }.map { it.second },
        color = StatusInProgress,
        radius = 28f
    )
    addHeatLayer(
        style = style,
        sourceId = "reports-resolved-source",
        layerId = "reports-resolved-heat",
        points = reportPoints.filter { it.first.status.lowercase() == "resolved" }.map { it.second },
        color = StatusResolved,
        radius = 28f
    )
    userLocation?.let {
        addHeatLayer(
            style = style,
            sourceId = "user-location-source",
            layerId = "user-location-heat",
            points = listOf(it),
            color = Color(0xFF00B0FF),
            radius = 18f
        )
    }

    reportPoints.forEach { (issue, point) ->
        map.addMarker(
            MarkerOptions()
                .position(point)
                .title("Report #${issue.id}")
                .snippet("${issue.description} - ${issue.status}")
        )
    }

    val reportLocations = reportPoints.map { it.second }
    when {
        reportLocations.size == 1 -> {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(reportLocations.first(), 15.0))
        }
        reportLocations.size > 1 -> {
            val bounds = LatLngBounds.Builder()
                .includes(reportLocations)
                .build()
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 96))
        }
        userLocation != null -> {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 14.0))
        }
    }
}

private fun clearHeatLayers(style: Style) {
    HEAT_LAYERS.forEach { layerId ->
        style.getLayer(layerId)?.let { style.removeLayer(it) }
    }
    HEAT_SOURCES.forEach { sourceId ->
        style.getSource(sourceId)?.let { style.removeSource(it) }
    }
}

private fun addHeatLayer(
    style: Style,
    sourceId: String,
    layerId: String,
    points: List<LatLng>,
    color: Color,
    radius: Float
) {
    if (points.isEmpty()) return

    style.addSource(GeoJsonSource(sourceId, points.toFeatureCollectionJson()))
    style.addLayer(
        CircleLayer(layerId, sourceId).withProperties(
            circleRadius(radius),
            circleColor(color.toArgb()),
            circleOpacity(0.32f),
            circleBlur(0.65f),
            circleStrokeColor(color.toArgb()),
            circleStrokeOpacity(0.55f),
            circleStrokeWidth(1.5f)
        )
    )
}

private fun List<LatLng>.toFeatureCollectionJson(): String {
    val features = joinToString(separator = ",") { point ->
        """{"type":"Feature","geometry":{"type":"Point","coordinates":[${point.longitude},${point.latitude}]},"properties":{}}"""
    }
    return """{"type":"FeatureCollection","features":[$features]}"""
}

private fun Issue.hasUsableMapCoordinates(): Boolean {
    return latitude in -90.0..90.0 &&
        longitude in -180.0..180.0 &&
        !(latitude == 0.0 && longitude == 0.0)
}
