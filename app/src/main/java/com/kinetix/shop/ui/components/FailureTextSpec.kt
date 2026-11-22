package com.kinetix.shop.ui.components

data class FailureTextSpec(
    val resId: Int,
    val quantity: Int? = null,
    val formatArgs: List<Any> = emptyList(),
)
