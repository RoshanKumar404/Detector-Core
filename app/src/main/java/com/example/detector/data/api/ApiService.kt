package com.example.detector.data.api

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import com.example.detector.BuildConfig

interface ApiService {

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse

    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): RegisterResponse

    @GET("municipalities/")
    suspend fun getMunicipalities(): List<MunicipalityDto>

    @GET("municipalities/{m_id}/wards")
    suspend fun getWards(
        @Path("m_id") municipalityId: Int
    ): List<WardDto>

    @GET("issues/")
    suspend fun getIssues(
        @Header("Authorization") token: String
    ): List<IssueDto>

    @GET("issues/global")
    suspend fun getGlobalIssues(
        @Header("Authorization") token: String
    ): List<IssueDto>


    @GET("issues/map")
    suspend fun getMapIssues(
        @Header("Authorization") token: String
    ): MapFeatureCollectionDto

    @Multipart
    @POST("issues/")
    suspend fun createIssue(
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("prediction") prediction: RequestBody,
        @Part("confidence") confidence: RequestBody,
        @Part("device_fingerprint") deviceFingerprint: RequestBody,
        @Part("location_source") locationSource: RequestBody
    ): CreateIssueResponseDto

    @Multipart
    @POST("predict/")
    suspend fun predictImage(
        @Part image: MultipartBody.Part
    ): PredictResponse

    companion object {
        // Change to your server's backend base URL
        private const val BASE_URL = "https://detectorbackend.onrender.com/api/"

        fun create(): ApiService {
            val logger = HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}
