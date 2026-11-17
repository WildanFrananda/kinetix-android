package com.kinetix.shop.services.session

import com.kinetix.shop.models.auth.StoredSession

data class RefreshTransportRotated(val session: StoredSession) : RefreshTransportOutcome
