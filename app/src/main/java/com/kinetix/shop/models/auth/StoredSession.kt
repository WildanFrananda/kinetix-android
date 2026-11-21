package com.kinetix.shop.models.auth

import com.kinetix.shop.models.ids.AccountId
import com.kinetix.shop.models.ids.PrincipalId

data class StoredSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Long,
    val accountId: AccountId,
    val principalId: PrincipalId,
    val email: String,
    val role: String = "customer",
) {
    val isBuyerAccount: Boolean
        get() = role.equals("customer", ignoreCase = true)
}
