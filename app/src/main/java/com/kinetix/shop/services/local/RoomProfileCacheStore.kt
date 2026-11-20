package com.kinetix.shop.services.local

import com.kinetix.shop.core.time.Clock
import com.kinetix.shop.models.ids.AccountId
import com.kinetix.shop.models.profile.DeliveryProfile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomProfileCacheStore @Inject constructor(
    private val dao: ProfileCacheDao,
    private val clock: Clock,
) : ProfileCacheStore {
    override suspend fun saveProfile(
        profile: DeliveryProfile,
    ) {
        dao.upsertProfile(
            CachedProfileEntity(
                accountId = profile.accountId.value,
                id = profile.id,
                fullName = profile.fullName,
                phoneNumber = profile.phoneNumber,
                streetAddress = profile.streetAddress,
                city = profile.city,
                postalCode = profile.postalCode,
                avatarUrl = profile.avatarUrl,
                latitude = profile.latitude,
                longitude = profile.longitude,
                geocodedAt = profile.geocodedAt,
                cachedAtEpochSeconds = clock.nowEpochSeconds(),
            )
        )
    }

    override suspend fun getProfile(
        accountId: AccountId,
    ): DeliveryProfile? {
        val entity = dao.findByAccountId(accountId.value) ?: return null
        return DeliveryProfile(
            id = entity.id,
            accountId = AccountId(entity.accountId),
            fullName = entity.fullName,
            phoneNumber = entity.phoneNumber,
            streetAddress = entity.streetAddress,
            city = entity.city,
            postalCode = entity.postalCode,
            avatarUrl = entity.avatarUrl,
            latitude = entity.latitude,
            longitude = entity.longitude,
            geocodedAt = entity.geocodedAt,
        )
    }

    override suspend fun clearProfile(
        accountId: AccountId,
    ) {
        dao.deleteByAccountId(accountId.value)
    }
}
