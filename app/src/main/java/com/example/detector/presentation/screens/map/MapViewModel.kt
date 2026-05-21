package com.example.detector.presentation.screens.map

import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.detector.domain.model.Issue
import com.example.detector.domain.repository.IssueRepository
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface MapUiState {
    object Loading : MapUiState
    data class Success(val issues: List<Issue>, val userLocation: Location?) : MapUiState
    data class Error(val message: String) : MapUiState
}

class MapViewModel(
    private val issueRepository: IssueRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Loading)
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    fun loadMapData(context: Context) {
        viewModelScope.launch {
            _uiState.value = MapUiState.Loading
            try {
                // Fetch map issues from backend
                val issues = issueRepository.getMapIssues()

                // Fetch current user location
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                var userLoc: Location? = null
                try {
                    userLoc = fusedLocationClient.lastLocation.await()
                } catch (e: SecurityException) {
                    // Ignore
                }

                _uiState.value = MapUiState.Success(issues, userLoc)
            } catch (e: Exception) {
                _uiState.value = MapUiState.Error(e.message ?: "Failed to load map data")
            }
        }
    }
}
