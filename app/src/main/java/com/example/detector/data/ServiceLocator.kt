package com.example.detector.data

import android.content.Context
import com.example.detector.data.api.ApiService
import com.example.detector.data.repository.AuthRepositoryImpl
import com.example.detector.data.repository.IssueRepositoryImpl
import com.example.detector.data.repository.SessionManager
import com.example.detector.domain.repository.AuthRepository
import com.example.detector.domain.repository.IssueRepository

object ServiceLocator {
    lateinit var sessionManager: SessionManager
        private set

    lateinit var apiService: ApiService
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var issueRepository: IssueRepository
        private set

    fun init(context: Context) {
        val appContext = context.applicationContext
        sessionManager = SessionManager(appContext)
        apiService = ApiService.create()
        authRepository = AuthRepositoryImpl(apiService, sessionManager)
        issueRepository = IssueRepositoryImpl(apiService, sessionManager)
    }
}
