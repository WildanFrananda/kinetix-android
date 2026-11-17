package com.kinetix.shop.core.config

object NetworkPolicy {
    const val CONNECT_TIMEOUT_SECONDS = 10L
    const val READ_TIMEOUT_SECONDS = 15L
    const val WRITE_TIMEOUT_SECONDS = 15L
    const val CALL_TIMEOUT_SECONDS = 20L
    const val DEFAULT_PAGE_SIZE = 20
    const val MAX_PAGE_SIZE = 100
    const val DEFAULT_SUGGESTION_LIMIT = 5
    const val SEARCH_DEBOUNCE_MILLIS = 200L
    const val MIN_PASSWORD_LENGTH = 12
    const val TOTP_CODE_LENGTH = 6
    const val ONE_SECOND_MILLIS = 1_000L
}
