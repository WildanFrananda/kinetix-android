package com.kinetix.shop.fakes

import com.kinetix.shop.models.common.DataOrigin
import com.kinetix.shop.models.ids.AccountId
import com.kinetix.shop.models.profile.DeliveryProfile
import com.kinetix.shop.services.local.ProfileCacheStore

class FakeProfileCacheStore : ProfileCacheStore {
    private val profiles = mutableMapOf<Long, DeliveryProfile>()
    private val confirmedProfiles = mutableMapOf<Long, DeliveryProfile>()

    override suspend fun saveProfile(
        profile: DeliveryProfile,
    ) {
        val confirmed = profile.copy(
            isConfirmed = true,
            dataOrigin = DataOrigin.CACHE,
            cachedAtEpochSeconds = profile.cachedAtEpochSeconds ?: 1_700_000_000L,
        )
        profiles[profile.accountId.value] = confirmed
        confirmedProfiles[profile.accountId.value] = confirmed
    }

    override suspend fun saveOptimisticProfile(
        profile: DeliveryProfile,
    ) {
        val existing = profiles[profile.accountId.value]
        if (existing != null && existing.isConfirmed && !confirmedProfiles.containsKey(profile.accountId.value)) {
            confirmedProfiles[profile.accountId.value] = existing
        }
        profiles[profile.accountId.value] = profile.copy(
            isConfirmed = false,
            dataOrigin = DataOrigin.CACHE,
            cachedAtEpochSeconds = profile.cachedAtEpochSeconds ?: 1_700_000_000L,
        )
    }

    override suspend fun getProfile(
        accountId: AccountId,
    ): DeliveryProfile? {
        return profiles[accountId.value]
    }

    override suspend fun getConfirmedProfile(
        accountId: AccountId,
    ): DeliveryProfile? {
        return confirmedProfiles[accountId.value]
    }

    override suspend fun withdrawOptimisticProfile(
        accountId: AccountId,
    ) {
        val confirmed = confirmedProfiles[accountId.value]
        if (confirmed != null) {
            profiles[accountId.value] = confirmed
        } else {
            profiles.remove(accountId.value)
        }
    }

    override suspend fun clearProfile(
        accountId: AccountId,
    ) {
        profiles.remove(accountId.value)
        confirmedProfiles.remove(accountId.value)
    }
}
