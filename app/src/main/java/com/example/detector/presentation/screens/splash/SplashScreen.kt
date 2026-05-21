package com.example.detector.presentation.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.detector.data.ServiceLocator
import com.example.detector.presentation.navigation.Screen
import com.example.detector.ui.theme.DarkBackground
import com.example.detector.ui.theme.DeepTeal
import com.example.detector.ui.theme.BrightTeal
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    val sessionManager = remember { ServiceLocator.sessionManager }

    LaunchedEffect(key1 = true) {
        delay(2000)
        if (sessionManager.getAuthToken() != null) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        } else {
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DarkBackground,
                        DeepTeal,
                        DarkBackground
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Draw a styled Floodwatch Circular Logo
            LogoAnimation()

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "FLOODWATCH",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "AI-Powered Waterlogging\nDetection for Safer Cities",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        // Simple bottom loading bar indicator line
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
                .width(100.dp)
                .height(4.dp)
                .background(Color.White.copy(alpha = 0.2f), shape = androidx.compose.foundation.shape.CircleShape)
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "loading")
            val animationProgress = infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "progress"
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animationProgress.value)
                    .background(BrightTeal, shape = androidx.compose.foundation.shape.CircleShape)
            )
        }
    }
}

@Composable
fun LogoAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "logo_pulse")
    val pulse = infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = Modifier.size(120.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = (size.minDimension / 2) * pulse.value

        // Draw Outer Glow Circle
        drawCircle(
            color = BrightTeal.copy(alpha = 0.15f),
            radius = radius + 10.dp.toPx()
        )

        // Draw Outer Circle Ring
        drawCircle(
            color = BrightTeal,
            radius = radius,
            style = Stroke(width = 2.dp.toPx())
        )

        // Draw Inner City and Wave Shapes (Abstract representation matching design)
        val innerRadius = radius * 0.6f
        drawCircle(
            color = Color.White.copy(alpha = 0.1f),
            radius = innerRadius
        )

        // City Silhouette + Waterline representation
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(center.x - innerRadius * 0.7f, center.y + innerRadius * 0.3f)
            // City skyline line approximations
            lineTo(center.x - innerRadius * 0.5f, center.y + innerRadius * 0.3f)
            lineTo(center.x - innerRadius * 0.5f, center.y - innerRadius * 0.2f)
            lineTo(center.x - innerRadius * 0.3f, center.y - innerRadius * 0.2f)
            lineTo(center.x - innerRadius * 0.3f, center.y + innerRadius * 0.1f)
            lineTo(center.x - innerRadius * 0.1f, center.y + innerRadius * 0.1f)
            lineTo(center.x - innerRadius * 0.1f, center.y - innerRadius * 0.4f)
            lineTo(center.x + innerRadius * 0.1f, center.y - innerRadius * 0.4f)
            lineTo(center.x + innerRadius * 0.1f, center.y + innerRadius * 0.2f)
            lineTo(center.x + innerRadius * 0.3f, center.y + innerRadius * 0.2f)
            lineTo(center.x + innerRadius * 0.3f, center.y - innerRadius * 0.1f)
            lineTo(center.x + innerRadius * 0.5f, center.y - innerRadius * 0.1f)
            lineTo(center.x + innerRadius * 0.5f, center.y + innerRadius * 0.3f)
            lineTo(center.x + innerRadius * 0.7f, center.y + innerRadius * 0.3f)
            
            // Draw Wave at bottom
            quadraticTo(
                center.x, center.y + innerRadius * 0.6f,
                center.x - innerRadius * 0.7f, center.y + innerRadius * 0.3f
            )
            close()
        }

        drawPath(
            path = path,
            color = Color.White
        )
    }
}
