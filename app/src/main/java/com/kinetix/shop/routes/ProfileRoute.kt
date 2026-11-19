package com.kinetix.shop.routes

import kotlinx.serialization.Serializable

@Serializable
data object ProfileRoute : AppRoute {
    override val requiresAuthentication: Boolean = true
}
