package com.kinetix.shop.viewmodels.profile

import com.kinetix.shop.core.failure.ValidationFailure
import com.kinetix.shop.core.result.Ok
import com.kinetix.shop.fakes.FakeProfileRepository
import com.kinetix.shop.models.ids.AccountId
import com.kinetix.shop.models.profile.DeliveryProfile
import com.kinetix.shop.state.profile.ProfileLoadedUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
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
        val error = (state as ProfileLoadedUiState).errorFailure
        assertTrue(error is ValidationFailure)
        assertNull(repository.lastUpdatedInput)
    }
}
