package com.example.detector.presentation.screens.map

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
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
import com.google.gson.Gson

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MapScreen(navController: NavController, viewModel: MapViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var webView by remember { mutableStateOf<WebView?>(null) }
    var pageReady by remember { mutableStateOf(false) }
    val gson = remember { Gson() }

    LaunchedEffect(Unit) {
        WebView.setWebContentsDebuggingEnabled(true)
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
                    WebView(ctx).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            allowFileAccess = true
                            allowContentAccess = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            cacheMode = WebSettings.LOAD_DEFAULT
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                pageReady = true
                            }
                        }
                        loadUrl("file:///android_asset/leaflet_map.html")
                        webView = this
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
        }
    }

    val currentState = uiState
    LaunchedEffect(webView, pageReady, currentState) {
        val mapWebView = webView ?: return@LaunchedEffect
        if (!pageReady || currentState !is MapUiState.Success) return@LaunchedEffect

        val points = currentState.issues
            .filter {
                it.latitude in -90.0..90.0 &&
                    it.longitude in -180.0..180.0 &&
                    !(it.latitude == 0.0 && it.longitude == 0.0)
            }
            .map {
                mapOf(
                    "id" to it.id,
                    "latitude" to it.latitude,
                    "longitude" to it.longitude,
                    "status" to it.status,
                    "prediction" to it.description,
                    "image_url" to it.imageUrl
                )
            }

        mapWebView.evaluateJavascript("window.updateMapPoints(${gson.toJson(points)});", null)

        currentState.userLocation?.let { loc ->
            if (!(loc.latitude == 0.0 && loc.longitude == 0.0)) {
                mapWebView.evaluateJavascript(
                    "window.setMyLocation(${loc.latitude}, ${loc.longitude});",
                    null
                )
            }
        }
    }

    DisposableEffect(webView) {
        onDispose {
            webView?.stopLoading()
            webView?.destroy()
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
