package com.example.detector.presentation.screens.capture

import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.detector.domain.repository.IssueRepository
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import kotlinx.coroutines.tasks.await

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

    fun submitReport(context: Context, imagePath: String, prediction: String, confidence: Double) {
        viewModelScope.launch {
            _uiState.value = AiResultUiState.Submitting
            try {
                val file = File(imagePath)
                if (!file.exists()) {
                    _uiState.value = AiResultUiState.Error("Image file missing")
                    return@launch
                }

                // Fetch Location coordinates natively
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                var loc: Location? = null
                try {
                    loc = fusedLocationClient.lastLocation.await()
                } catch (e: SecurityException) {
                    // Ignore
                }

                val lat = loc?.latitude ?: 0.0
                val lon = loc?.longitude ?: 0.0

                val bytes = file.readBytes()
                issueRepository.createIssue(
                    imageBytes = bytes,
                    filename = file.name,
                    latitude = lat,
                    longitude = lon,
                    prediction = prediction,
                    confidence = confidence
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
}
