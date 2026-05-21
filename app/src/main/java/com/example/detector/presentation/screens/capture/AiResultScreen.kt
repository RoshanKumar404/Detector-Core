package com.example.detector.presentation.screens.capture

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.detector.presentation.navigation.Screen
import com.example.detector.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiResultScreen(
    navController: NavController,
    viewModel: AiResultViewModel,
    imagePath: String
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Trigger analysis as soon as screen opens if idle
    LaunchedEffect(key1 = imagePath) {
        viewModel.runPrediction(Uri.decode(imagePath))
    }

    var prediction by remember { mutableStateOf("Analyzing...") }
    var confidence by remember { mutableStateOf(0.0) }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AiResultUiState.Analyzed -> {
                prediction = state.prediction
                confidence = state.confidence
            }
            is AiResultUiState.Success -> {
                Toast.makeText(context, "Report submitted successfully!", Toast.LENGTH_LONG).show()
                viewModel.resetState()
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Home.route) { inclusive = true }
                }
            }
            is AiResultUiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Analysis Result", fontWeight = FontWeight.Bold, color = TextDark) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightBackground)
            )
        },
        containerColor = LightBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Image Preview Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.DarkGray)
                ) {
                    AsyncImage(
                        model = File(Uri.decode(imagePath)),
                        contentDescription = "Report preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (uiState is AiResultUiState.Analyzing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = BrightTeal)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Analyzing Image...", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                if (uiState is AiResultUiState.Analyzed || uiState is AiResultUiState.Submitting || uiState is AiResultUiState.Success) {
                    // Tag label
                    val isWaterlogged = prediction.lowercase() == "waterlogged"
                    val tagBackground = if (isWaterlogged) StatusPending else StatusResolved
                    val tagLabel = if (isWaterlogged) "WATERLOGGED" else "DRY / SAFE"

                    Box(
                        modifier = Modifier
                            .background(tagBackground, shape = RoundedCornerShape(20.dp))
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = tagLabel,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Confidence Score
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Confidence Score", color = TextDark, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = "${(confidence * 100).toInt()}%",
                            color = TextDark,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress bar
                    LinearProgressIndicator(
                        progress = confidence.toFloat(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = tagBackground,
                        trackColor = Color.LightGray
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Notice Message Banner
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = DeepTeal)
                            Spacer(modifier = Modifier.width(12.dp))
                            val noticeText = if (isWaterlogged) {
                                "Our AI model has detected water accumulation in this area."
                            } else {
                                "The scanned area appears dry and safe."
                            }
                            Text(text = noticeText, color = TextDark, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.Gray)
                ) {
                    Text("Retake Photo", color = TextDark, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        viewModel.submitReport(context, Uri.decode(imagePath), prediction, confidence)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepTeal),
                    enabled = uiState is AiResultUiState.Analyzed && uiState !is AiResultUiState.Submitting
                ) {
                    if (uiState is AiResultUiState.Submitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Submit Report", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
