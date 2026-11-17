package com.kinetix.shop.routes

sealed interface AppRoute {
    val requiresAuthentication: Boolean
}
