package com.kinetix.shop.routes

import kotlinx.serialization.Serializable

@Serializable
data object TwoFactorSettingsRoute : AppRoute {
    override val requiresAuthentication: Boolean = true
}
