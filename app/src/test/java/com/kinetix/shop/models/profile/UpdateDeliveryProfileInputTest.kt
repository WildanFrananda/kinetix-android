package com.kinetix.shop.models.profile

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateDeliveryProfileInputTest {
    @Test
    fun omitsUnsetFieldsAndSerializesAvatarNullOnlyWhenClearingAvatar() {
        val empty = UpdateDeliveryProfileInput()
        assertFalse(empty.hasChanges())
        assertTrue(empty.toJsonObject().isEmpty())

        val partial = UpdateDeliveryProfileInput(
            city = "Bandung",
            clearAvatar = true,
            latitude = -6.9175,
            longitude = 107.6191,
        )
        assertTrue(partial.hasChanges())
        val json = partial.toJsonObject()
        assertEquals(JsonPrimitive("Bandung"), json["city"])
        assertEquals(JsonNull, json["avatarUrl"])
        assertEquals(JsonPrimitive(-6.9175), json["latitude"])
        assertEquals(JsonPrimitive(107.6191), json["longitude"])
        assertFalse(json.containsKey("fullName"))
        assertFalse(json.containsKey("phoneNumber"))
    }
}
