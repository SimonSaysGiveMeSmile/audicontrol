package com.audicontrol.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "audicontrol_tokens",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var idkAccessToken: String?
        get() = prefs.getString(KEY_IDK_ACCESS, null)
        set(value) = prefs.edit().putString(KEY_IDK_ACCESS, value).apply()

    var idkRefreshToken: String?
        get() = prefs.getString(KEY_IDK_REFRESH, null)
        set(value) = prefs.edit().putString(KEY_IDK_REFRESH, value).apply()

    var idkIdToken: String?
        get() = prefs.getString(KEY_IDK_ID_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_IDK_ID_TOKEN, value).apply()

    var idkExpiresAt: Long
        get() = prefs.getLong(KEY_IDK_EXPIRES, 0L)
        set(value) = prefs.edit().putLong(KEY_IDK_EXPIRES, value).apply()

    var mbbAccessToken: String?
        get() = prefs.getString(KEY_MBB_ACCESS, null)
        set(value) = prefs.edit().putString(KEY_MBB_ACCESS, value).apply()

    var mbbRefreshToken: String?
        get() = prefs.getString(KEY_MBB_REFRESH, null)
        set(value) = prefs.edit().putString(KEY_MBB_REFRESH, value).apply()

    var mbbExpiresAt: Long
        get() = prefs.getLong(KEY_MBB_EXPIRES, 0L)
        set(value) = prefs.edit().putLong(KEY_MBB_EXPIRES, value).apply()

    val isLoggedIn: Boolean
        get() = mbbAccessToken != null

    val isIdkExpired: Boolean
        get() = System.currentTimeMillis() >= idkExpiresAt

    val isMbbExpired: Boolean
        get() = System.currentTimeMillis() >= mbbExpiresAt

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_IDK_ACCESS = "idk_access_token"
        private const val KEY_IDK_REFRESH = "idk_refresh_token"
        private const val KEY_IDK_ID_TOKEN = "idk_id_token"
        private const val KEY_IDK_EXPIRES = "idk_expires_at"
        private const val KEY_MBB_ACCESS = "mbb_access_token"
        private const val KEY_MBB_REFRESH = "mbb_refresh_token"
        private const val KEY_MBB_EXPIRES = "mbb_expires_at"
    }
}
