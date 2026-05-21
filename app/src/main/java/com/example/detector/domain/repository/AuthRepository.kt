package com.example.detector.domain.repository

import com.example.detector.domain.model.Municipality
import com.example.detector.domain.model.User
import com.example.detector.domain.model.Ward

interface AuthRepository {
    suspend fun login(email: String, password: String): User
    suspend fun register(
        name: String,
        email: String,
        password: String,
        phone: String?,
        municipalityId: Int?,
        wardId: Int?
    )
    suspend fun getMunicipalities(): List<Municipality>
    suspend fun getWards(municipalityId: Int): List<Ward>
}
