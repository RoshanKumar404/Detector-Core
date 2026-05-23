package com.example.detector.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import com.example.detector.domain.model.User
import com.google.gson.Gson
import java.security.MessageDigest

class SessionManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = context.getSharedPreferences("detector_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER = "user_info"
    }

    fun saveAuthToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getAuthToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun getFormattedToken(): String? {
        val token = getAuthToken()
        return if (token != null) "Bearer $token" else null
    }

    fun saveUser(user: User) {
        val json = gson.toJson(user)
        prefs.edit().putString(KEY_USER, json).apply()
    }

    fun getUser(): User? {
        val json = prefs.getString(KEY_USER, null) ?: return null
        return try {
            gson.fromJson(json, User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun getDeviceFingerprint(): String {
        val androidId = Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown-device"
        return MessageDigest.getInstance("SHA-256")
            .digest(androidId.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
