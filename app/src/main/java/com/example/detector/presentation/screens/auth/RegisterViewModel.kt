package com.example.detector.presentation.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.detector.domain.model.Municipality
import com.example.detector.domain.model.Ward
import com.example.detector.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RegisterUiState {
    object Idle : RegisterUiState
    object Loading : RegisterUiState
    object Success : RegisterUiState
    data class Error(val message: String) : RegisterUiState
}

class RegisterViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _municipalities = MutableStateFlow<List<Municipality>>(emptyList())
    val municipalities: StateFlow<List<Municipality>> = _municipalities.asStateFlow()

    private val _wards = MutableStateFlow<List<Ward>>(emptyList())
    val wards: StateFlow<List<Ward>> = _wards.asStateFlow()

    private val _isLoadingGeo = MutableStateFlow(false)
    val isLoadingGeo: StateFlow<Boolean> = _isLoadingGeo.asStateFlow()

    init {
        loadMunicipalities()
    }

    private fun loadMunicipalities() {
        viewModelScope.launch {
            _isLoadingGeo.value = true
            try {
                _municipalities.value = authRepository.getMunicipalities()
            } catch (e: Exception) {
                _uiState.value = RegisterUiState.Error(
                    e.message ?: "Failed to load municipalities from backend."
                )
            } finally {
                _isLoadingGeo.value = false
            }
        }
    }

    fun onMunicipalitySelected(municipalityId: Int) {
        viewModelScope.launch {
            _isLoadingGeo.value = true
            _wards.value = emptyList() // clear current wards first
            try {
                _wards.value = authRepository.getWards(municipalityId)
            } catch (e: Exception) {
                _uiState.value = RegisterUiState.Error(
                    e.message ?: "Failed to load wards from backend."
                )
            } finally {
                _isLoadingGeo.value = false
            }
        }
    }

    fun register(
        name: String,
        email: String,
        password: String,
        phone: String?,
        municipalityId: Int?,
        wardId: Int?
    ) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.value = RegisterUiState.Error("Name, Email and Password are required.")
            return
        }

        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading
            try {
                authRepository.register(
                    name = name,
                    email = email,
                    password = password,
                    phone = if (phone?.isBlank() == true) null else phone,
                    municipalityId = municipalityId,
                    wardId = wardId
                )
                _uiState.value = RegisterUiState.Success
            } catch (e: Exception) {
                _uiState.value = RegisterUiState.Error(e.message ?: "Registration failed.")
            }
        }
    }

    fun resetState() {
        _uiState.value = RegisterUiState.Idle
    }
}
