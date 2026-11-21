package com.kinetix.shop.services.session

import com.kinetix.shop.core.concurrency.IoDispatcher
import com.kinetix.shop.core.config.AppConfigCheckResult
import com.kinetix.shop.core.config.AppConfigValid
import com.kinetix.shop.core.network.RequestCorrelationInterceptor
import com.kinetix.shop.dto.auth.RefreshTokenRequestDto
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

@Singleton
class OkHttpRefreshWireExecutor @Inject constructor(
    private val configCheckResult: AppConfigCheckResult,
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : RefreshWireExecutor {
    override suspend fun postRefresh(
        refreshToken: String,
    ): RefreshWireOutcome {
        val validConfig = configCheckResult as? AppConfigValid
            ?: return RefreshWireFailure(
                throwable = IllegalStateException("Invalid configuration"),
                leftDevice = false,
            )

        return withContext(ioDispatcher) {
            val listener = RefreshWireEventListener()
            val payload = json.encodeToString(
                RefreshTokenRequestDto.serializer(),
                RefreshTokenRequestDto(refreshToken = refreshToken),
            )
            val request = Request.Builder()
                .url("${validConfig.config.httpBase}/api/v1/auth/refresh")
                .post(payload.toRequestBody(JSON_MEDIA_TYPE))
                .build()
            val callClient = okHttpClient.newBuilder()
                .eventListener(listener)
                .retryOnConnectionFailure(false)
                .build()

            try {
                callClient.newCall(request).execute().use { response ->
                    val rawBody = response.body.string()
                    val retryAfter = response.header("Retry-After")?.toIntOrNull()
                    val requestId = response.header(RequestCorrelationInterceptor.HEADER_REQUEST_ID)
                        ?: response.request.header(RequestCorrelationInterceptor.HEADER_REQUEST_ID)

                    RefreshWireResponse(
                        statusCode = response.code,
                        rawBody = rawBody,
                        retryAfterHeaderSeconds = retryAfter,
                        requestId = requestId,
                        leftDevice = listener.didLeaveDevice(),
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: IOException) {
                RefreshWireFailure(
                    throwable = exception,
                    leftDevice = listener.didLeaveDevice(),
                )
            }
        }
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=UTF-8".toMediaType()
    }
}
