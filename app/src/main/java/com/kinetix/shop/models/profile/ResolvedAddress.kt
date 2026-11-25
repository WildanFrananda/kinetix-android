package com.kinetix.shop.models.profile

data class ResolvedAddress(
    val streetAddress: String,
    val city: String,
    val postalCode: String,
    val coordinates: GeoPointCoordinates,
)
