package com.example.detector.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.detector.data.ServiceLocator
import com.example.detector.presentation.screens.auth.LoginViewModel
import com.example.detector.presentation.screens.auth.RegisterViewModel
import com.example.detector.presentation.screens.capture.AiResultViewModel
import com.example.detector.presentation.screens.home.HomeViewModel
import com.example.detector.presentation.screens.map.MapViewModel
import com.example.detector.presentation.screens.tracking.IssueDetailsViewModel
import com.example.detector.presentation.screens.tracking.TrackingViewModel

@Suppress("UNCHECKED_CAST")
class ViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                LoginViewModel(ServiceLocator.authRepository) as T
            }
            modelClass.isAssignableFrom(RegisterViewModel::class.java) -> {
                RegisterViewModel(ServiceLocator.authRepository) as T
            }
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(ServiceLocator.issueRepository, ServiceLocator.sessionManager) as T
            }
            modelClass.isAssignableFrom(MapViewModel::class.java) -> {
                MapViewModel(ServiceLocator.issueRepository) as T
            }
            modelClass.isAssignableFrom(TrackingViewModel::class.java) -> {
                TrackingViewModel(ServiceLocator.issueRepository) as T
            }
            modelClass.isAssignableFrom(IssueDetailsViewModel::class.java) -> {
                IssueDetailsViewModel(ServiceLocator.issueRepository) as T
            }
            modelClass.isAssignableFrom(AiResultViewModel::class.java) -> {
                AiResultViewModel(ServiceLocator.issueRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
