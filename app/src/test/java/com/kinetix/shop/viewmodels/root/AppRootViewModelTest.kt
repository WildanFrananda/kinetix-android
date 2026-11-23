package com.kinetix.shop.viewmodels.root

import com.kinetix.shop.fakes.FakeFeatureFlagProvider
import com.kinetix.shop.fakes.FakeSessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppRootViewModelTest {
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
    fun refreshesFlagsAtStartupAndForegroundAndExposesForceUpdateState() = runTest(testDispatcher) {
        val featureFlags = FakeFeatureFlagProvider(forceUpdateRequired = false)
        val viewModel = AppRootViewModel(
            sessionRepository = FakeSessionRepository(),
            featureFlagProvider = featureFlags,
        )
        advanceUntilIdle()
        assertFalse(viewModel.forceUpdateRequired.value)

        featureFlags.forceUpdateRequired = true
        viewModel.onAppForegrounded()
        advanceUntilIdle()
        assertTrue(viewModel.forceUpdateRequired.value)
    }
}
