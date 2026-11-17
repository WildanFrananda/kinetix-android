package com.kinetix.shop.models.auth

data class SignInAuthenticated(val session: StoredSession) : SignInOutcome
