package com.kinetix.shop.repositories.profile

import com.kinetix.shop.core.network.SafeApiCaller
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.core.result.Result
import com.kinetix.shop.dto.profile.UserProfileResponseDto
import com.kinetix.shop.models.ids.AccountId
import com.kinetix.shop.models.profile.DeliveryProfile
import com.kinetix.shop.models.profile.UpdateDeliveryProfileInput
import com.kinetix.shop.repositories.auth.SessionRepository
import com.kinetix.shop.services.remote.IdentityApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val apiService: IdentityApiService,
    private val safeApiCaller: SafeApiCaller,
    private val sessionRepository: SessionRepository,
) : ProfileRepository {
    override suspend fun getProfile(): Result<DeliveryProfile> {
        return sessionRepository.withValidSession { session ->
            safeApiCaller.execute(
                call = {
                    apiService.getProfile(
                        accountId = session.accountId.value,
                        bearerToken = "Bearer ${session.accessToken}",
                    )
                },
                transform = { dto ->
                    Ok(mapToDomain(dto))
                },
            )
        }
    }

    override suspend fun updateProfile(
        input: UpdateDeliveryProfileInput,
    ): Result<DeliveryProfile> {
        return sessionRepository.withValidSession { session ->
            safeApiCaller.execute(
                call = {
                    apiService.updateProfile(
                        accountId = session.accountId.value,
                        bearerToken = "Bearer ${session.accessToken}",
                        body = input.toJsonObject(),
                    )
                },
                transform = { dto ->
                    Ok(mapToDomain(dto))
                },
            )
        }
    }

    private fun mapToDomain(
        dto: UserProfileResponseDto,
    ): DeliveryProfile {
        return DeliveryProfile(
            id = dto.id,
            accountId = AccountId(dto.userId),
            fullName = dto.fullName,
            phoneNumber = dto.phoneNumber,
            streetAddress = dto.streetAddress,
            city = dto.city,
            postalCode = dto.postalCode,
            avatarUrl = dto.avatarUrl,
            latitude = dto.latitude,
            longitude = dto.longitude,
            geocodedAt = dto.geocodedAt,
        )
    }
}
