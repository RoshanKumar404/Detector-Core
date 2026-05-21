package com.example.detector.domain.model

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String?,
    val municipalityId: Int?,
    val municipalityName: String?,
    val wardId: Int?
)
