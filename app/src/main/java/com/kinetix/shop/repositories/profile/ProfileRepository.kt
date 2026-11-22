package com.kinetix.shop.repositories.profile

import com.kinetix.shop.core.result.Result
import com.kinetix.shop.models.profile.DeliveryProfile
import com.kinetix.shop.models.profile.ProfileUpdateOutcome
import com.kinetix.shop.models.profile.UpdateDeliveryProfileInput

interface ProfileRepository {
    suspend fun getProfile(): Result<DeliveryProfile>

    suspend fun updateProfile(
        input: UpdateDeliveryProfileInput,
    ): Result<ProfileUpdateOutcome>
}
