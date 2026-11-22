package com.kinetix.shop.services.local

import com.kinetix.shop.core.time.Clock
import com.kinetix.shop.models.common.DataOrigin
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
        val now = clock.nowEpochSeconds()
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
                cachedAtEpochSeconds = now,
                isConfirmed = true,
                confirmedFullName = profile.fullName,
                confirmedPhoneNumber = profile.phoneNumber,
                confirmedStreetAddress = profile.streetAddress,
                confirmedCity = profile.city,
                confirmedPostalCode = profile.postalCode,
                confirmedAvatarUrl = profile.avatarUrl,
                hasConfirmedSnapshot = true,
            )
        )
    }

    override suspend fun saveOptimisticProfile(
        profile: DeliveryProfile,
    ) {
        val now = clock.nowEpochSeconds()
        val existing = dao.findByAccountId(profile.accountId.value)
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
                cachedAtEpochSeconds = now,
                isConfirmed = false,
                confirmedFullName = existing?.confirmedFullName ?: existing?.fullName,
                confirmedPhoneNumber = existing?.confirmedPhoneNumber ?: existing?.phoneNumber,
                confirmedStreetAddress = existing?.confirmedStreetAddress ?: existing?.streetAddress,
                confirmedCity = existing?.confirmedCity ?: existing?.city,
                confirmedPostalCode = existing?.confirmedPostalCode ?: existing?.postalCode,
                confirmedAvatarUrl = if (existing?.hasConfirmedSnapshot == true) {
                    existing.confirmedAvatarUrl
                } else {
                    existing?.avatarUrl
                },
                hasConfirmedSnapshot = existing != null,
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
            isConfirmed = entity.isConfirmed,
            dataOrigin = DataOrigin.CACHE,
            cachedAtEpochSeconds = entity.cachedAtEpochSeconds,
        )
    }

    override suspend fun getConfirmedProfile(
        accountId: AccountId,
    ): DeliveryProfile? {
        val entity = dao.findByAccountId(accountId.value) ?: return null
        if (!entity.hasConfirmedSnapshot && !entity.isConfirmed) {
            return null
        }

        return DeliveryProfile(
            id = entity.id,
            accountId = AccountId(entity.accountId),
            fullName = entity.confirmedFullName ?: entity.fullName,
            phoneNumber = entity.confirmedPhoneNumber ?: entity.phoneNumber,
            streetAddress = entity.confirmedStreetAddress ?: entity.streetAddress,
            city = entity.confirmedCity ?: entity.city,
            postalCode = entity.confirmedPostalCode ?: entity.postalCode,
            avatarUrl = if (entity.hasConfirmedSnapshot) {
                entity.confirmedAvatarUrl
            } else {
                entity.avatarUrl
            },
            latitude = entity.latitude,
            longitude = entity.longitude,
            geocodedAt = entity.geocodedAt,
            isConfirmed = true,
            dataOrigin = DataOrigin.CACHE,
            cachedAtEpochSeconds = entity.cachedAtEpochSeconds,
        )
    }

    override suspend fun withdrawOptimisticProfile(
        accountId: AccountId,
    ) {
        val existing = dao.findByAccountId(accountId.value) ?: return
        if (!existing.hasConfirmedSnapshot) {
            dao.deleteByAccountId(accountId.value)
            return
        }

        dao.upsertProfile(
            existing.copy(
                fullName = existing.confirmedFullName ?: existing.fullName,
                phoneNumber = existing.confirmedPhoneNumber ?: existing.phoneNumber,
                streetAddress = existing.confirmedStreetAddress ?: existing.streetAddress,
                city = existing.confirmedCity ?: existing.city,
                postalCode = existing.confirmedPostalCode ?: existing.postalCode,
                avatarUrl = existing.confirmedAvatarUrl,
                isConfirmed = true,
            )
        )
    }

    override suspend fun clearProfile(
        accountId: AccountId,
    ) {
        dao.deleteByAccountId(accountId.value)
    }
}
