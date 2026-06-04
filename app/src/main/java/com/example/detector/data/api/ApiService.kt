package com.example.detector.data.api

import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import com.example.detector.BuildConfig
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

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
        @Part("captured_at") capturedAt: RequestBody,
        @Part("device_fingerprint") deviceFingerprint: RequestBody,
        @Part("location_source") locationSource: RequestBody
    ): CreateIssueResponseDto

    @Multipart
    @POST("predict/")
    suspend fun predictImage(
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part
    ): PredictResponse

    companion object {
        fun create(): ApiService {
            val logger = HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }

            // Retries once after 3 s on timeout/IO error to survive Render cold-starts (~30-60 s wake-up).
            val coldStartRetry = Interceptor { chain ->
                var response: Response? = null
                var lastError: IOException? = null
                for (attempt in 1..2) {
                    try {
                        response = chain.proceed(chain.request())
                        break
                    } catch (e: SocketTimeoutException) {
                        lastError = e
                        if (attempt < 2) Thread.sleep(3_000)
                    } catch (e: IOException) {
                        lastError = e
                        if (attempt < 2) Thread.sleep(3_000)
                    }
                }
                response ?: throw lastError!!
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .addInterceptor(coldStartRetry)
                .connectTimeout(90, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(90, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}
