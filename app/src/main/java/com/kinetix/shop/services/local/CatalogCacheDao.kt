package com.kinetix.shop.services.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CatalogCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(
        categories: List<CachedCategoryEntity>,
    )

    @Query("SELECT * FROM cached_categories ORDER BY name ASC")
    suspend fun listCategories(): List<CachedCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProducts(
        products: List<CachedProductEntity>,
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProduct(
        product: CachedProductEntity,
    )

    @Query("SELECT * FROM cached_products WHERE sku = :sku LIMIT 1")
    suspend fun findProductBySku(
        sku: String,
    ): CachedProductEntity?

    @Query(
        "SELECT * FROM cached_products " +
            "WHERE (:categorySlug IS NULL OR categorySlug = :categorySlug) " +
            "AND (:query IS NULL OR title LIKE '%' || :query || '%') " +
            "ORDER BY id DESC LIMIT :limit"
    )
    suspend fun listProducts(
        categorySlug: String?,
        query: String?,
        limit: Int,
    ): List<CachedProductEntity>
}
