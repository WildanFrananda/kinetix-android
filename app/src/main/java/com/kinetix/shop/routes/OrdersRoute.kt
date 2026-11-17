package com.kinetix.shop.routes

data object OrdersRoute : AppRoute {
    override val requiresAuthentication: Boolean = true
}
