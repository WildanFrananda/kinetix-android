package com.kinetix.shop.routes

import com.kinetix.shop.models.ids.CategorySlug
import kotlinx.serialization.Serializable

@Serializable
data class SearchRoute(
    val initialQuery: String = "",
    val initialCategorySlug: String? = null,
) : AppRoute {
    override val requiresAuthentication: Boolean = false

    fun toCategorySlug(): CategorySlug? {
        return initialCategorySlug?.let { CategorySlug(it) }
    }
}
