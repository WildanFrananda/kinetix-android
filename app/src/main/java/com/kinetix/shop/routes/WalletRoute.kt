package com.kinetix.shop.routes

data object WalletRoute : AppRoute {
    override val requiresAuthentication: Boolean = true
}
