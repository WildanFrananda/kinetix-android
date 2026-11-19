package com.kinetix.shop.core.network

import com.kinetix.shop.core.failure.NetworkFailure
import com.kinetix.shop.core.failure.TimeoutFailure
import com.kinetix.shop.core.failure.UnreadableAnswerFailure
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Ok
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class SafeApiCallerTest {
    private val caller = SafeApiCaller(
        HttpFailureMapper(
            ApiErrorParser(
                Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                }
            )
        )
    )

    @Test
    fun mapsSocketTimeoutSerializationAndIoExceptions() = runTest {
        val timeoutResult = caller.execute<String, String>(
            call = { throw SocketTimeoutException("read timed out") },
            transform = { Ok(it) },
        )
        assertEquals(Err(TimeoutFailure), timeoutResult)

        val serializationResult = caller.execute<String, String>(
            call = { throw SerializationException("bad json") },
            transform = { Ok(it) },
        )
        assertTrue((serializationResult as Err).failure is UnreadableAnswerFailure)

        val networkResult = caller.execute<String, String>(
            call = { throw IOException("no route to host") },
            transform = { Ok(it) },
        )
        assertTrue((networkResult as Err).failure is NetworkFailure)
    }

    @Test
    fun executeNullableInvokesOnNullSuccessFor200WithNullBody() = runTest {
        val result = caller.executeNullable<String, String?>(
            call = { Response.success(null) },
            onNullSuccess = { Ok(null) },
            transform = { Ok(it) },
        )
        assertEquals(Ok(null), result)
    }
}
