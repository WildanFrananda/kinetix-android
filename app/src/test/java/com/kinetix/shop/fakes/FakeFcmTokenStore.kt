package com.kinetix.shop.fakes

import com.kinetix.shop.services.notification.FcmTokenStore

class FakeFcmTokenStore : FcmTokenStore {
    private var storedToken: String? = null

    override fun getToken(): String? {
        return storedToken
    }

    override fun saveToken(
        token: String,
    ) {
        storedToken = token
    }

    override fun clearToken() {
        storedToken = null
    }
}
