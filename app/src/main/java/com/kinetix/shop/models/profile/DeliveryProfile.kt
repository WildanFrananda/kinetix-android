package com.kinetix.shop.models.profile

import com.kinetix.shop.models.common.DataOrigin
import com.kinetix.shop.models.ids.AccountId

data class DeliveryProfile(
    val id: Long,
    val accountId: AccountId,
    val fullName: String,
    val phoneNumber: String,
    val streetAddress: String,
    val city: String,
    val postalCode: String,
    val avatarUrl: String?,
    val latitude: Double?,
    val longitude: Double?,
    val geocodedAt: String?,
    val isConfirmed: Boolean = true,
    val dataOrigin: DataOrigin = DataOrigin.LIVE,
    val cachedAtEpochSeconds: Long? = null,
)
