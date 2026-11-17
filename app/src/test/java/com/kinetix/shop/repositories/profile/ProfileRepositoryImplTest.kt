package com.kinetix.shop.repositories.profile

import com.kinetix.shop.core.network.ApiErrorParser
import com.kinetix.shop.core.network.HttpFailureMapper
import com.kinetix.shop.core.network.SafeApiCaller
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.dto.profile.UserProfileResponseDto
import com.kinetix.shop.fakes.FakeIdentityApiService
import com.kinetix.shop.fakes.FakeSessionRepository
import com.kinetix.shop.models.auth.StoredSession
import com.kinetix.shop.models.ids.AccountId
import com.kinetix.shop.models.ids.PrincipalId
import com.kinetix.shop.models.profile.UpdateDeliveryProfileInput
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class ProfileRepositoryImplTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }
    private val errorParser = ApiErrorParser(json)
    private val safeApiCaller = SafeApiCaller(HttpFailureMapper(errorParser))

    @Test
    fun updateProfileSerializesOnlyChangedFieldsAndExplicitAvatarNull() = runTest {
        val initialSession = StoredSession(
            accessToken = "acc-1",
            refreshToken = "ref-1",
            expiresAtEpochSeconds = 1_700_000_900L,
            accountId = AccountId(22L),
            principalId = PrincipalId("usr-22"),
            email = "buyer@kinetix.test",
        )
        var capturedAvatarIsJsonNull = false
        var hasPhoneNumberField = false
        val apiService = FakeIdentityApiService().apply {
            updateProfileHandler = { _, _, body ->
                capturedAvatarIsJsonNull = body["avatarUrl"] is JsonNull
                hasPhoneNumberField = body.containsKey("phoneNumber")
                Response.success(
                    UserProfileResponseDto(
                        id = 1L,
                        userId = 22L,
                        fullName = "Budi Santoso",
                        phoneNumber = "+628123456789",
                        streetAddress = "Jl. Sudirman No. 1",
                        city = "Jakarta",
                        postalCode = "10220",
                        avatarUrl = null,
                        latitude = -6.2,
                        longitude = 106.8,
                        geocodedAt = "2026-10-20T10:00:00Z",
                    )
                )
            }
        }
        val repository = ProfileRepositoryImpl(
            apiService = apiService,
            safeApiCaller = safeApiCaller,
            sessionRepository = FakeSessionRepository(initialSession = initialSession),
        )

        val result = repository.updateProfile(
            UpdateDeliveryProfileInput(
                streetAddress = "Jl. Sudirman No. 1",
                clearAvatar = true,
            )
        )
        assertTrue(result is Ok)
        assertTrue(capturedAvatarIsJsonNull)
        assertFalse(hasPhoneNumberField)
        assertEquals("Jl. Sudirman No. 1", (result as Ok).value.streetAddress)
    }
}
