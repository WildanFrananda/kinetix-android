package com.kinetix.shop.viewmodels.root

import com.kinetix.shop.fakes.FakeFeatureFlagProvider
import com.kinetix.shop.fakes.FakeSessionRepository
import com.kinetix.shop.services.config.FeatureFlagProvider
import kotlinx.coroutines.CompletableDeferred
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

    @Test
    fun appliesActivatedForceUpdateImmediatelyBeforeDelayedRefreshCompletes() = runTest(testDispatcher) {
        val refreshGate = CompletableDeferred<Boolean>()
        val activatedProvider = object : FeatureFlagProvider {
            override fun isForceUpdateRequired(currentVersionCode: Int): Boolean = true
            override fun isCheckoutEnabled(): Boolean = true
            override fun isWalletTopUpEnabled(): Boolean = true
            override fun isLiveTrackingEnabled(): Boolean = true
            override fun isOutboxSyncEnabled(): Boolean = true
            override suspend fun refreshFlags(): Boolean = refreshGate.await()
        }

        val viewModel = AppRootViewModel(
            sessionRepository = FakeSessionRepository(),
            featureFlagProvider = activatedProvider,
        )

        assertTrue(viewModel.forceUpdateRequired.value)

        refreshGate.complete(true)
        advanceUntilIdle()
        assertTrue(viewModel.forceUpdateRequired.value)
    }
}
