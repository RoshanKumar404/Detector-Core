package com.example.detector.presentation.screens.issues

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.detector.presentation.navigation.Screen
import com.example.detector.ui.theme.DeepTeal
import com.example.detector.ui.theme.LightBackground
import com.example.detector.ui.theme.StatusInProgress
import com.example.detector.ui.theme.StatusPending
import com.example.detector.ui.theme.StatusResolved
import com.example.detector.ui.theme.SurfaceLight
import com.example.detector.ui.theme.TextDark
import com.example.detector.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllIssuesScreen(
    navController: NavController,
    viewModel: AllIssuesViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All User Issues", fontWeight = FontWeight.Bold, color = TextDark) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextDark)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadIssues() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = DeepTeal)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightBackground)
            )
        },
        containerColor = LightBackground
    ) { innerPadding ->
        when (val state = uiState) {
            is AllIssuesUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = DeepTeal)
                }
            }
            is AllIssuesUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = Color(0xFFE53935), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.loadIssues() },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepTeal),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Retry", color = Color.White)
                        }
                    }
                }
            }
            is AllIssuesUiState.Success -> {
                var selectedMunicipality by remember { mutableStateOf("All") }

                val municipalities = remember(state.issues) {
                    val list = state.issues.mapNotNull { it.municipalityName }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .sorted()
                    listOf("All") + list
                }

                val filteredIssues = remember(state.issues, selectedMunicipality) {
                    if (selectedMunicipality == "All") {
                        state.issues
                    } else {
                        state.issues.filter { it.municipalityName == selectedMunicipality }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (municipalities.size > 1) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LightBackground)
                        ) {
                            items(municipalities) { municipality ->
                                val isSelected = municipality == selectedMunicipality
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedMunicipality = municipality },
                                    label = { Text(municipality) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = DeepTeal,
                                        selectedLabelColor = Color.White,
                                        containerColor = SurfaceLight,
                                        labelColor = TextMuted
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        selectedBorderColor = DeepTeal,
                                        borderColor = Color.LightGray.copy(alpha = 0.5f),
                                        borderWidth = 1.dp,
                                        selectedBorderWidth = 1.dp
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    if (filteredIssues.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (selectedMunicipality == "All") "No issues found." else "No issues found for $selectedMunicipality.",
                                color = TextMuted
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredIssues, key = { it.id }) { issue ->
                                IssueSummaryCard(
                                    issue = issue,
                                    onClick = {
                                        navController.navigate(Screen.IssueDetails.createRoute(issue.id))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IssueSummaryCard(
    issue: Issue,
    onClick: () -> Unit
) {
    val statusColor = when (issue.status.lowercase()) {
        "pending" -> StatusPending
        "in_progress" -> StatusInProgress
        "resolved" -> StatusResolved
        else -> TextMuted
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            AsyncImage(
                model = issue.imageUrl,
                contentDescription = "Issue image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(statusColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Report #${issue.id}",
                        color = TextDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Text(
                    text = issue.status.replace("_", " ").uppercase(),
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = issue.municipalityName ?: "Municipality not available",
                    color = TextDark,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                if (!issue.userName.isNullOrBlank()) {
                    Text(
                        text = "by ${issue.userName}",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }


            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Description: ${issue.description}",
                color = TextMuted,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Place, contentDescription = null, tint = DeepTeal, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Lat ${String.format("%.5f", issue.latitude)}, Lon ${String.format("%.5f", issue.longitude)}",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }
    }
}
