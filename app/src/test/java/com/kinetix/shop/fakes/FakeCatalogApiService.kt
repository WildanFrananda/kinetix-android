package com.kinetix.shop.fakes

import com.kinetix.shop.dto.catalog.CategoryDto
import com.kinetix.shop.dto.catalog.ProductDetailResponseDto
import com.kinetix.shop.dto.catalog.ProductListResponseDto
import com.kinetix.shop.services.remote.CatalogApiService
import retrofit2.Response

class FakeCatalogApiService : CatalogApiService {
    var listCategoriesHandler: () -> Response<List<CategoryDto>> = {
        Response.success(emptyList())
    }
    var listProductsHandler: (String?, String?, Int, Int) -> Response<ProductListResponseDto> = { _, _, _, _ ->
        throw UnsupportedOperationException("listProductsHandler not configured")
    }
    var getProductHandler: (String) -> Response<ProductDetailResponseDto> = {
        throw UnsupportedOperationException("getProductHandler not configured")
    }

    override suspend fun listCategories(): Response<List<CategoryDto>> {
        return listCategoriesHandler()
    }

    override suspend fun listProducts(
        categorySlug: String?,
        query: String?,
        page: Int,
        pageSize: Int,
    ): Response<ProductListResponseDto> {
        return listProductsHandler(categorySlug, query, page, pageSize)
    }

    override suspend fun getProduct(
        sku: String,
    ): Response<ProductDetailResponseDto> {
        return getProductHandler(sku)
    }
}
