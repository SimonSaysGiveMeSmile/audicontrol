package com.audicontrol.auth

import android.content.Context
import android.content.Intent
import net.openid.appauth.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed class AuthState {
    data object LoggedOut : AuthState()
    data object Loading : AuthState()
    data object LoggedIn : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthManager(context: Context) {

    val tokenManager = TokenManager(context)
    private val mbbExchange = MbbTokenExchange(tokenManager)

    private val serviceConfig = AuthorizationServiceConfiguration(
        AudiAuthConfig.AUTHORIZATION_ENDPOINT,
        AudiAuthConfig.TOKEN_ENDPOINT
    )

    private val authService = AuthorizationService(context)

    private val _authState = MutableStateFlow<AuthState>(
        if (tokenManager.isLoggedIn) AuthState.LoggedIn else AuthState.LoggedOut
    )
    val authState: StateFlow<AuthState> = _authState

    fun buildAuthIntent(): Intent {
        val request = AuthorizationRequest.Builder(
            serviceConfig,
            AudiAuthConfig.CLIENT_ID,
            ResponseTypeValues.CODE,
            android.net.Uri.parse(AudiAuthConfig.REDIRECT_URI)
        )
            .setScope(AudiAuthConfig.SCOPE)
            .setCodeVerifier(CodeVerifierUtil.generateRandomCodeVerifier())
            .build()

        return authService.getAuthorizationRequestIntent(request)
    }

    suspend fun handleAuthResponse(intent: Intent): Boolean {
        _authState.value = AuthState.Loading

        val response = AuthorizationResponse.fromIntent(intent)
        val exception = AuthorizationException.fromIntent(intent)

        if (response == null) {
            _authState.value = AuthState.Error(exception?.errorDescription ?: "Authorization failed")
            return false
        }

        return try {
            val tokenResponse = performTokenExchange(response)
            if (tokenResponse != null) {
                tokenManager.idkAccessToken = tokenResponse.accessToken
                tokenManager.idkRefreshToken = tokenResponse.refreshToken
                tokenManager.idkIdToken = tokenResponse.idToken
                tokenManager.idkExpiresAt = tokenResponse.accessTokenExpirationTime
                    ?: (System.currentTimeMillis() + 3600_000)

                val idToken = tokenResponse.idToken
                if (idToken != null && mbbExchange.exchange(idToken)) {
                    _authState.value = AuthState.LoggedIn
                    true
                } else {
                    _authState.value = AuthState.Error("MBB token exchange failed")
                    false
                }
            } else {
                _authState.value = AuthState.Error("Token exchange failed")
                false
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Unknown error")
            false
        }
    }

    private suspend fun performTokenExchange(
        authResponse: AuthorizationResponse
    ): TokenResponse? {
        return kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            authService.performTokenRequest(authResponse.createTokenExchangeRequest()) { response, ex ->
                if (response != null) {
                    cont.resumeWith(Result.success(response))
                } else {
                    cont.resumeWith(Result.success(null))
                }
            }
        }
    }

    suspend fun refreshTokens(): Boolean {
        if (!tokenManager.isMbbExpired) return true

        if (tokenManager.isIdkExpired) {
            val refreshToken = tokenManager.idkRefreshToken ?: return false
            val refreshed = refreshIdkToken(refreshToken)
            if (!refreshed) return false
        }

        return mbbExchange.refresh()
    }

    private suspend fun refreshIdkToken(refreshToken: String): Boolean {
        return kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            val request = TokenRequest.Builder(serviceConfig, AudiAuthConfig.CLIENT_ID)
                .setGrantType(GrantTypeValues.REFRESH_TOKEN)
                .setRefreshToken(refreshToken)
                .build()

            authService.performTokenRequest(request) { response, _ ->
                if (response != null) {
                    tokenManager.idkAccessToken = response.accessToken
                    tokenManager.idkRefreshToken = response.refreshToken ?: refreshToken
                    tokenManager.idkExpiresAt = response.accessTokenExpirationTime
                        ?: (System.currentTimeMillis() + 3600_000)
                    cont.resumeWith(Result.success(true))
                } else {
                    cont.resumeWith(Result.success(false))
                }
            }
        }
    }

    fun logout() {
        tokenManager.clear()
        _authState.value = AuthState.LoggedOut
    }

    fun dispose() {
        authService.dispose()
    }
}
