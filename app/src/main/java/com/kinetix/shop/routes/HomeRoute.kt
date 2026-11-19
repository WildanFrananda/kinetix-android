package com.kinetix.shop.routes

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute : AppRoute {
    override val requiresAuthentication: Boolean = false
}
