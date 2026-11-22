package com.kinetix.shop.services.local

import com.kinetix.shop.models.ids.AccountId
import com.kinetix.shop.models.profile.DeliveryProfile

interface ProfileCacheStore {
    suspend fun saveProfile(
        profile: DeliveryProfile,
    )

    suspend fun saveOptimisticProfile(
        profile: DeliveryProfile,
    )

    suspend fun getProfile(
        accountId: AccountId,
    ): DeliveryProfile?

    suspend fun getConfirmedProfile(
        accountId: AccountId,
    ): DeliveryProfile?

    suspend fun withdrawOptimisticProfile(
        accountId: AccountId,
    )

    suspend fun clearProfile(
        accountId: AccountId,
    )
}
