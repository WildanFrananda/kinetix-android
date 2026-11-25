package com.kinetix.shop.models.profile

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

data class UpdateDeliveryProfileInput(
    val fullName: String? = null,
    val phoneNumber: String? = null,
    val streetAddress: String? = null,
    val city: String? = null,
    val postalCode: String? = null,
    val avatarUrl: String? = null,
    val clearAvatar: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
) {
    fun hasChanges(): Boolean {
        return fullName != null ||
            phoneNumber != null ||
            streetAddress != null ||
            city != null ||
            postalCode != null ||
            avatarUrl != null ||
            clearAvatar ||
            (latitude != null && longitude != null)
    }

    fun toJsonObject(): JsonObject {
        return buildJsonObject {
            fullName?.let { put("fullName", JsonPrimitive(it)) }
            phoneNumber?.let { put("phoneNumber", JsonPrimitive(it)) }
            streetAddress?.let { put("streetAddress", JsonPrimitive(it)) }
            city?.let { put("city", JsonPrimitive(it)) }
            postalCode?.let { put("postalCode", JsonPrimitive(it)) }
            if (clearAvatar) {
                put("avatarUrl", JsonNull)
            } else {
                avatarUrl?.let { put("avatarUrl", JsonPrimitive(it)) }
            }
            if (latitude != null && longitude != null) {
                put("latitude", JsonPrimitive(latitude))
                put("longitude", JsonPrimitive(longitude))
            }
        }
    }
}
