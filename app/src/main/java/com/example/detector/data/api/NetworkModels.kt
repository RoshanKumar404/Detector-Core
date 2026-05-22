package com.example.detector.data.api

import com.google.gson.annotations.SerializedName

// --- Auth Requests ---
data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val phone: String? = null,
    @SerializedName("municipality_id") val municipalityId: Int? = null,
    @SerializedName("ward_id") val wardId: Int? = null
)

// --- Auth Responses ---
data class LoginResponse(
    @SerializedName("access_token") val accessToken: String,
    val user: UserDto
)

data class UserDto(
    @SerializedName("id") val id: Int,
    val name: String,
    val email: String,
    val phone: String?,
    @SerializedName("municipality_id") val municipalityId: Int?,
    @SerializedName("municipality_name") val municipalityName: String?,
    @SerializedName("ward_id") val wardId: Int?
)

data class RegisterResponse(
    val message: String
)

// --- Municipality & Ward DTOs ---
data class MunicipalityDto(
    @SerializedName("municipality_id") val municipalityId: Int,
    @SerializedName("municipality_name") val municipalityName: String,
    @SerializedName("municipality_address") val municipalityAddress: String?,
    @SerializedName("municipality_contact") val municipalityContact: String?,
    @SerializedName("municipality_email") val municipalityEmail: String?
)

data class WardDto(
    @SerializedName("ward_id") val wardId: Int,
    @SerializedName("ward_number") val wardNumber: Int,
    @SerializedName("municipality_id") val municipalityId: Int
)

// --- Issue DTOs ---
data class IssueDto(
    val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("image_url") val imageUrl: String,
    val latitude: Double,
    val longitude: Double,
    val prediction: String,
    val confidence: Double,
    val status: String,
    @SerializedName("created_at") val createdAt: String
)

data class CreateIssueResponseDto(
    val message: String,
    @SerializedName("issue_id") val issueId: Int,
    @SerializedName("municipality_name") val municipalityName: String?,
    @SerializedName("image_url") val imageUrl: String
)

// --- Map GeoJSON DTOs ---
data class MapFeatureCollectionDto(
    val type: String,
    val features: List<MapFeatureDto>
)

data class MapFeatureDto(
    val type: String,
    val geometry: GeometryDto,
    val properties: MapPropertiesDto
)

data class GeometryDto(
    val type: String,
    val coordinates: List<Double> // [longitude, latitude]
)

data class MapPropertiesDto(
    val id: Int,
    @SerializedName("image_url") val imageUrl: String,
    val prediction: String,
    val status: String,
    @SerializedName("municipality_name") val municipalityName: String?,
    @SerializedName("created_at") val createdAt: String?
)

// --- Prediction response ---
data class PredictResponse(
    val prediction: String,
    val confidence: Double
)
