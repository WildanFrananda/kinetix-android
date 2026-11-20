package com.kinetix.shop.core.network

import com.kinetix.shop.core.failure.ChallengeSpentFailure
import com.kinetix.shop.core.failure.ConflictFailure
import com.kinetix.shop.core.failure.Failure
import com.kinetix.shop.core.failure.ForbiddenFailure
import com.kinetix.shop.core.failure.NetworkFailure
import com.kinetix.shop.core.failure.NotFoundFailure
import com.kinetix.shop.core.failure.RateLimitedFailure
import com.kinetix.shop.core.failure.SignInDelayedFailure
import com.kinetix.shop.core.failure.TimeoutFailure
import com.kinetix.shop.core.failure.UnauthorizedFailure
import com.kinetix.shop.core.failure.UnavailableFailure
import com.kinetix.shop.core.failure.UnknownOutcomeFailure
import com.kinetix.shop.core.failure.UnreadableAnswerFailure
import com.kinetix.shop.core.failure.ValidationFailure
import com.kinetix.shop.core.failure.WrongPasswordFailure
import com.kinetix.shop.core.failure.WrongTwoFactorCodeFailure
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
    private val correlationRecorder: RequestCorrelationRecorder,
) {
    constructor(
        failureMapper: HttpFailureMapper,
    ) : this(
        failureMapper = failureMapper,
        correlationRecorder = object : RequestCorrelationRecorder {
            override fun recordRequest(
                method: String,
                routeTemplate: String,
                requestId: String,
            ) {}
        },
    )

    suspend fun <Dto, Domain> execute(
        call: suspend () -> Response<Dto>,
        onErrorOverride: ((Int, String?, Int?) -> Failure?)? = null,
        transform: suspend (Dto) -> Result<Domain>,
    ): Result<Domain> {
        return invokeAndMap(
            call = call,
            onNullBody = { response ->
                val requestId = extractHeaderRequestId(response)
                Err(
                    UnreadableAnswerFailure(
                        detail = "Empty response body on HTTP ${response.code()}",
                        requestId = requestId,
                    )
                )
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
            return Err(
                UnreadableAnswerFailure(
                    detail = exception.message ?: "Unreadable response payload",
                    requestId = correlationRecorder.latestRequestId(),
                )
            )
        } catch (exception: IOException) {
            return Err(
                NetworkFailure(
                    detail = exception.message ?: "Network error",
                    requestId = correlationRecorder.latestRequestId(),
                )
            )
        } catch (exception: Exception) {
            return Err(
                UnreadableAnswerFailure(
                    detail = exception.message ?: "Unexpected transport error",
                    requestId = correlationRecorder.latestRequestId(),
                )
            )
        }

        val headerRequestId = extractHeaderRequestId(response)
        if (response.isSuccessful) {
            val body = response.body() ?: return onNullBody(response)
            return try {
                transform(body)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Err(
                    UnreadableAnswerFailure(
                        detail = exception.message ?: "Failed to map response",
                        requestId = headerRequestId,
                    )
                )
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
            return Err(attachRequestId(customFailure, headerRequestId))
        }

        return Err(
            failureMapper.map(
                statusCode = statusCode,
                rawErrorBody = rawErrorBody,
                retryAfterHeaderSeconds = retryAfterSeconds,
                requestId = headerRequestId,
            )
        )
    }

    private fun <Dto> extractHeaderRequestId(
        response: Response<Dto>,
    ): String? {
        return response.headers()[RequestCorrelationInterceptor.HEADER_REQUEST_ID]?.takeIf {
            it.isNotBlank()
        } ?: response.raw().request.header(RequestCorrelationInterceptor.HEADER_REQUEST_ID)?.takeIf {
            it.isNotBlank()
        }
    }

    private fun attachRequestId(
        failure: Failure,
        requestId: String?,
    ): Failure {
        if (requestId == null || failure.requestId != null) {
            return failure
        }

        return when (failure) {
            is ChallengeSpentFailure -> failure.copy(requestId = requestId)
            is ConflictFailure -> failure.copy(requestId = requestId)
            is ForbiddenFailure -> failure.copy(requestId = requestId)
            is NetworkFailure -> failure.copy(requestId = requestId)
            is NotFoundFailure -> failure.copy(requestId = requestId)
            is RateLimitedFailure -> failure.copy(requestId = requestId)
            is SignInDelayedFailure -> failure.copy(requestId = requestId)
            is UnauthorizedFailure -> failure.copy(requestId = requestId)
            is UnavailableFailure -> failure.copy(requestId = requestId)
            is UnknownOutcomeFailure -> failure.copy(requestId = requestId)
            is UnreadableAnswerFailure -> failure.copy(requestId = requestId)
            is ValidationFailure -> failure.copy(requestId = requestId)
            is WrongPasswordFailure -> failure.copy(requestId = requestId)
            is WrongTwoFactorCodeFailure -> failure.copy(requestId = requestId)
            else -> failure
        }
    }
}
