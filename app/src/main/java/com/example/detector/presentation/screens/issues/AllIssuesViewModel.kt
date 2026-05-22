package com.example.detector.presentation.screens.issues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.detector.domain.model.Issue
import com.example.detector.domain.repository.IssueRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AllIssuesUiState {
    object Loading : AllIssuesUiState
    data class Success(val issues: List<Issue>) : AllIssuesUiState
    data class Error(val message: String) : AllIssuesUiState
}

class AllIssuesViewModel(
    private val issueRepository: IssueRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AllIssuesUiState>(AllIssuesUiState.Loading)
    val uiState: StateFlow<AllIssuesUiState> = _uiState.asStateFlow()

    init {
        loadIssues()
    }

    fun loadIssues() {
        viewModelScope.launch {
            _uiState.value = AllIssuesUiState.Loading
            try {
                val issues = issueRepository.getGlobalIssues().sortedByDescending { it.createdAt }
                _uiState.value = AllIssuesUiState.Success(issues)
            } catch (e: Exception) {
                _uiState.value = AllIssuesUiState.Error(e.message ?: "Failed to load issues")
            }
        }
    }
}
