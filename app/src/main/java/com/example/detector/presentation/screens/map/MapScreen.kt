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
import androidx.compose.ui.draw.clip
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
import com.example.detector.ui.theme.SurfaceLight
import com.example.detector.ui.theme.TextDark
import com.google.gson.Gson

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MapScreen(navController: NavController, viewModel: MapViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    val gson = remember { Gson() }

    LaunchedEffect(key1 = true) {
        viewModel.loadMapData(context)
    }

    // Function to safely inject markers and user location into JS
    val updateMapJs: (WebView, MapUiState.Success) -> Unit = { webView, successState ->
        val jsonPoints = successState.issues.map { issue ->
            mapOf(
                "id" to issue.id,
                "latitude" to issue.latitude,
                "longitude" to issue.longitude,
                "status" to issue.status,
                "prediction" to issue.description,
                "image_url" to issue.imageUrl
            )
        }
        val pointsString = gson.toJson(jsonPoints)
        
        // Execute Leaflet updating script
        webView.evaluateJavascript("updateMapPoints('$pointsString');", null)
        
        // If user coordinates available, center/place pin
        successState.userLocation?.let { loc ->
            webView.evaluateJavascript("setMyLocation(${loc.latitude}, ${loc.longitude});", null)
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
                modifier = Modifier.padding(bottom = 80.dp) // Offset above bottom navbar
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
            // Android WebView wrapper
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                // Run initial updates on finish loading
                                if (uiState is MapUiState.Success) {
                                    updateMapJs(this@apply, uiState as MapUiState.Success)
                                }
                            }
                        }
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        }
                        // Load html from assets folder
                        loadUrl("file:///android_asset/leaflet_map.html")
                        webViewInstance = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Trigger updates dynamically on state changes
            val currentState = uiState
            if (currentState is MapUiState.Success) {
                webViewInstance?.let { webView ->
                    updateMapJs(webView, currentState)
                }
            }

            // Floating Header overlay info
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .fillMaxWidth(0.9f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight.copy(alpha = 0.9f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFFE67E22), RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pending", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    
                    Spacer(modifier = Modifier.width(14.dp))
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFF2980B9), RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("In Progress", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)

                    Spacer(modifier = Modifier.width(14.dp))
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFF27AE60), RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Resolved", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
                }
            }

            // Loading overlay
            if (uiState is MapUiState.Loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = DeepTeal)
                }
            }
        }
    }
}
