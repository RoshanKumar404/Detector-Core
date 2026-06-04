package com.example.detector.domain.model

data class Issue(
    val id: String,
    val userId: String,
    val imageUrl: String,
    val latitude: Double,
    val longitude: Double,
    val severity: String, // "high" (waterlogged) or "low" (dry/safe)
    val description: String,
    val status: String, // "pending", "confirmed", "resolved", "rejected"
    val createdAt: String,
    val updatedAt: String,
    val municipalityName: String? = null,
    val userName: String? = null,
    val verificationStatus: String? = null,   // "pending", "confirmed", "flagged"
    val verificationWeight: Double? = null,
    val verificationScore: Double? = null,
    val fraudFlags: List<String>? = null
)

