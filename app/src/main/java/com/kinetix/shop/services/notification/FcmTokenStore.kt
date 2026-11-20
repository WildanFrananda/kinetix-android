package com.kinetix.shop.services.notification

interface FcmTokenStore {
    fun getToken(): String?

    fun saveToken(
        token: String,
    )

    fun clearToken()
}
