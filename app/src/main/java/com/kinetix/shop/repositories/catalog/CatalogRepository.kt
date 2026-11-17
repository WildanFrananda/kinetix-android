package com.kinetix.shop.repositories.catalog

import com.kinetix.shop.core.result.Result
import com.kinetix.shop.models.catalog.Category
import com.kinetix.shop.models.catalog.ProductDetail
import com.kinetix.shop.models.catalog.ProductPage
import com.kinetix.shop.models.ids.CategorySlug
import com.kinetix.shop.models.ids.ProductSku

interface CatalogRepository {
    suspend fun listCategories(): Result<List<Category>>

    suspend fun listProducts(
        categorySlug: CategorySlug? = null,
        query: String? = null,
        page: Int = 1,
        pageSize: Int = 20,
    ): Result<ProductPage>

    suspend fun getProductDetail(
        sku: ProductSku,
    ): Result<ProductDetail>
}
