package com.example.detector.presentation.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.detector.ui.theme.*

data class NotificationItem(
    val id: String,
    val title: String,
    val description: String,
    val time: String,
    val type: NotificationType
)

enum class NotificationType {
    ALERT,
    STATUS_UPDATE,
    GENERAL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(navController: NavController) {
    // Static notifications matching typical live workflow
    val notifications = listOf(
        NotificationItem(
            "1",
            "Report #39 Status Updated",
            "Your report on MG Road has been marked as IN PROGRESS by the municipal admin.",
            "2 hours ago",
            NotificationType.STATUS_UPDATE
        ),
        NotificationItem(
            "2",
            "Heavy Rainfall Warning",
            "High risk of waterlogging predicted in Wards 3, 4 and 8 for the next 6 hours.",
            "5 hours ago",
            NotificationType.ALERT
        ),
        NotificationItem(
            "3",
            "Report #34 Resolved",
            "Incident #34 at Sector 15 has been fully resolved and marked clean.",
            "Yesterday",
            NotificationType.STATUS_UPDATE
        ),
        NotificationItem(
            "4",
            "Floodwatch App Updated",
            "We have improved GPS accuracy and AI classification speed. Update now!",
            "3 days ago",
            NotificationType.GENERAL
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold, color = TextDark) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightBackground)
            )
        },
        containerColor = LightBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(notifications) { item ->
                NotificationRow(notification = item)
            }
        }
    }
}

@Composable
fun NotificationRow(notification: NotificationItem) {
    val iconColor = when (notification.type) {
        NotificationType.ALERT -> Color(0xFFFF3D00)
        NotificationType.STATUS_UPDATE -> DeepTeal
        NotificationType.GENERAL -> Color(0xFF00E676)
    }

    val iconVector = when (notification.type) {
        NotificationType.ALERT -> Icons.Default.Info
        NotificationType.STATUS_UPDATE -> Icons.Default.Notifications
        NotificationType.GENERAL -> Icons.Default.Star
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(iconVector, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.description,
                    fontSize = 12.sp,
                    color = TextMuted,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = notification.time,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMutedLight
                )
            }
        }
    }
}
