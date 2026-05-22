package com.example.detector.domain.model

data class Issue(
    val id: String,
    val userId: String,
    val imageUrl: String,
    val latitude: Double,
    val longitude: Double,
    val severity: String, // "high" (waterlogged) or "low" (dry/safe)
    val description: String,
    val status: String, // "pending", "in_progress", "resolved"
    val createdAt: String,
    val updatedAt: String,
    val municipalityName: String? = null,
    val userName: String? = null
)

