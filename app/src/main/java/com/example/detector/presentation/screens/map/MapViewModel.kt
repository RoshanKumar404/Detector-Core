package com.example.detector.presentation.screens.map

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.detector.domain.model.Issue
import com.example.detector.domain.repository.IssueRepository
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
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

    companion object {
        private const val TAG = "MapViewModel"
    }

    fun loadMapData(context: Context) {
        viewModelScope.launch {
            _uiState.value = MapUiState.Loading
            try {
                // Fetch map issues from backend
                Log.d(TAG, "Fetching map issues from backend...")
                val issues = issueRepository.getMapIssues()
                Log.d(TAG, "Received ${issues.size} issues from backend")

                // Fetch current user location — try fresh fix, then fall back to cached
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                var userLoc: Location? = null
                try {
                    // First try a fresh location fix
                    val locationRequest = CurrentLocationRequest.Builder()
                        .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                        .setMaxUpdateAgeMillis(60_000) // Accept location up to 60s old
                        .setDurationMillis(5_000) // Wait up to 5s for a fix
                        .build()
                    userLoc = fusedLocationClient.getCurrentLocation(locationRequest, null).await()

                    // Fall back to last known location if fresh fix failed
                    if (userLoc == null) {
                        Log.d(TAG, "getCurrentLocation returned null, trying lastLocation fallback...")
                        userLoc = fusedLocationClient.lastLocation.await()
                    }

                    if (userLoc != null) {
                        Log.d(TAG, "User location: lat=${userLoc.latitude}, lng=${userLoc.longitude}")
                    } else {
                        Log.w(TAG, "Location unavailable — is GPS/Location Services enabled on the device?")
                    }
                } catch (e: SecurityException) {
                    Log.w(TAG, "Location permission not granted: ${e.message}")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to get user location: ${e.message}")
                }

                _uiState.value = MapUiState.Success(issues, userLoc)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load map data", e)
                val errorMsg = when {
                    e.message?.contains("401") == true -> "Session expired. Please login again."
                    e.message?.contains("timeout", ignoreCase = true) == true ||
                        e.message?.contains("connect", ignoreCase = true) == true ->
                        "Server is starting up, please retry in a moment."
                    e.message?.contains("authenticated", ignoreCase = true) == true ->
                        "Please login to view the map."
                    else -> e.message ?: "Failed to load map data"
                }
                _uiState.value = MapUiState.Error(errorMsg)
            }
        }
    }
}
