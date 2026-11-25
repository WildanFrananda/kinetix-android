package com.kinetix.shop.services.location

import com.kinetix.shop.models.profile.GeoPointCoordinates
import com.kinetix.shop.models.profile.ResolvedAddress

interface AddressGeocoder {
    suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double,
    ): ResolvedAddress?

    suspend fun forwardGeocode(
        streetAddress: String,
        city: String,
        postalCode: String,
    ): GeoPointCoordinates?
}
