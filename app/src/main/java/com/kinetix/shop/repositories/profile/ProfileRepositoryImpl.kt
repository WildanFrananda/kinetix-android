package com.kinetix.shop.repositories.profile

import com.kinetix.shop.core.failure.Failure
import com.kinetix.shop.core.failure.NetworkFailure
import com.kinetix.shop.core.failure.NotFoundFailure
import com.kinetix.shop.core.failure.TimeoutFailure
import com.kinetix.shop.core.failure.UnavailableFailure
import com.kinetix.shop.core.network.SafeApiCaller
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.core.result.Result
import com.kinetix.shop.dto.profile.UserProfileResponseDto
import com.kinetix.shop.models.ids.AccountId
import com.kinetix.shop.models.outbox.OutboxOperationType
import com.kinetix.shop.models.profile.DeliveryProfile
import com.kinetix.shop.models.profile.UpdateDeliveryProfileInput
import com.kinetix.shop.repositories.auth.SessionRepository
import com.kinetix.shop.services.local.OutboxStore
import com.kinetix.shop.services.local.ProfileCacheStore
import com.kinetix.shop.services.outbox.OutboxSyncScheduler
import com.kinetix.shop.services.remote.IdentityApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val apiService: IdentityApiService,
    private val safeApiCaller: SafeApiCaller,
    private val sessionRepository: SessionRepository,
    private val profileCacheStore: ProfileCacheStore,
    private val outboxStore: OutboxStore,
    private val outboxSyncScheduler: OutboxSyncScheduler,
) : ProfileRepository {
    override suspend fun getProfile(): Result<DeliveryProfile> {
        return sessionRepository.withValidSession { session ->
            val networkResult = safeApiCaller.execute(
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
            when (networkResult) {
                is Ok -> {
                    profileCacheStore.saveProfile(networkResult.value)
                    networkResult
                }

                is Err -> {
                    if (networkResult.failure is NotFoundFailure) {
                        profileCacheStore.clearProfile(session.accountId)
                        return@withValidSession networkResult
                    }
                    if (canFallbackToCache(networkResult.failure)) {
                        val cached = profileCacheStore.getProfile(session.accountId)
                        if (cached != null) {
                            return@withValidSession Ok(cached)
                        }
                    }
                    networkResult
                }
            }
        }
    }

    override suspend fun updateProfile(
        input: UpdateDeliveryProfileInput,
    ): Result<DeliveryProfile> {
        return sessionRepository.withValidSession { session ->
            val payloadObject = input.toJsonObject()
            val networkResult = safeApiCaller.execute(
                call = {
                    apiService.updateProfile(
                        accountId = session.accountId.value,
                        bearerToken = "Bearer ${session.accessToken}",
                        body = payloadObject,
                    )
                },
                transform = { dto ->
                    Ok(mapToDomain(dto))
                },
            )
            when (networkResult) {
                is Ok -> {
                    profileCacheStore.saveProfile(networkResult.value)
                    networkResult
                }

                is Err -> {
                    if (isOfflineMutationEligible(networkResult.failure)) {
                        val existing = profileCacheStore.getProfile(session.accountId)
                        if (existing != null) {
                            val optimistic = applyInput(existing, input)
                            profileCacheStore.saveProfile(optimistic)
                            outboxStore.enqueue(
                                principalId = session.principalId,
                                operationType = OutboxOperationType.PROFILE_UPDATE,
                                dedupeKey = "profile:${session.accountId.value}",
                                payloadJson = payloadObject.toString(),
                            )
                            outboxSyncScheduler.scheduleSync()
                            return@withValidSession Ok(optimistic)
                        }
                    }
                    networkResult
                }
            }
        }
    }

    private fun applyInput(
        existing: DeliveryProfile,
        input: UpdateDeliveryProfileInput,
    ): DeliveryProfile {
        val nextAvatar = if (input.clearAvatar) {
            null
        } else {
            input.avatarUrl ?: existing.avatarUrl
        }
        return existing.copy(
            fullName = input.fullName ?: existing.fullName,
            phoneNumber = input.phoneNumber ?: existing.phoneNumber,
            streetAddress = input.streetAddress ?: existing.streetAddress,
            city = input.city ?: existing.city,
            postalCode = input.postalCode ?: existing.postalCode,
            avatarUrl = nextAvatar,
        )
    }

    private fun canFallbackToCache(
        failure: Failure,
    ): Boolean {
        return when (failure) {
            is NetworkFailure,
            is TimeoutFailure,
            is UnavailableFailure -> true

            else -> false
        }
    }

    private fun isOfflineMutationEligible(
        failure: Failure,
    ): Boolean {
        return when (failure) {
            is NetworkFailure,
            is TimeoutFailure -> true

            else -> false
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
