package com.kinetix.shop.fakes

import com.kinetix.shop.models.profile.GeoPointCoordinates
import com.kinetix.shop.models.profile.ResolvedAddress
import com.kinetix.shop.services.location.AddressGeocoder

class FakeAddressGeocoder : AddressGeocoder {
    var reverseGeocodeHandler: suspend (Double, Double) -> ResolvedAddress? = { _, _ -> null }
    var forwardGeocodeHandler: suspend (String, String, String) -> GeoPointCoordinates? = { _, _, _ -> null }
    var reverseCalls: Int = 0
    var forwardCalls: Int = 0

    override suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double,
    ): ResolvedAddress? {
        reverseCalls += 1
        return reverseGeocodeHandler(latitude, longitude)
    }

    override suspend fun forwardGeocode(
        streetAddress: String,
        city: String,
        postalCode: String,
    ): GeoPointCoordinates? {
        forwardCalls += 1
        return forwardGeocodeHandler(streetAddress, city, postalCode)
    }
}
