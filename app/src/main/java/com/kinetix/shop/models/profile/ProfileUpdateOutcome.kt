package com.kinetix.shop.models.profile

sealed interface ProfileUpdateOutcome {
    val profile: DeliveryProfile
}
