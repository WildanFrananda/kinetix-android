package com.kinetix.shop.routes

data object HomeRoute : AppRoute {
    override val requiresAuthentication: Boolean = false
}
