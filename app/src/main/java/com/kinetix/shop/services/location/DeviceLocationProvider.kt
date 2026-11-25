package com.kinetix.shop.services.location

import com.kinetix.shop.models.profile.GeoPointCoordinates

interface DeviceLocationProvider {
    suspend fun getCurrentLocation(): GeoPointCoordinates?
}
