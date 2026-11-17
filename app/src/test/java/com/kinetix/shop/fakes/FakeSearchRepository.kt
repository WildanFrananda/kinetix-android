package com.kinetix.shop.fakes

import com.kinetix.shop.core.failure.UnavailableFailure
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.core.result.Result
import com.kinetix.shop.models.ids.CategorySlug
import com.kinetix.shop.models.search.ProductSearchResult
import com.kinetix.shop.repositories.search.SearchRepository

class FakeSearchRepository : SearchRepository {
    var suggestResult: Result<List<String>> = Ok(emptyList())
    var searchResult: Result<ProductSearchResult> = Err(
        UnavailableFailure("NOT_CONFIGURED", null)
    )

    var lastSearchQuery: String? = null
    var lastSearchCategory: CategorySlug? = null
    var lastSearchSort: String? = null
    var lastSearchOffset: Int = 0

    override suspend fun suggest(
        query: String,
        limit: Int,
    ): Result<List<String>> {
        return suggestResult
    }

    override suspend fun search(
        query: String?,
        categorySlug: CategorySlug?,
        sort: String?,
        limit: Int,
        offset: Int,
    ): Result<ProductSearchResult> {
        lastSearchQuery = query
        lastSearchCategory = categorySlug
        lastSearchSort = sort
        lastSearchOffset = offset
        return searchResult
    }
}
