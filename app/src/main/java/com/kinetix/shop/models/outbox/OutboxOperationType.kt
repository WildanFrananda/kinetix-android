package com.kinetix.shop.models.outbox

enum class OutboxOperationType {
    PROFILE_UPDATE,
    PRODUCT_REVIEW_SUBMIT,
    DRIVER_REVIEW_SUBMIT,
    CART_ITEM_UPSERT,
    CART_ITEM_REMOVE,
    CART_VOUCHER_APPLY,
}
