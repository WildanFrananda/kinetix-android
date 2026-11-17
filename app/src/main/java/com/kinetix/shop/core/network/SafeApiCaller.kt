package com.kinetix.shop.core.network

import com.kinetix.shop.core.failure.Failure
import com.kinetix.shop.core.failure.NetworkFailure
import com.kinetix.shop.core.failure.TimeoutFailure
import com.kinetix.shop.core.failure.UnreadableAnswerFailure
import com.kinetix.shop.core.result.Err
import com.kinetix.shop.core.result.Result
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.Response

@Singleton
class SafeApiCaller @Inject constructor(
    private val failureMapper: HttpFailureMapper,
) {
    suspend fun <Dto, Domain> execute(
        call: suspend () -> Response<Dto>,
        onErrorOverride: ((Int, String?, Int?) -> Failure?)? = null,
        transform: suspend (Dto) -> Result<Domain>,
    ): Result<Domain> {
        return invokeAndMap(
            call = call,
            onNullBody = { response ->
                Err(UnreadableAnswerFailure("Empty response body on HTTP ${response.code()}"))
            },
            onErrorOverride = onErrorOverride,
            transform = { body -> transform(body) },
        )
    }

    suspend fun <Dto, Domain> executeNullable(
        call: suspend () -> Response<Dto?>,
        onNullSuccess: suspend () -> Result<Domain>,
        onErrorOverride: ((Int, String?, Int?) -> Failure?)? = null,
        transform: suspend (Dto) -> Result<Domain>,
    ): Result<Domain> {
        return invokeAndMap(
            call = call,
            onNullBody = { onNullSuccess() },
            onErrorOverride = onErrorOverride,
            transform = { body ->
                if (body == null) {
                    onNullSuccess()
                } else {
                    transform(body)
                }
            },
        )
    }

    private suspend fun <Dto, Domain> invokeAndMap(
        call: suspend () -> Response<Dto>,
        onNullBody: suspend (Response<Dto>) -> Result<Domain>,
        onErrorOverride: ((Int, String?, Int?) -> Failure?)?,
        transform: suspend (Dto) -> Result<Domain>,
    ): Result<Domain> {
        val response = try {
            call()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: SocketTimeoutException) {
            return Err(TimeoutFailure)
        } catch (exception: SerializationException) {
            return Err(UnreadableAnswerFailure(exception.message ?: "Unreadable response payload"))
        } catch (exception: IOException) {
            return Err(NetworkFailure(exception.message ?: "Network error"))
        } catch (exception: Exception) {
            return Err(UnreadableAnswerFailure(exception.message ?: "Unexpected transport error"))
        }

        if (response.isSuccessful) {
            val body = response.body() ?: return onNullBody(response)
            return try {
                transform(body)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Err(UnreadableAnswerFailure(exception.message ?: "Failed to map response"))
            }
        }

        val statusCode = response.code()
        val rawErrorBody = try {
            response.errorBody()?.string()
        } catch (_: Exception) {
            null
        }
        val retryAfterSeconds = response.headers()["Retry-After"]?.toIntOrNull()

        val customFailure = onErrorOverride?.invoke(
            statusCode,
            rawErrorBody,
            retryAfterSeconds,
        )
        if (customFailure != null) {
            return Err(customFailure)
        }

        return Err(
            failureMapper.map(
                statusCode = statusCode,
                rawErrorBody = rawErrorBody,
                retryAfterHeaderSeconds = retryAfterSeconds,
            )
        )
    }
}
