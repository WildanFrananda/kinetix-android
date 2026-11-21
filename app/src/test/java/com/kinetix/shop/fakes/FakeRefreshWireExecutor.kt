package com.kinetix.shop.fakes

import com.kinetix.shop.services.session.RefreshWireExecutor
import com.kinetix.shop.services.session.RefreshWireOutcome

class FakeRefreshWireExecutor(
    var handler: suspend (String) -> RefreshWireOutcome,
) : RefreshWireExecutor {
    var callCount: Int = 0
        private set

    override suspend fun postRefresh(
        refreshToken: String,
    ): RefreshWireOutcome {
        callCount += 1
        return handler(refreshToken)
    }
}
