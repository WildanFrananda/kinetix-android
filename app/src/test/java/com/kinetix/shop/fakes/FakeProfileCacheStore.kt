package com.kinetix.shop.fakes

import com.kinetix.shop.models.ids.AccountId
import com.kinetix.shop.models.profile.DeliveryProfile
import com.kinetix.shop.services.local.ProfileCacheStore

class FakeProfileCacheStore : ProfileCacheStore {
    private val profiles = mutableMapOf<Long, DeliveryProfile>()

    override suspend fun saveProfile(
        profile: DeliveryProfile,
    ) {
        profiles[profile.accountId.value] = profile
    }

    override suspend fun getProfile(
        accountId: AccountId,
    ): DeliveryProfile? {
        return profiles[accountId.value]
    }

    override suspend fun clearProfile(
        accountId: AccountId,
    ) {
        profiles.remove(accountId.value)
    }
}
