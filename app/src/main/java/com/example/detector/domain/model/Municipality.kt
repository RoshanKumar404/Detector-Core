package com.example.detector.domain.model

data class Municipality(
    val municipalityId: Int,
    val municipalityName: String,
    val municipalityAddress: String?,
    val municipalityContact: String?,
    val municipalityEmail: String?
)

data class Ward(
    val wardId: Int,
    val wardNumber: Int,
    val municipalityId: Int
)
