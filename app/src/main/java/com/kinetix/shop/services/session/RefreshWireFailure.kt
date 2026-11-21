package com.kinetix.shop.services.session

data class RefreshWireFailure(
    val throwable: Throwable,
    val leftDevice: Boolean,
) : RefreshWireOutcome
