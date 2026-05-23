package com.example.detector.domain.repository

import com.example.detector.domain.model.Issue

interface IssueRepository {
    suspend fun getIssues(): List<Issue>
    suspend fun getMapIssues(): List<Issue>
    suspend fun getGlobalIssues(): List<Issue>

    suspend fun createIssue(
        imageBytes: ByteArray,
        filename: String,
        latitude: Double,
        longitude: Double,
        prediction: String,
        confidence: Double,
        locationSource: String
    ): Issue
    suspend fun predictImage(
        imageBytes: ByteArray,
        filename: String
    ): Pair<String, Double>
}
