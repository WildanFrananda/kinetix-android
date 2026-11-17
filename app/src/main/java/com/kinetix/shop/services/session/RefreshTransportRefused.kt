package com.kinetix.shop.services.session

data class RefreshTransportRefused(
    val statusCode: Int,
    val errorCode: String?,
    val message: String,
) : RefreshTransportOutcome
