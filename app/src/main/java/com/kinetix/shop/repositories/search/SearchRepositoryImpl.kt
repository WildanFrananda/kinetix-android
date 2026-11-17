package com.kinetix.shop.repositories.search

import com.kinetix.shop.core.network.SafeApiCaller
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.core.result.Result
import com.kinetix.shop.models.ids.CategorySlug
import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.models.ids.ProductSku
import com.kinetix.shop.models.search.ProductSearchResult
import com.kinetix.shop.models.search.SearchFacetCount
import com.kinetix.shop.models.search.SearchHit
import com.kinetix.shop.models.search.SearchIndexState
import com.kinetix.shop.services.remote.SearchApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val apiService: SearchApiService,
    private val safeApiCaller: SafeApiCaller,
) : SearchRepository {
    override suspend fun suggest(
        query: String,
        limit: Int,
    ): Result<List<String>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return Ok(emptyList())
        }

        return safeApiCaller.execute(
            call = {
                apiService.suggestProducts(
                    query = trimmed,
                    limit = limit,
                )
            },
            transform = { dto ->
                Ok(dto.suggestions)
            },
        )
    }

    override suspend fun search(
        query: String?,
        categorySlug: CategorySlug?,
        sort: String?,
        limit: Int,
        offset: Int,
    ): Result<ProductSearchResult> {
        return safeApiCaller.execute(
            call = {
                apiService.searchProducts(
                    query = query?.takeIf { it.isNotBlank() },
                    categorySlug = categorySlug?.value,
                    sort = sort?.takeIf { it.isNotBlank() },
                    limit = limit,
                    offset = offset,
                )
            },
            transform = { dto ->
                val hits = dto.hits.map { hit ->
                    SearchHit(
                        sku = ProductSku(hit.id),
                        merchantPrincipalId = PrincipalId(hit.merchantId),
                        title = hit.title,
                        description = hit.description,
                        categories = hit.categories.map { CategorySlug(it) },
                        priceMinor = hit.priceMinor,
                        currency = hit.currency,
                    )
                }

                Ok(
                    ProductSearchResult(
                        total = dto.total,
                        hits = hits,
                        categoryFacets = dto.facets.categories.map { entry ->
                            SearchFacetCount(
                                value = entry.value,
                                count = entry.count,
                            )
                        },
                        indexState = SearchIndexState(
                            indexedThroughEpochMillis = dto.index.indexedThroughMs,
                            ageSeconds = dto.index.ageSeconds,
                            stale = dto.index.stale,
                        ),
                    )
                )
            },
        )
    }
}
