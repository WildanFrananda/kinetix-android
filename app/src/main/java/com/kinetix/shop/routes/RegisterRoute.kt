package com.kinetix.shop.routes

data object RegisterRoute : AppRoute {
    override val requiresAuthentication: Boolean = false
}
