package com.kinetix.shop.services.notification

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

@Singleton
class SharedPreferencesFcmTokenStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : FcmTokenStore {
    override fun getToken(): String? {
        return prefs().getString(KEY_FCM_TOKEN, null)?.takeIf { it.isNotBlank() }
    }

    override fun saveToken(
        token: String,
    ) {
        if (token.isNotBlank()) {
            prefs().edit { putString(KEY_FCM_TOKEN, token) }
        }
    }

    override fun clearToken() {
        prefs().edit { remove(KEY_FCM_TOKEN) }
    }

    private fun prefs() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        const val PREFS_NAME = "kinetix_fcm"
        const val KEY_FCM_TOKEN = "fcm_registration_token"
    }
}
