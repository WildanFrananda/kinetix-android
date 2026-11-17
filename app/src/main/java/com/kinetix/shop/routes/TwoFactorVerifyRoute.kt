package com.kinetix.shop.routes

data class TwoFactorVerifyRoute(
    val challengeToken: String,
) : AppRoute {
    override val requiresAuthentication: Boolean = false
}
