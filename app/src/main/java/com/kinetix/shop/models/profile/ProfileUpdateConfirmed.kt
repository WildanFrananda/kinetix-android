package com.kinetix.shop.models.profile

data class ProfileUpdateConfirmed(
    override val profile: DeliveryProfile,
) : ProfileUpdateOutcome
