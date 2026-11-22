package com.kinetix.shop.fakes

import com.kinetix.shop.models.catalog.Category
import com.kinetix.shop.models.catalog.ProductDetail
import com.kinetix.shop.models.catalog.ProductSummary
import com.kinetix.shop.models.catalog.StockStatus
import com.kinetix.shop.models.common.DataOrigin
import com.kinetix.shop.models.ids.CategorySlug
import com.kinetix.shop.models.ids.ProductSku
import com.kinetix.shop.services.local.CatalogCacheStore

class FakeCatalogCacheStore : CatalogCacheStore {
    private val categories = mutableListOf<Category>()
    private val summariesBySku = linkedMapOf<String, ProductSummary>()
    private val detailsBySku = linkedMapOf<String, ProductDetail>()

    override suspend fun saveCategories(
        categories: List<Category>,
    ) {
        this.categories.clear()
        this.categories.addAll(
            categories.map { category ->
                category.copy(
                    dataOrigin = DataOrigin.CACHE,
                    cachedAtEpochSeconds = category.cachedAtEpochSeconds ?: 1_700_000_000L,
                )
            }
        )
    }

    override suspend fun getCategories(): List<Category> {
        return categories.toList()
    }

    override suspend fun saveProductSummaries(
        products: List<ProductSummary>,
    ) {
        for (product in products) {
            summariesBySku[product.sku.value] = product.copy(
                availableStock = null,
                stockStatus = StockStatus.UNKNOWN,
                dataOrigin = DataOrigin.CACHE,
                cachedAtEpochSeconds = product.cachedAtEpochSeconds ?: 1_700_000_000L,
            )
        }
    }

    override suspend fun getProductSummaries(
        categorySlug: CategorySlug?,
        query: String?,
        limit: Int,
    ): List<ProductSummary> {
        return summariesBySku.values
            .filter { item ->
                (categorySlug == null || item.categorySlug == categorySlug) &&
                    (query.isNullOrBlank() || item.title.contains(query, ignoreCase = true))
            }
            .take(limit)
    }

    override suspend fun saveProductDetail(
        detail: ProductDetail,
    ) {
        detailsBySku[detail.sku.value] = detail.copy(
            warehouseStock = detail.warehouseStock.copy(
                binLocation = null,
                availableQuantity = null,
                reservedQuantity = null,
                status = StockStatus.UNKNOWN,
            ),
            dataOrigin = DataOrigin.CACHE,
            cachedAtEpochSeconds = detail.cachedAtEpochSeconds ?: 1_700_000_000L,
        )
    }

    override suspend fun getProductDetail(
        sku: ProductSku,
    ): ProductDetail? {
        return detailsBySku[sku.value]
    }
}
