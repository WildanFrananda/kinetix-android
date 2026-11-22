package com.kinetix.shop.viewmodels.profile

import com.kinetix.shop.core.failure.ClientValidationFailure
import com.kinetix.shop.core.failure.ClientValidationReason
import com.kinetix.shop.core.failure.NotFoundFailure
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.fakes.FakeProfileRepository
import com.kinetix.shop.models.ids.AccountId
import com.kinetix.shop.models.profile.DeliveryProfile
import com.kinetix.shop.models.profile.ProfileUpdateConfirmed
import com.kinetix.shop.models.profile.ProfileUpdateQueued
import com.kinetix.shop.state.profile.ProfileLoadedUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun treatsNotFound404AsEmptyEditableProfileAndAllowsCreatingFirstProfile() = runTest(testDispatcher) {
        val createdProfile = DeliveryProfile(
            id = 9L,
            accountId = AccountId(1L),
            fullName = "Wildan Frananda",
            phoneNumber = "+628123456789",
            streetAddress = "Jl. Merdeka No. 10",
            city = "Jakarta",
            postalCode = "10110",
            avatarUrl = null,
            latitude = -6.175,
            longitude = 106.827,
            geocodedAt = "2026-11-19T10:00:00Z",
        )
        val repository = FakeProfileRepository().apply {
            getProfileResult = Err(NotFoundFailure("HTTP_404", "Not found"))
            updateProfileResult = Ok(ProfileUpdateConfirmed(createdProfile))
        }
        val viewModel = ProfileViewModel(repository)

        viewModel.loadProfile()
        advanceUntilIdle()

        val emptyState = viewModel.uiState.value
        assertTrue(emptyState is ProfileLoadedUiState)
        assertNull((emptyState as ProfileLoadedUiState).profile)

        viewModel.saveProfile(
            fullNameInput = "Wildan Frananda",
            phoneNumberInput = "+628123456789",
            streetAddressInput = "Jl. Merdeka No. 10",
            cityInput = "Jakarta",
            postalCodeInput = "10110",
        )
        advanceUntilIdle()

        val savedState = viewModel.uiState.value as ProfileLoadedUiState
        assertEquals(createdProfile, savedState.profile)
        assertTrue(savedState.savedBanner)
        assertFalse(savedState.queuedOfflineBanner)
        assertEquals("Wildan Frananda", repository.lastUpdatedInput?.fullName)
    }

    @Test
    fun showsQueuedOfflineBannerWhenUpdateIsQueuedInOutbox() = runTest(testDispatcher) {
        val initialProfile = DeliveryProfile(
            id = 1L,
            accountId = AccountId(1L),
            fullName = "Budi Santoso",
            phoneNumber = "+628111111",
            streetAddress = "Jl. Sudirman No. 1",
            city = "Jakarta",
            postalCode = "10220",
            avatarUrl = null,
            latitude = -6.2,
            longitude = 106.8,
            geocodedAt = "2026-10-20T09:00:00Z",
        )
        val optimisticProfile = initialProfile.copy(
            streetAddress = "Jl. Offline No. 7",
            isConfirmed = false,
        )
        val repository = FakeProfileRepository().apply {
            getProfileResult = Ok(initialProfile)
            updateProfileResult = Ok(ProfileUpdateQueued(optimisticProfile))
        }
        val viewModel = ProfileViewModel(repository)

        viewModel.loadProfile()
        advanceUntilIdle()

        viewModel.saveProfile(
            fullNameInput = "Budi Santoso",
            phoneNumberInput = "+628111111",
            streetAddressInput = "Jl. Offline No. 7",
            cityInput = "Jakarta",
            postalCodeInput = "10220",
        )
        advanceUntilIdle()

        val queuedState = viewModel.uiState.value as ProfileLoadedUiState
        assertEquals(optimisticProfile, queuedState.profile)
        assertFalse(queuedState.savedBanner)
        assertTrue(queuedState.queuedOfflineBanner)
    }

    @Test
    fun rejectsBlankRequiredFieldsBeforeCallingRepository() = runTest(testDispatcher) {
        val initialProfile = DeliveryProfile(
            id = 1L,
            accountId = AccountId(1L),
            fullName = "Budi Santoso",
            phoneNumber = "+628111111",
            streetAddress = "Jl. Sudirman No. 1",
            city = "Jakarta",
            postalCode = "10220",
            avatarUrl = null,
            latitude = -6.2,
            longitude = 106.8,
            geocodedAt = "2026-10-20T09:00:00Z",
        )
        val repository = FakeProfileRepository().apply {
            getProfileResult = Ok(initialProfile)
        }
        val viewModel = ProfileViewModel(repository)

        viewModel.loadProfile()
        advanceUntilIdle()

        viewModel.saveProfile(
            fullNameInput = "   ",
            phoneNumberInput = "+628111111",
            streetAddressInput = "Jl. Sudirman No. 1",
            cityInput = "Jakarta",
            postalCodeInput = "10220",
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ProfileLoadedUiState)
        assertEquals(
            ClientValidationFailure(ClientValidationReason.PROFILE_FIELDS_REQUIRED),
            (state as ProfileLoadedUiState).errorFailure,
        )
        assertNull(repository.lastUpdatedInput)
    }
}
