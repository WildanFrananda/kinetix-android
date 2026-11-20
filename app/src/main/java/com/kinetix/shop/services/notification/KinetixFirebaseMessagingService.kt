package com.kinetix.shop.services.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class KinetixFirebaseMessagingService : FirebaseMessagingService() {
    @Inject
    lateinit var pushMessageHandler: PushMessageHandler

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onNewToken(
        token: String,
    ) {
        pushMessageHandler.onNewDeviceToken(token)
    }

    override fun onMessageReceived(
        message: RemoteMessage,
    ) {
        super.onMessageReceived(message)
        if (message.data.isNotEmpty()) {
            pushMessageHandler.onDataMessageReceived(message.data)
        }
    }
}
