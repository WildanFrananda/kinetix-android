package com.kinetix.shop.core.serialization

import java.math.BigDecimal
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "com.kinetix.shop.core.serialization.BigDecimal",
        kind = PrimitiveKind.STRING,
    )

    override fun deserialize(
        decoder: Decoder,
    ): BigDecimal {
        val raw = if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            val primitive = element as? JsonPrimitive
                ?: throw SerializationException("Expected JSON primitive for BigDecimal")
            primitive.content
        } else {
            decoder.decodeString()
        }

        return try {
            BigDecimal(raw.trim())
        } catch (exception: NumberFormatException) {
            throw SerializationException("Invalid BigDecimal value '$raw'", exception)
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: BigDecimal,
    ) {
        encoder.encodeString(value.toPlainString())
    }
}
