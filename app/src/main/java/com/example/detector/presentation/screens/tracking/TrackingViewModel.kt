package com.example.detector.presentation.screens.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.detector.domain.model.Issue
import com.example.detector.domain.repository.IssueRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface TrackingUiState {
    object Loading : TrackingUiState
    data class Success(val issues: List<Issue>) : TrackingUiState
    data class Error(val message: String) : TrackingUiState
}

class TrackingViewModel(
    private val issueRepository: IssueRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TrackingUiState>(TrackingUiState.Loading)
    val uiState: StateFlow<TrackingUiState> = _uiState.asStateFlow()

    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    init {
        loadIssues()
    }

    fun loadIssues() {
        viewModelScope.launch {
            _uiState.value = TrackingUiState.Loading
            try {
                val issuesList = issueRepository.getIssues()
                _uiState.value = TrackingUiState.Success(issuesList)
            } catch (e: Exception) {
                _uiState.value = TrackingUiState.Error(e.message ?: "Failed to load reports")
            }
        }
    }

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun getFilteredIssues(issues: List<Issue>): List<Issue> {
        val filter = _selectedFilter.value.lowercase()
        if (filter == "all") return issues
        
        val targetStatus = when (filter) {
            "in progress" -> "in_progress"
            else -> filter
        }
        return issues.filter { it.status.lowercase() == targetStatus }
    }
}
