package com.kinetix.shop.models.ids

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class IdempotencyKey(val value: String)
