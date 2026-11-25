package com.kinetix.shop.services.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.kinetix.shop.core.concurrency.IoDispatcher
import com.kinetix.shop.models.profile.GeoPointCoordinates
import com.kinetix.shop.models.profile.ResolvedAddress
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

@Singleton
class AndroidAddressGeocoder @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AddressGeocoder {
    override suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double,
    ): ResolvedAddress? {
        if (!Geocoder.isPresent()) {
            return null
        }

        val geocoder = Geocoder(context, Locale.getDefault())
        val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            reverseGeocodeApi33(geocoder, latitude, longitude)
        } else {
            reverseGeocodeLegacy(geocoder, latitude, longitude)
        } ?: return null

        val street = buildStreetAddress(address)
        val city = resolveCity(address)
        val postalCode = address.postalCode?.trim().orEmpty()
        if (street.isEmpty() && city.isEmpty() && postalCode.isEmpty()) {
            return null
        }

        return ResolvedAddress(
            streetAddress = street,
            city = city,
            postalCode = postalCode,
            coordinates = GeoPointCoordinates(
                latitude = latitude,
                longitude = longitude,
            ),
        )
    }

    override suspend fun forwardGeocode(
        streetAddress: String,
        city: String,
        postalCode: String,
    ): GeoPointCoordinates? {
        if (!Geocoder.isPresent()) {
            return null
        }

        val query = listOf(streetAddress, city, postalCode)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(", ")
        if (query.isEmpty()) {
            return null
        }

        val geocoder = Geocoder(context, Locale.getDefault())
        val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            forwardGeocodeApi33(geocoder, query)
        } else {
            forwardGeocodeLegacy(geocoder, query)
        } ?: return null

        return GeoPointCoordinates(
            latitude = address.latitude,
            longitude = address.longitude,
        )
    }

    private suspend fun reverseGeocodeApi33(
        geocoder: Geocoder,
        latitude: Double,
        longitude: Double,
    ): Address? {
        return try {
            suspendCancellableCoroutine { continuation ->
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    continuation.resume(null)
                    return@suspendCancellableCoroutine
                }
                geocoder.getFromLocation(
                    latitude,
                    longitude,
                    1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (continuation.isActive) {
                                continuation.resume(addresses.firstOrNull())
                            }
                        }

                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) {
                                continuation.resume(null)
                            }
                        }
                    },
                )
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun forwardGeocodeApi33(
        geocoder: Geocoder,
        query: String,
    ): Address? {
        return try {
            suspendCancellableCoroutine { continuation ->
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    continuation.resume(null)
                    return@suspendCancellableCoroutine
                }
                geocoder.getFromLocationName(
                    query,
                    1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (continuation.isActive) {
                                continuation.resume(addresses.firstOrNull())
                            }
                        }

                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) {
                                continuation.resume(null)
                            }
                        }
                    },
                )
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun reverseGeocodeLegacy(
        geocoder: Geocoder,
        latitude: Double,
        longitude: Double,
    ): Address? {
        return withContext(ioDispatcher) {
            try {
                geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                null
            }
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun forwardGeocodeLegacy(
        geocoder: Geocoder,
        query: String,
    ): Address? {
        return withContext(ioDispatcher) {
            try {
                geocoder.getFromLocationName(query, 1)?.firstOrNull()
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun buildStreetAddress(
        address: Address,
    ): String {
        val thoroughfare = address.thoroughfare?.trim().orEmpty()
        val subThoroughfare = address.subThoroughfare?.trim().orEmpty()
        val combined = listOf(thoroughfare, subThoroughfare)
            .filter { it.isNotEmpty() }
            .joinToString(" ")
        if (combined.isNotEmpty()) {
            return combined.take(255)
        }
        val firstLine = address.getAddressLine(0)?.trim().orEmpty()
        return firstLine.substringBefore(',').trim().take(255)
    }

    private fun resolveCity(
        address: Address,
    ): String {
        val candidates = listOf(
            address.subAdminArea,
            address.locality,
            address.adminArea,
        )
        return candidates
            .firstOrNull { !it.isNullOrBlank() }
            ?.trim()
            ?.take(100)
            .orEmpty()
    }
}
