package com.kinetix.shop.routes

data object ProfileRoute : AppRoute {
    override val requiresAuthentication: Boolean = true
}
