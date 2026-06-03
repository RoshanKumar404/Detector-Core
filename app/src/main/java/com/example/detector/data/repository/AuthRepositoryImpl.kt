package com.example.detector.data.repository

import com.example.detector.data.api.ApiService
import com.example.detector.data.api.LoginRequest
import com.example.detector.data.api.RegisterRequest
import com.example.detector.domain.model.Municipality
import com.example.detector.domain.model.User
import com.example.detector.domain.model.Ward
import com.example.detector.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) : AuthRepository {

    override suspend fun login(email: String, password: String): User {
        val response = apiService.login(LoginRequest(email.trim(), password))
        
        val domainUser = User(
            id = response.user.id,
            name = response.user.name,
            email = response.user.email,
            phone = response.user.phone,
            municipalityId = response.user.municipalityId,
            municipalityName = response.user.municipalityName,
            wardId = response.user.wardId
        )

        // Save session tokens and user context locally
        sessionManager.saveAuthToken(response.accessToken)
        sessionManager.saveUser(domainUser)

        return domainUser
    }

    override suspend fun register(
        name: String,
        email: String,
        password: String,
        phone: String?,
        municipalityId: Int?,
        wardId: Int?
    ) {
        apiService.register(
            RegisterRequest(
                name = name.trim(),
                email = email.trim(),
                password = password,
                phone = phone?.trim(),
                municipalityId = municipalityId,
                wardId = wardId
            )
        )
    }

    override suspend fun getMunicipalities(): List<Municipality> {
        return apiService.getMunicipalities().map {
            Municipality(
                municipalityId = it.municipalityId,
                municipalityName = it.municipalityName,
                municipalityAddress = it.municipalityAddress,
                municipalityContact = it.municipalityContact,
                municipalityEmail = it.municipalityEmail
            )
        }
    }

    override suspend fun getWards(municipalityId: Int): List<Ward> {
        return apiService.getWards(municipalityId).map {
            Ward(
                wardId = it.wardId,
                wardNumber = it.wardNumber,
                municipalityId = it.municipalityId
            )
        }
    }
}
