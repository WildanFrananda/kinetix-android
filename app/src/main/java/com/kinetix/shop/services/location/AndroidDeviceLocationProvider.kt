package com.kinetix.shop.services.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import com.kinetix.shop.core.concurrency.IoDispatcher
import com.kinetix.shop.models.profile.GeoPointCoordinates
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class AndroidDeviceLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DeviceLocationProvider {
    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): GeoPointCoordinates? {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) {
            return null
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        val providers = orderedProviders(locationManager, hasFine)
        for (provider in providers) {
            val fresh = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                requestCurrentLocationApi30(locationManager, provider)
            } else {
                requestCurrentLocationLegacy(locationManager, provider)
            }
            if (fresh != null) {
                return GeoPointCoordinates(fresh.latitude, fresh.longitude)
            }
        }

        return readBestLastKnown(locationManager)
    }

    private fun orderedProviders(
        locationManager: LocationManager,
        hasFine: Boolean,
    ): List<String> {
        val candidates = if (hasFine) {
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        } else {
            listOf(LocationManager.NETWORK_PROVIDER)
        }
        return candidates.filter { provider ->
            try {
                locationManager.isProviderEnabled(provider)
            } catch (_: Exception) {
                false
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestCurrentLocationApi30(
        locationManager: LocationManager,
        provider: String,
    ): Location? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return null
        }
        return try {
            withTimeoutOrNull(LOCATION_REQUEST_TIMEOUT_MS) {
                suspendCancellableCoroutine { continuation ->
                    val signal = CancellationSignal()
                    val executor = Executors.newSingleThreadExecutor()
                    continuation.invokeOnCancellation {
                        signal.cancel()
                        executor.shutdown()
                    }
                    locationManager.getCurrentLocation(
                        provider,
                        signal,
                        executor,
                    ) { location ->
                        executor.shutdown()
                        if (continuation.isActive) {
                            continuation.resume(location)
                        }
                    }
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    private suspend fun requestCurrentLocationLegacy(
        locationManager: LocationManager,
        provider: String,
    ): Location? {
        return try {
            withTimeoutOrNull(LOCATION_REQUEST_TIMEOUT_MS) {
                suspendCancellableCoroutine { continuation ->
                    val listener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            locationManager.removeUpdates(this)
                            if (continuation.isActive) {
                                continuation.resume(location)
                            }
                        }

                        @Suppress("OVERRIDE_DEPRECATION")
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

                        override fun onProviderEnabled(provider: String) {}

                        override fun onProviderDisabled(provider: String) {
                            locationManager.removeUpdates(this)
                            if (continuation.isActive) {
                                continuation.resume(null)
                            }
                        }
                    }
                    continuation.invokeOnCancellation {
                        locationManager.removeUpdates(listener)
                    }
                    locationManager.requestSingleUpdate(
                        provider,
                        listener,
                        Looper.getMainLooper(),
                    )
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun readBestLastKnown(
        locationManager: LocationManager,
    ): GeoPointCoordinates? {
        return withContext(ioDispatcher) {
            val nowMs = System.currentTimeMillis()
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER,
            )
            var best: Location? = null
            for (provider in providers) {
                val candidate = try {
                    locationManager.getLastKnownLocation(provider)
                } catch (_: Exception) {
                    null
                }
                val isFresh = candidate != null &&
                    candidate.time > 0L &&
                    nowMs - candidate.time <= MAX_CACHED_LOCATION_AGE_MS
                if (isFresh && (best == null || candidate.time > best.time)) {
                    best = candidate
                }
            }
            best?.let { GeoPointCoordinates(it.latitude, it.longitude) }
        }
    }

    companion object {
        private const val LOCATION_REQUEST_TIMEOUT_MS = 8_000L
        private const val MAX_CACHED_LOCATION_AGE_MS = 5L * 60L * 1000L
    }
}
