package com.kinetix.shop.services.local

import com.kinetix.shop.core.time.Clock
import com.kinetix.shop.models.catalog.Category
import com.kinetix.shop.models.catalog.ProductDetail
import com.kinetix.shop.models.catalog.ProductSummary
import com.kinetix.shop.models.catalog.StockStatus
import com.kinetix.shop.models.catalog.WarehouseStockInfo
import com.kinetix.shop.models.common.DataOrigin
import com.kinetix.shop.models.ids.CategorySlug
import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.models.ids.ProductSku
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class RoomCatalogCacheStore @Inject constructor(
    private val dao: CatalogCacheDao,
    private val clock: Clock,
) : CatalogCacheStore {
    private val writeMutex = Mutex()

    override suspend fun saveCategories(
        categories: List<Category>,
    ) {
        val now = clock.nowEpochSeconds()
        writeMutex.withLock {
            dao.upsertCategories(
                categories.map { category ->
                    CachedCategoryEntity(
                        slug = category.slug.value,
                        id = category.id,
                        name = category.name,
                        cachedAtEpochSeconds = now,
                    )
                }
            )
            dao.deleteCategoriesOlderThan(now - MAX_CACHE_AGE_SECONDS)
        }
    }

    override suspend fun getCategories(): List<Category> {
        val minEpoch = clock.nowEpochSeconds() - MAX_CACHE_AGE_SECONDS
        dao.deleteCategoriesOlderThan(minEpoch)
        return dao.listCategories().map { entity ->
            Category(
                id = entity.id,
                name = entity.name,
                slug = CategorySlug(entity.slug),
                dataOrigin = DataOrigin.CACHE,
                cachedAtEpochSeconds = entity.cachedAtEpochSeconds,
            )
        }
    }

    override suspend fun saveProductSummaries(
        products: List<ProductSummary>,
    ) {
        val now = clock.nowEpochSeconds()
        writeMutex.withLock {
            val entities = products.map { item ->
                val existing = dao.findProductBySku(item.sku.value)
                CachedProductEntity(
                    sku = item.sku.value,
                    id = item.id,
                    title = item.title,
                    description = existing?.description,
                    categoryName = item.categoryName,
                    categorySlug = item.categorySlug.value,
                    price = item.price,
                    currency = item.currency,
                    imageUrl = item.imageUrl,
                    merchantPrincipalId = item.merchantPrincipalId.value,
                    weightGrams = item.weightGrams ?: existing?.weightGrams,
                    availableQuantity = item.availableStock,
                    reservedQuantity = existing?.reservedQuantity,
                    binLocation = existing?.binLocation,
                    stockStatus = item.stockStatus.name,
                    cachedAtEpochSeconds = now,
                )
            }
            dao.upsertProducts(entities)
            dao.deleteProductsOlderThan(now - MAX_CACHE_AGE_SECONDS)
        }
    }

    override suspend fun getProductSummaries(
        categorySlug: CategorySlug?,
        query: String?,
        limit: Int,
    ): List<ProductSummary> {
        val minEpoch = clock.nowEpochSeconds() - MAX_CACHE_AGE_SECONDS
        dao.deleteProductsOlderThan(minEpoch)
        return dao.listProducts(
            categorySlug = categorySlug?.value,
            query = query?.takeIf { it.isNotBlank() },
            limit = limit,
        ).map { entity ->
            ProductSummary(
                id = entity.id,
                sku = ProductSku(entity.sku),
                title = entity.title,
                categoryName = entity.categoryName,
                categorySlug = CategorySlug(entity.categorySlug),
                price = entity.price,
                currency = entity.currency,
                imageUrl = entity.imageUrl,
                merchantPrincipalId = PrincipalId(entity.merchantPrincipalId),
                weightGrams = entity.weightGrams,
                availableStock = null,
                stockStatus = StockStatus.UNKNOWN,
                dataOrigin = DataOrigin.CACHE,
                cachedAtEpochSeconds = entity.cachedAtEpochSeconds,
            )
        }
    }

    override suspend fun saveProductDetail(
        detail: ProductDetail,
    ) {
        val now = clock.nowEpochSeconds()
        writeMutex.withLock {
            dao.upsertProduct(
                CachedProductEntity(
                    sku = detail.sku.value,
                    id = detail.id,
                    title = detail.title,
                    description = detail.description,
                    categoryName = detail.categoryName,
                    categorySlug = detail.categorySlug.value,
                    price = detail.price,
                    currency = detail.currency,
                    imageUrl = detail.imageUrl,
                    merchantPrincipalId = detail.merchantPrincipalId.value,
                    weightGrams = detail.weightGrams,
                    availableQuantity = detail.warehouseStock.availableQuantity,
                    reservedQuantity = detail.warehouseStock.reservedQuantity,
                    binLocation = detail.warehouseStock.binLocation,
                    stockStatus = detail.warehouseStock.status.name,
                    cachedAtEpochSeconds = now,
                )
            )
            dao.deleteProductsOlderThan(now - MAX_CACHE_AGE_SECONDS)
        }
    }

    override suspend fun getProductDetail(
        sku: ProductSku,
    ): ProductDetail? {
        val minEpoch = clock.nowEpochSeconds() - MAX_CACHE_AGE_SECONDS
        dao.deleteProductsOlderThan(minEpoch)
        val entity = dao.findProductBySku(sku.value) ?: return null
        val description = entity.description ?: return null
        return ProductDetail(
            id = entity.id,
            sku = ProductSku(entity.sku),
            title = entity.title,
            description = description,
            categoryName = entity.categoryName,
            categorySlug = CategorySlug(entity.categorySlug),
            price = entity.price,
            currency = entity.currency,
            imageUrl = entity.imageUrl,
            merchantPrincipalId = PrincipalId(entity.merchantPrincipalId),
            weightGrams = entity.weightGrams,
            warehouseStock = WarehouseStockInfo(
                sku = ProductSku(entity.sku),
                binLocation = null,
                availableQuantity = null,
                reservedQuantity = null,
                status = StockStatus.UNKNOWN,
            ),
            dataOrigin = DataOrigin.CACHE,
            cachedAtEpochSeconds = entity.cachedAtEpochSeconds,
        )
    }

    companion object {
        const val MAX_CACHE_AGE_SECONDS: Long = 7L * 24L * 3600L
    }
}
