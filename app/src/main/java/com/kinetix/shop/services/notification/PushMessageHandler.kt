package com.kinetix.shop.services.notification

import com.kinetix.shop.models.notification.DeviceVerificationChallenge
import kotlinx.coroutines.flow.StateFlow

interface PushMessageHandler {
    val latestVerificationChallenge: StateFlow<DeviceVerificationChallenge?>

    fun onNewDeviceToken(
        token: String,
    )

    fun onDataMessageReceived(
        data: Map<String, String>,
    )

    fun clearVerificationChallenge()
}
