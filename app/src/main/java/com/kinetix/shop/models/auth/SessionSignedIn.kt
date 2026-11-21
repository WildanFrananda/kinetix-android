package com.kinetix.shop.models.auth

data class SessionSignedIn(
    val session: StoredSession,
) : SessionState
