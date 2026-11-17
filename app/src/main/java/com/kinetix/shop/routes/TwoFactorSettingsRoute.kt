package com.kinetix.shop.routes

data object TwoFactorSettingsRoute : AppRoute {
    override val requiresAuthentication: Boolean = true
}
