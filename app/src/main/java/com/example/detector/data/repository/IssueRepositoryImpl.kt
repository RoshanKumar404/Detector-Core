package com.example.detector.data.repository

import com.example.detector.data.api.ApiService
import com.example.detector.domain.model.Issue
import com.example.detector.domain.repository.IssueRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class IssueRepositoryImpl(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) : IssueRepository {

    private fun getAuthToken(): String {
        return sessionManager.getFormattedToken()
            ?: throw IllegalStateException("User is not authenticated")
    }

    override suspend fun getIssues(): List<Issue> {
        return apiService.getIssues(getAuthToken()).map {
            Issue(
                id = it.id.toString(),
                userId = it.userId.toString(),
                imageUrl = it.imageUrl,
                latitude = it.latitude,
                longitude = it.longitude,
                severity = if (it.prediction == "waterlogged") "high" else "low",
                description = it.prediction,
                status = it.status,
                createdAt = it.createdAt,
                updatedAt = it.createdAt
            )
        }
    }

    override suspend fun getMapIssues(): List<Issue> {
        val geoJson = apiService.getMapIssues(getAuthToken())
        return geoJson.features.map {
            Issue(
                id = it.properties.id.toString(),
                userId = "",
                imageUrl = it.properties.imageUrl,
                latitude = it.geometry.coordinates[1], // GeoJSON is [lon, lat]
                longitude = it.geometry.coordinates[0],
                severity = if (it.properties.prediction == "waterlogged") "high" else "low",
                description = it.properties.prediction,
                status = it.properties.status,
                createdAt = it.properties.createdAt ?: "",
                updatedAt = it.properties.createdAt ?: "",
                municipalityName = it.properties.municipalityName
            )
        }
    }

    override suspend fun getGlobalIssues(): List<Issue> {
        return apiService.getGlobalIssues(getAuthToken()).map {
            Issue(
                id = it.id.toString(),
                userId = it.userId.toString(),
                imageUrl = it.imageUrl,
                latitude = it.latitude,
                longitude = it.longitude,
                severity = if (it.prediction == "waterlogged") "high" else "low",
                description = it.prediction,
                status = it.status,
                createdAt = it.createdAt,
                updatedAt = it.createdAt,
                municipalityName = it.municipalityName,
                userName = it.userName
            )
        }
    }


    override suspend fun createIssue(
        imageBytes: ByteArray,
        filename: String,
        latitude: Double,
        longitude: Double,
        prediction: String,
        confidence: Double
    ): Issue {
        val requestFile = imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull(), 0, imageBytes.size)
        val imagePart = MultipartBody.Part.createFormData("image", filename, requestFile)

        val latBody = latitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val lonBody = longitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val predBody = prediction.toRequestBody("text/plain".toMediaTypeOrNull())
        val confBody = confidence.toString().toRequestBody("text/plain".toMediaTypeOrNull())

        val result = apiService.createIssue(
            token = getAuthToken(),
            image = imagePart,
            latitude = latBody,
            longitude = lonBody,
            prediction = predBody,
            confidence = confBody
        )

        return Issue(
            id = result.issueId.toString(),
            userId = "",
            imageUrl = result.imageUrl,
            latitude = latitude,
            longitude = longitude,
            severity = if (prediction == "waterlogged") "high" else "low",
            description = prediction,
            status = "pending",
            createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
            updatedAt = "",
            municipalityName = result.municipalityName
        )
    }

    override suspend fun predictImage(
        imageBytes: ByteArray,
        filename: String
    ): Pair<String, Double> {
        val requestFile = imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull(), 0, imageBytes.size)
        val imagePart = MultipartBody.Part.createFormData("image", filename, requestFile)

        val result = apiService.predictImage(imagePart)
        return Pair(result.prediction, result.confidence)
    }
}
