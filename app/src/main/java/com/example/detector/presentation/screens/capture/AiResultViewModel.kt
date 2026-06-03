package com.example.detector.presentation.screens.capture

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.detector.domain.repository.IssueRepository
import com.google.android.gms.location.Priority
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant

sealed interface AiResultUiState {
    object Idle : AiResultUiState
    object Analyzing : AiResultUiState
    data class Analyzed(val prediction: String, val confidence: Double) : AiResultUiState
    object Submitting : AiResultUiState
    object Success : AiResultUiState
    data class Error(val message: String) : AiResultUiState
}

class AiResultViewModel(
    private val issueRepository: IssueRepository
) : ViewModel() {

    private companion object {
        const val MIN_WATERLOGGED_CONFIDENCE = 0.88
    }

    private val _uiState = MutableStateFlow<AiResultUiState>(AiResultUiState.Idle)
    val uiState: StateFlow<AiResultUiState> = _uiState.asStateFlow()

    fun runPrediction(imagePath: String) {
        viewModelScope.launch {
            _uiState.value = AiResultUiState.Analyzing
            try {
                val file = File(imagePath)
                if (!file.exists()) {
                    _uiState.value = AiResultUiState.Error("Image file does not exist locally")
                    return@launch
                }
                val bytes = file.readBytes()
                val (prediction, confidence) = issueRepository.predictImage(bytes, file.name)
                _uiState.value = AiResultUiState.Analyzed(prediction, confidence)
            } catch (e: Exception) {
                _uiState.value = AiResultUiState.Error("AI Analysis failed: ${e.message}")
            }
        }
    }

    fun submitReport(
        context: Context,
        imagePath: String,
        prediction: String,
        confidence: Double,
        manualLatitude: Double? = null,
        manualLongitude: Double? = null
    ) {
        viewModelScope.launch {
            _uiState.value = AiResultUiState.Submitting
            try {
                if (prediction.lowercase() != "waterlogged" || confidence < MIN_WATERLOGGED_CONFIDENCE) {
                    _uiState.value = AiResultUiState.Error(
                        "Only waterlogged results above 88% confidence can be submitted."
                    )
                    return@launch
                }

                val file = File(imagePath)
                if (!file.exists()) {
                    _uiState.value = AiResultUiState.Error("Image file missing")
                    return@launch
                }

                val manualLocation = if (manualLatitude != null && manualLongitude != null) {
                    manualLatitude to manualLongitude
                } else {
                    null
                }

                if (manualLocation != null && !hasValidReportCoordinates(manualLocation.first, manualLocation.second)) {
                    _uiState.value = AiResultUiState.Error("Please enter a valid latitude and longitude.")
                    return@launch
                }

                val loc = if (manualLocation == null) getReportLocation(context) else null
                if (manualLocation == null && (loc == null || !loc.hasValidReportCoordinates())) {
                    _uiState.value = AiResultUiState.Error(
                        "Could not get GPS. Enter the location manually and try again."
                    )
                    return@launch
                }

                val latitude = manualLocation?.first ?: loc!!.latitude
                val longitude = manualLocation?.second ?: loc!!.longitude

                val bytes = file.readBytes()
                issueRepository.createIssue(
                    imageBytes = bytes,
                    filename = file.name,
                    latitude = latitude,
                    longitude = longitude,
                    prediction = prediction,
                    confidence = confidence,
                    capturedAt = Instant.ofEpochMilli(file.lastModified()).toString(),
                    locationSource = if (manualLocation == null) "gps" else "manual"
                )

                _uiState.value = AiResultUiState.Success
            } catch (e: Exception) {
                _uiState.value = AiResultUiState.Error("Failed to submit report: ${e.message}")
            }
        }
    }

    fun resetState() {
        _uiState.value = AiResultUiState.Idle
    }

    private suspend fun getReportLocation(context: Context): Location? {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) return null

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        return try {
            val lastLocation = fusedLocationClient.lastLocation.await()
            if (lastLocation?.hasValidReportCoordinates() == true) {
                lastLocation
            } else {
                val tokenSource = CancellationTokenSource()
                withTimeoutOrNull(10_000) {
                    fusedLocationClient
                        .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.token)
                        .await()
                }.also {
                    if (it == null) tokenSource.cancel()
                }
            }
        } catch (e: SecurityException) {
            null
        }
    }

    private fun Location.hasValidReportCoordinates(): Boolean {
        return hasValidReportCoordinates(latitude, longitude)
    }

    private fun hasValidReportCoordinates(latitude: Double, longitude: Double): Boolean {
        return latitude in -90.0..90.0 &&
            longitude in -180.0..180.0 &&
            !(latitude == 0.0 && longitude == 0.0)
    }
}
