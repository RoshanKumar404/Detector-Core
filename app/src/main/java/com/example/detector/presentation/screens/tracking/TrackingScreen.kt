package com.example.detector.presentation.screens.tracking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.detector.domain.model.Issue
import com.example.detector.presentation.components.RefreshableContent
import com.example.detector.presentation.navigation.Screen
import com.example.detector.presentation.screens.home.BottomNavigationBar
import com.example.detector.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(navController: NavController, viewModel: TrackingViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Reports", fontWeight = FontWeight.Bold, color = TextDark) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightBackground)
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController, activeRoute = Screen.Tracking.route)
        },
        containerColor = LightBackground
    ) { innerPadding ->
        RefreshableContent(
            isRefreshing = uiState is TrackingUiState.Loading,
            onRefresh = { viewModel.loadIssues() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Segmented Control Filter Tabs Row
                FilterTabsRow(
                    selectedFilter = selectedFilter,
                    onFilterSelected = { viewModel.setFilter(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                when (val state = uiState) {
                    is TrackingUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = DeepTeal)
                        }
                    }
                    is TrackingUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = state.message, color = Color.Red)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { viewModel.loadIssues() }) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                    is TrackingUiState.Success -> {
                        val filteredList = viewModel.getFilteredIssues(state.issues)
                        if (filteredList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "No reports found for '$selectedFilter'", color = TextMuted)
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredList) { report ->
                                    TrackingReportCard(report = report, onClick = {
                                        navController.navigate(Screen.IssueDetails.createRoute(report.id))
                                    })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterTabsRow(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    val filters = listOf("All", "Pending", "In Progress", "Resolved")
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        filters.forEach { filter ->
            val isSelected = filter == selectedFilter
            val tabBackground = if (isSelected) DeepTeal else Color.Transparent
            val tabTextColor = if (isSelected) Color.White else TextDark

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(tabBackground)
                    .clickable { onFilterSelected(filter) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = filter,
                    color = tabTextColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TrackingReportCard(report: Issue, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail Image loaded via Coil
            AsyncImage(
                model = report.imageUrl,
                contentDescription = "Incident thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Text Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Report #${report.id}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Loc: ${String.format("%.4f", report.latitude)}, ${String.format("%.4f", report.longitude)}",
                    fontSize = 12.sp,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                // Status tag
                val tagColor = when (report.status.lowercase()) {
                    "pending" -> StatusPending
                    "in_progress" -> StatusInProgress
                    else -> StatusResolved
                }
                Box(
                    modifier = Modifier
                        .background(tagColor.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = report.status.uppercase(),
                        color = tagColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Arrow indicator
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Details",
                tint = TextMuted
            )
        }
    }
}
