package com.kinetix.shop.models.catalog

import com.kinetix.shop.models.common.DataOrigin
import com.kinetix.shop.models.ids.CategorySlug

data class Category(
    val id: Long,
    val name: String,
    val slug: CategorySlug,
    val dataOrigin: DataOrigin = DataOrigin.LIVE,
    val cachedAtEpochSeconds: Long? = null,
)
