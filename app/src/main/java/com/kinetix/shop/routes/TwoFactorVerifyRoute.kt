package com.kinetix.shop.routes

import kotlinx.serialization.Serializable

@Serializable
data class TwoFactorVerifyRoute(
    val challengeToken: String,
) : AppRoute {
    override val requiresAuthentication: Boolean = false
}
