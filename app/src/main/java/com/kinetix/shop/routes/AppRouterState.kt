package com.kinetix.shop.routes

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppRouterState(
    initialRoute: AppRoute = HomeRoute,
) {
    private val backStack = ArrayDeque<AppRoute>().apply {
        addLast(initialRoute)
    }

    private val currentRouteFlow = MutableStateFlow(initialRoute)
    val currentRoute: StateFlow<AppRoute> = currentRouteFlow.asStateFlow()

    var pendingDestinationAfterAuth: AppRoute? = null
        private set

    fun navigateTo(
        destination: AppRoute,
        isSignedIn: Boolean,
    ) {
        if (destination.requiresAuthentication && !isSignedIn) {
            pendingDestinationAfterAuth = destination
            pushRoute(SignInRoute)
            return
        }
        pushRoute(destination)
    }

    fun onAuthenticated() {
        val pending = pendingDestinationAfterAuth
        pendingDestinationAfterAuth = null

        while (
            backStack.isNotEmpty() &&
            (
                backStack.last() is SignInRoute ||
                backStack.last() is RegisterRoute ||
                backStack.last() is TwoFactorVerifyRoute
            )
        ) {
            backStack.removeLast()
        }

        if (backStack.isEmpty()) {
            backStack.addLast(HomeRoute)
        }

        if (pending != null) {
            backStack.addLast(pending)
        }

        currentRouteFlow.value = backStack.last()
    }

    fun onSignedOut() {
        pendingDestinationAfterAuth = null
        while (backStack.size > 1 && backStack.last().requiresAuthentication) {
            backStack.removeLast()
        }
        if (backStack.last().requiresAuthentication) {
            backStack.clear()
            backStack.addLast(HomeRoute)
        }
        currentRouteFlow.value = backStack.last()
    }

    fun navigateBack(): Boolean {
        if (backStack.size <= 1) {
            return false
        }
        backStack.removeLast()
        currentRouteFlow.value = backStack.last()
        return true
    }

    private fun pushRoute(route: AppRoute) {
        if (backStack.lastOrNull() == route) {
            return
        }
        backStack.addLast(route)
        currentRouteFlow.value = route
    }
}
