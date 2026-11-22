package com.kinetix.shop.models.profile

data class ProfileUpdateQueued(
    override val profile: DeliveryProfile,
) : ProfileUpdateOutcome
