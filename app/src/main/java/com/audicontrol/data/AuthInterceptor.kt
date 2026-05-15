package com.audicontrol.data

import com.audicontrol.auth.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenManager: TokenManager,
    private val onTokenRefresh: suspend () -> Boolean
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        if (tokenManager.isMbbExpired) {
            runBlocking { onTokenRefresh() }
        }

        val token = tokenManager.mbbAccessToken ?: return chain.proceed(chain.request())

        val request = chain.request().newBuilder()
            .header("Authorization", "Bearer $token")
            .header("X-App-Name", "myAudi")
            .header("X-App-Version", "4.13.0")
            .header("Accept", "application/json")
            .build()

        val response = chain.proceed(request)

        if (response.code == 401) {
            response.close()
            val refreshed = runBlocking { onTokenRefresh() }
            if (!refreshed) return response

            val newToken = tokenManager.mbbAccessToken ?: return response
            val retry = chain.request().newBuilder()
                .header("Authorization", "Bearer $newToken")
                .header("X-App-Name", "myAudi")
                .header("X-App-Version", "4.13.0")
                .header("Accept", "application/json")
                .build()
            return chain.proceed(retry)
        }

        return response
    }
}
