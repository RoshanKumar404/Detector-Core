package com.example.detector.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.detector.data.repository.SessionManager
import com.example.detector.domain.model.Issue
import com.example.detector.domain.model.User
import com.example.detector.domain.repository.IssueRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(
        val user: User?,
        val newCount: Int,
        val inProgressCount: Int,
        val resolvedCount: Int,
        val recentReports: List<Issue>
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(
    private val issueRepository: IssueRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val user = sessionManager.getUser()
                val issues = issueRepository.getIssues()

                // Calculate local stats based on actual reports
                val newCount = issues.count { it.status.lowercase() == "pending" }
                val inProgressCount = issues.count { it.status.lowercase() == "in_progress" }
                val resolvedCount = issues.count { it.status.lowercase() == "resolved" }

                // Sort by date/id descending, take first 5
                val recent = issues.sortedByDescending { it.id }.take(5)

                _uiState.value = HomeUiState.Success(
                    user = user,
                    newCount = newCount,
                    inProgressCount = inProgressCount,
                    resolvedCount = resolvedCount,
                    recentReports = recent
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Failed to load dashboard data")
            }
        }
    }
}
