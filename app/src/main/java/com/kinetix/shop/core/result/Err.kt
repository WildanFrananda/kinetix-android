package com.kinetix.shop.core.result

import com.kinetix.shop.core.failure.Failure

data class Err(val failure: Failure) : Result<Nothing>
