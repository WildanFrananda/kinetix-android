package com.kinetix.shop.fakes

import com.kinetix.shop.core.failure.UnauthorizedFailure
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Result
import com.kinetix.shop.models.profile.DeliveryProfile
import com.kinetix.shop.models.profile.ProfileUpdateOutcome
import com.kinetix.shop.models.profile.UpdateDeliveryProfileInput
import com.kinetix.shop.repositories.profile.ProfileRepository

class FakeProfileRepository : ProfileRepository {
    var getProfileResult: Result<DeliveryProfile> = Err(
        UnauthorizedFailure("NOT_CONFIGURED")
    )
    var updateProfileResult: Result<ProfileUpdateOutcome> = Err(
        UnauthorizedFailure("NOT_CONFIGURED")
    )
    var lastUpdatedInput: UpdateDeliveryProfileInput? = null

    override suspend fun getProfile(): Result<DeliveryProfile> {
        return getProfileResult
    }

    override suspend fun updateProfile(
        input: UpdateDeliveryProfileInput,
    ): Result<ProfileUpdateOutcome> {
        lastUpdatedInput = input
        return updateProfileResult
    }
}
