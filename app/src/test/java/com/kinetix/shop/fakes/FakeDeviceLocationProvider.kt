package com.kinetix.shop.fakes

import com.kinetix.shop.models.profile.GeoPointCoordinates
import com.kinetix.shop.services.location.DeviceLocationProvider

class FakeDeviceLocationProvider : DeviceLocationProvider {
    var currentLocationResult: GeoPointCoordinates? = null

    override suspend fun getCurrentLocation(): GeoPointCoordinates? {
        return currentLocationResult
    }
}
