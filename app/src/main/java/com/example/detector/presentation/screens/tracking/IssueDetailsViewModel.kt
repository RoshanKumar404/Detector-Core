package com.example.detector.presentation.screens.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.detector.domain.model.Issue
import com.example.detector.domain.repository.IssueRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface IssueDetailsUiState {
    object Loading : IssueDetailsUiState
    data class Success(val issue: Issue) : IssueDetailsUiState
    data class Error(val message: String) : IssueDetailsUiState
}

class IssueDetailsViewModel(
    private val issueRepository: IssueRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<IssueDetailsUiState>(IssueDetailsUiState.Loading)
    val uiState: StateFlow<IssueDetailsUiState> = _uiState.asStateFlow()

    fun loadIssueDetails(issueId: String) {
        viewModelScope.launch {
            _uiState.value = IssueDetailsUiState.Loading
            try {
                val issues = issueRepository.getIssues()
                val found = issues.find { it.id == issueId }
                if (found != null) {
                    _uiState.value = IssueDetailsUiState.Success(found)
                } else {
                    _uiState.value = IssueDetailsUiState.Error("Issue with ID #$issueId not found")
                }
            } catch (e: Exception) {
                _uiState.value = IssueDetailsUiState.Error(e.message ?: "Failed to load issue details")
            }
        }
    }
}
