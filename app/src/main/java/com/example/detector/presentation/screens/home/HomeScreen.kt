package com.example.detector.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.detector.domain.model.Issue
import com.example.detector.presentation.navigation.Screen
import com.example.detector.ui.theme.*

@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController, activeRoute = Screen.Home.route)
        },
        containerColor = LightBackground
    ) { innerPadding ->
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = DeepTeal)
                }
            }
            is HomeUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = state.message, color = Color.Red, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadDashboardData() }) {
                            Text("Retry")
                        }
                    }
                }
            }
            is HomeUiState.Success -> {
                HomeScreenContent(
                    state = state,
                    navController = navController,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }
}

@Composable
fun HomeScreenContent(
    state: HomeUiState.Success,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            // Top Profile Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hello, ${state.user?.name ?: "User"}",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        text = "Let's make our city better!",
                        fontSize = 14.sp,
                        color = TextMuted
                    )
                }
                
                // Profile Avatar + Alert Bell
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.navigate(Screen.Notifications.route) }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Alerts", tint = TextDark)
                    }
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(DeepTeal),
                        contentAlignment = Alignment.Center
                    ) {
                        val initials = state.user?.name?.take(2)?.uppercase() ?: "US"
                        Text(text = initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }

        // Dashboard Card Info
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(
                        Brush.horizontalGradient(listOf(DeepTeal, SurfaceDark)),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Detection Active",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Help us keep your city safe and clean.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.15f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = BrightTeal, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }

        // Quick Actions
        item {
            Column {
                Text(
                    text = "Quick Actions",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuickActionItem(
                        title = "Report",
                        subtitle = "Waterlogging",
                        iconColor = Color(0xFF00BFA5),
                        backgroundColor = Color(0xFFE0F2F1),
                        icon = Icons.Default.Add,
                        onClick = { navController.navigate(Screen.Capture.route) }
                    )
                    QuickActionItem(
                        title = "Live Map",
                        subtitle = "Heatmap",
                        iconColor = Color(0xFFFF6D00),
                        backgroundColor = Color(0xFFFFE0B2),
                        icon = Icons.Default.Place,
                        onClick = { navController.navigate(Screen.Map.route) }
                    )
                    QuickActionItem(
                        title = "Track",
                        subtitle = "My Reports",
                        iconColor = Color(0xFF00C853),
                        backgroundColor = Color(0xFFE8F5E9),
                        icon = Icons.AutoMirrored.Filled.List,
                        onClick = { navController.navigate(Screen.Tracking.route) }
                    )
                }
            }
        }

        item {
            Button(
                onClick = { navController.navigate(Screen.AllIssues.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepTeal)
            ) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("View All Issues", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // Nearby Overview stats
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nearby Overview",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        text = "Last 24 hours",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(count = state.newCount.toString(), label = "New Reports", modifier = Modifier.weight(1f))
                    StatCard(count = state.inProgressCount.toString(), label = "In Progress", modifier = Modifier.weight(1f))
                    StatCard(count = state.resolvedCount.toString(), label = "Resolved", modifier = Modifier.weight(1f))
                }
            }
        }

        // Recent Reports
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Reports",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    text = "View all",
                    fontSize = 13.sp,
                    color = DeepTeal,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { navController.navigate(Screen.Tracking.route) }
                )
            }
        }

        if (state.recentReports.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "No recent reports.", color = TextMuted)
                }
            }
        } else {
            items(state.recentReports) { report ->
                RecentReportRow(report = report, onClick = {
                    navController.navigate(Screen.IssueDetails.createRoute(report.id))
                })
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun QuickActionItem(
    title: String,
    subtitle: String,
    iconColor: Color,
    backgroundColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(100.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(backgroundColor, shape = RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Text(text = subtitle, fontSize = 11.sp, color = TextMuted)
    }
}

@Composable
fun StatCard(count: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = count, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, fontSize = 11.sp, color = TextMuted, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun RecentReportRow(report: Issue, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stat Indicator color circle
            val statusColor = when (report.status.lowercase()) {
                "pending" -> StatusPending
                "in_progress" -> StatusInProgress
                else -> StatusResolved
            }
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(statusColor, shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Report #${report.id}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    text = "Location: Lat ${String.format("%.4f", report.latitude)}, Lon ${String.format("%.4f", report.longitude)}",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
            Text(
                text = report.status.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor,
                modifier = Modifier
                    .background(statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController, activeRoute: String) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp),
        containerColor = SurfaceLight,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = activeRoute == Screen.Home.route,
            onClick = {
                if (activeRoute != Screen.Home.route) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                    }
                }
            },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DeepTeal,
                selectedTextColor = DeepTeal,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted,
                indicatorColor = LightTeal
            )
        )
        NavigationBarItem(
            selected = activeRoute == Screen.Map.route,
            onClick = {
                if (activeRoute != Screen.Map.route) {
                    navController.navigate(Screen.Map.route) {
                        launchSingleTop = true
                    }
                }
            },
            icon = { Icon(Icons.Default.Place, contentDescription = "Map") },
            label = { Text("Map") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DeepTeal,
                selectedTextColor = DeepTeal,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted,
                indicatorColor = LightTeal
            )
        )
        
        // Floating action button inside navbar placeholder
        Box(
            modifier = Modifier
                .weight(1f)
                .height(80.dp),
            contentAlignment = Alignment.Center
        ) {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.Capture.route) },
                containerColor = DeepTeal,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(54.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Report", modifier = Modifier.size(28.dp))
            }
        }

        NavigationBarItem(
            selected = activeRoute == Screen.Tracking.route,
            onClick = {
                if (activeRoute != Screen.Tracking.route) {
                    navController.navigate(Screen.Tracking.route) {
                        launchSingleTop = true
                    }
                }
            },
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Reports") },
            label = { Text("Reports") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DeepTeal,
                selectedTextColor = DeepTeal,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted,
                indicatorColor = LightTeal
            )
        )
        NavigationBarItem(
            selected = activeRoute == Screen.Profile.route,
            onClick = {
                if (activeRoute != Screen.Profile.route) {
                    navController.navigate(Screen.Profile.route) {
                        launchSingleTop = true
                    }
                }
            },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DeepTeal,
                selectedTextColor = DeepTeal,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted,
                indicatorColor = LightTeal
            )
        )
    }
}
