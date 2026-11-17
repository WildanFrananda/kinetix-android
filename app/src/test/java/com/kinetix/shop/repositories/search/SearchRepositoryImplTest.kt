package com.kinetix.shop.repositories.search

import com.kinetix.shop.core.network.ApiErrorParser
import com.kinetix.shop.core.network.HttpFailureMapper
import com.kinetix.shop.core.network.SafeApiCaller
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.dto.search.SearchFacetEntryDto
import com.kinetix.shop.dto.search.SearchFacetsDto
import com.kinetix.shop.dto.search.SearchHitDto
import com.kinetix.shop.dto.search.SearchIndexDto
import com.kinetix.shop.dto.search.SearchProductsResponseDto
import com.kinetix.shop.fakes.FakeSearchApiService
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class SearchRepositoryImplTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }
    private val safeApiCaller = SafeApiCaller(HttpFailureMapper(ApiErrorParser(json)))

    @Test
    fun searchMapsMinorPriceAndPreservesIndexStaleFlag() = runTest {
        val apiService = FakeSearchApiService().apply {
            searchHandler = { _, _, _, _, _ ->
                Response.success(
                    SearchProductsResponseDto(
                        total = 1,
                        hits = listOf(
                            SearchHitDto(
                                id = "SKU-900",
                                merchantId = "merch-9",
                                title = "Studio Monitor",
                                description = "Nearfield monitor",
                                categories = listOf("audio"),
                                priceMinor = 29995L,
                                currency = "USD",
                                updatedAtMs = 1_700_000_000_000L,
                            )
                        ),
                        facets = SearchFacetsDto(
                            categories = listOf(SearchFacetEntryDto("audio", 1)),
                            currency = listOf(SearchFacetEntryDto("USD", 1)),
                            merchantId = listOf(SearchFacetEntryDto("merch-9", 1)),
                        ),
                        index = SearchIndexDto(
                            indexedThroughMs = 1_700_000_000_000L,
                            ageSeconds = 120L,
                            stale = true,
                        ),
                    )
                )
            }
        }
        val repository = SearchRepositoryImpl(
            apiService = apiService,
            safeApiCaller = safeApiCaller,
        )

        val result = repository.search(query = "studio")
        assertTrue(result is Ok)
        val searchResult = (result as Ok).value
        assertEquals(29995L, searchResult.hits.first().priceMinor)
        assertTrue(searchResult.indexState.stale)
    }
}
