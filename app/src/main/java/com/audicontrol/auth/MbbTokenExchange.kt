package com.audicontrol.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class MbbTokenExchange(private val tokenManager: TokenManager) {

    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun exchange(idToken: String): Boolean = withContext(Dispatchers.IO) {
        val body = FormBody.Builder()
            .add("grant_type", "id_token")
            .add("token", idToken)
            .add("scope", "sc2:fal")
            .build()

        val request = Request.Builder()
            .url(AudiAuthConfig.MBB_TOKEN_URL)
            .header("X-Client-Id", AudiAuthConfig.MBB_CLIENT_ID)
            .header("X-App-Name", "myAudi")
            .header("X-App-Version", "4.13.0")
            .header("User-Agent", "okhttp/4.12.0")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return@withContext false

        val responseBody = response.body?.string() ?: return@withContext false
        val jsonObj = json.parseToJsonElement(responseBody).jsonObject

        tokenManager.mbbAccessToken = jsonObj["access_token"]?.jsonPrimitive?.content
        tokenManager.mbbRefreshToken = jsonObj["refresh_token"]?.jsonPrimitive?.content

        val expiresIn = jsonObj["expires_in"]?.jsonPrimitive?.content?.toLongOrNull() ?: 3600
        tokenManager.mbbExpiresAt = System.currentTimeMillis() + (expiresIn * 1000)

        tokenManager.mbbAccessToken != null
    }

    suspend fun refresh(): Boolean = withContext(Dispatchers.IO) {
        val refreshToken = tokenManager.mbbRefreshToken ?: return@withContext false

        val body = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
            .add("scope", "sc2:fal")
            .build()

        val request = Request.Builder()
            .url(AudiAuthConfig.MBB_TOKEN_URL)
            .header("X-Client-Id", AudiAuthConfig.MBB_CLIENT_ID)
            .header("X-App-Name", "myAudi")
            .header("X-App-Version", "4.13.0")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return@withContext false

        val responseBody = response.body?.string() ?: return@withContext false
        val jsonObj = json.parseToJsonElement(responseBody).jsonObject

        tokenManager.mbbAccessToken = jsonObj["access_token"]?.jsonPrimitive?.content
        tokenManager.mbbRefreshToken = jsonObj["refresh_token"]?.jsonPrimitive?.content
            ?: tokenManager.mbbRefreshToken

        val expiresIn = jsonObj["expires_in"]?.jsonPrimitive?.content?.toLongOrNull() ?: 3600
        tokenManager.mbbExpiresAt = System.currentTimeMillis() + (expiresIn * 1000)

        tokenManager.mbbAccessToken != null
    }
}
