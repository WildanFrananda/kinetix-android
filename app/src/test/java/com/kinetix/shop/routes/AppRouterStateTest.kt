package com.kinetix.shop.routes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppRouterStateTest {
    @Test
    fun redirectsProtectedDestinationToSignInAndResumesAfterAuthentication() {
        val router = AppRouterState(initialRoute = HomeRoute)

        router.navigateTo(
            destination = ProfileRoute,
            isSignedIn = false,
        )

        assertEquals(SignInRoute, router.currentRoute.value)
        assertEquals(ProfileRoute, router.pendingDestinationAfterAuth)

        router.navigateTo(
            destination = TwoFactorVerifyRoute("chal-1"),
            isSignedIn = false,
        )
        assertEquals(TwoFactorVerifyRoute("chal-1"), router.currentRoute.value)

        router.onAuthenticated()
        assertEquals(ProfileRoute, router.currentRoute.value)
        assertNull(router.pendingDestinationAfterAuth)
    }

    @Test
    fun signOutPopsProtectedRoutesBackToPublicRoute() {
        val router = AppRouterState(initialRoute = HomeRoute)
        val detailRoute = ProductDetailRoute("SKU-100")

        router.navigateTo(
            destination = detailRoute,
            isSignedIn = true,
        )
        router.navigateTo(
            destination = ProfileRoute,
            isSignedIn = true,
        )
        router.navigateTo(
            destination = TwoFactorSettingsRoute,
            isSignedIn = true,
        )

        assertEquals(TwoFactorSettingsRoute, router.currentRoute.value)

        router.onSignedOut()
        assertEquals(detailRoute, router.currentRoute.value)
    }
}
