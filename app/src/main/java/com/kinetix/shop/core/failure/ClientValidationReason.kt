package com.kinetix.shop.core.failure

enum class ClientValidationReason {
    CREDENTIALS_REQUIRED,
    PASSWORD_TOO_SHORT,
    INVALID_TOTP_CODE,
    PASSWORD_REQUIRED,
    PROFILE_FIELDS_REQUIRED,
}
