package com.kinetix.shop.models.ids

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class OrderNumber(val value: String)
