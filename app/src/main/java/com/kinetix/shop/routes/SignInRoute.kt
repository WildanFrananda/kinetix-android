package com.kinetix.shop.routes

data object SignInRoute : AppRoute {
    override val requiresAuthentication: Boolean = false
}
