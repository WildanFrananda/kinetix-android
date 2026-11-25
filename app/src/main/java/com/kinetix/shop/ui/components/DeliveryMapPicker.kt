package com.kinetix.shop.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kinetix.shop.R
import com.kinetix.shop.models.profile.ProfileMapStatusNotice
import com.kinetix.shop.ui.theme.KinetixPalette
import java.io.File
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

@Composable
fun DeliveryMapPicker(
    latitude: Double?,
    longitude: Double?,
    isGeocoding: Boolean,
    statusNotice: ProfileMapStatusNotice?,
    enabled: Boolean,
    onSelectCoordinates: (Double, Double) -> Unit,
    onRequestCurrentLocation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnSelect by rememberUpdatedState(onSelectCoordinates)
    val latestEnabled by rememberUpdatedState(enabled)

    val mapView = remember(context) {
        val config = Configuration.getInstance()
        config.userAgentValue = context.packageName
        val tileDir = File(context.cacheDir, "osmdroid")
        config.osmdroidBasePath = tileDir
        config.osmdroidTileCache = File(tileDir, "tiles")

        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(if (latitude != null && longitude != null) 16.0 else 12.0)
            controller.setCenter(
                GeoPoint(
                    latitude ?: -6.2088,
                    longitude ?: 106.8456,
                )
            )
            overlays.add(
                MapEventsOverlay(
                    object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                            if (latestEnabled && p != null) {
                                latestOnSelect(p.latitude, p.longitude)
                                return true
                            }
                            return false
                        }

                        override fun longPressHelper(p: GeoPoint?): Boolean {
                            return false
                        }
                    }
                )
            )
        }
    }

    val marker = remember(mapView) {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    Card(
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.profile_map_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (latitude != null && longitude != null) {
                        stringResource(
                            R.string.profile_map_coordinates_format,
                            latitude,
                            longitude,
                        )
                    } else {
                        stringResource(R.string.profile_map_no_pin)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            Text(
                text = stringResource(R.string.profile_map_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(MaterialTheme.shapes.small)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = MaterialTheme.shapes.small,
                    ),
            ) {
                AndroidView(
                    factory = { mapView },
                    update = { view ->
                        if (latitude != null && longitude != null) {
                            val point = GeoPoint(latitude, longitude)
                            marker.position = point
                            if (!view.overlays.contains(marker)) {
                                view.overlays.add(marker)
                            }
                            if (view.zoomLevelDouble < 15.0) {
                                view.controller.setZoom(16.0)
                            }
                            view.controller.animateTo(point)
                        } else {
                            view.overlays.remove(marker)
                        }
                        view.invalidate()
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (isGeocoding) {
                Text(
                    text = stringResource(R.string.profile_map_geocoding),
                    style = MaterialTheme.typography.labelSmall,
                    color = KinetixPalette.WarningAmber,
                )
            } else if (statusNotice != null) {
                val noticeText = when (statusNotice) {
                    ProfileMapStatusNotice.PIN_UPDATED_FROM_ADDRESS ->
                        stringResource(R.string.profile_map_notice_pin_updated)
                    ProfileMapStatusNotice.ADDRESS_UPDATED_FROM_PIN ->
                        stringResource(R.string.profile_map_notice_address_updated)
                    ProfileMapStatusNotice.LOCATION_UNAVAILABLE ->
                        stringResource(R.string.profile_map_notice_location_unavailable)
                    ProfileMapStatusNotice.PERMISSION_DENIED ->
                        stringResource(R.string.profile_map_notice_permission_denied)
                }
                val noticeColor = when (statusNotice) {
                    ProfileMapStatusNotice.PIN_UPDATED_FROM_ADDRESS,
                    ProfileMapStatusNotice.ADDRESS_UPDATED_FROM_PIN -> MaterialTheme.colorScheme.tertiary
                    ProfileMapStatusNotice.LOCATION_UNAVAILABLE,
                    ProfileMapStatusNotice.PERMISSION_DENIED -> KinetixPalette.WarningAmber
                }
                Text(
                    text = noticeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = noticeColor,
                )
            }

            OutlinedButton(
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                onClick = onRequestCurrentLocation,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(18.dp),
                )
                Text(
                    text = stringResource(R.string.profile_map_use_current_location),
                )
            }
        }
    }
}
