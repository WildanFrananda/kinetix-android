package com.kinetix.shop.repositories.search

import com.kinetix.shop.core.result.Result
import com.kinetix.shop.models.ids.CategorySlug
import com.kinetix.shop.models.search.ProductSearchResult

interface SearchRepository {
    suspend fun suggest(
        query: String,
        limit: Int = 8,
    ): Result<List<String>>

    suspend fun search(
        query: String? = null,
        categorySlug: CategorySlug? = null,
        sort: String? = null,
        limit: Int = 20,
        offset: Int = 0,
    ): Result<ProductSearchResult>
}
