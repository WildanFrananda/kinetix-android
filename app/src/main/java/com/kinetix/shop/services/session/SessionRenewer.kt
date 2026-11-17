package com.kinetix.shop.services.session

import com.kinetix.shop.core.result.Result
import com.kinetix.shop.models.auth.StoredSession

interface SessionRenewer {
    suspend fun renew(failedAccessToken: String? = null): Result<StoredSession>
}
