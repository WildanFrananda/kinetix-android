package com.kinetix.shop.routes

data object CartRoute : AppRoute {
    override val requiresAuthentication: Boolean = true
}
