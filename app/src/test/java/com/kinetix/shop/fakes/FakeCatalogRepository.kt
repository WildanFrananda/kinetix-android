package com.kinetix.shop.fakes

import com.kinetix.shop.core.failure.UnavailableFailure
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.core.result.Result
import com.kinetix.shop.models.catalog.Category
import com.kinetix.shop.models.catalog.ProductDetail
import com.kinetix.shop.models.catalog.ProductPage
import com.kinetix.shop.models.ids.CategorySlug
import com.kinetix.shop.models.ids.ProductSku
import com.kinetix.shop.repositories.catalog.CatalogRepository

class FakeCatalogRepository : CatalogRepository {
    var categoriesResult: Result<List<Category>> = Ok(emptyList())
    var productsResult: Result<ProductPage> = Err(
        UnavailableFailure("NOT_CONFIGURED", null)
    )
    var productDetailResult: Result<ProductDetail> = Err(
        UnavailableFailure("NOT_CONFIGURED", null)
    )

    var lastRequestedCategory: CategorySlug? = null
    var lastRequestedPage: Int = 1

    override suspend fun listCategories(): Result<List<Category>> {
        return categoriesResult
    }

    override suspend fun listProducts(
        categorySlug: CategorySlug?,
        query: String?,
        page: Int,
        pageSize: Int,
    ): Result<ProductPage> {
        lastRequestedCategory = categorySlug
        lastRequestedPage = page
        return productsResult
    }

    override suspend fun getProductDetail(
        sku: ProductSku,
    ): Result<ProductDetail> {
        return productDetailResult
    }
}
