package com.kinetix.shop.routes

import kotlinx.serialization.Serializable

@Serializable
data object SignInRoute : AppRoute {
    override val requiresAuthentication: Boolean = false
}
