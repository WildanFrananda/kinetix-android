package com.kinetix.shop.routes

import com.kinetix.shop.models.ids.CategorySlug

data class SearchRoute(
    val initialQuery: String = "",
    val initialCategory: CategorySlug? = null,
) : AppRoute {
    override val requiresAuthentication: Boolean = false
}
