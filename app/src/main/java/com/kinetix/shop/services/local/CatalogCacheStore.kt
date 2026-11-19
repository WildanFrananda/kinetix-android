package com.kinetix.shop.services.local

import com.kinetix.shop.models.catalog.Category
import com.kinetix.shop.models.catalog.ProductDetail
import com.kinetix.shop.models.catalog.ProductSummary
import com.kinetix.shop.models.ids.CategorySlug
import com.kinetix.shop.models.ids.ProductSku

interface CatalogCacheStore {
    suspend fun saveCategories(
        categories: List<Category>,
    )

    suspend fun getCategories(): List<Category>

    suspend fun saveProductSummaries(
        products: List<ProductSummary>,
    )

    suspend fun getProductSummaries(
        categorySlug: CategorySlug?,
        query: String?,
        limit: Int,
    ): List<ProductSummary>

    suspend fun saveProductDetail(
        detail: ProductDetail,
    )

    suspend fun getProductDetail(
        sku: ProductSku,
    ): ProductDetail?
}
