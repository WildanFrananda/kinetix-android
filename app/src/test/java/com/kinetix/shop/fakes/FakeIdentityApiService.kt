package com.kinetix.shop.fakes

import com.kinetix.shop.dto.auth.CredentialsRequestDto
import com.kinetix.shop.dto.auth.CurrentSessionResponseDto
import com.kinetix.shop.dto.auth.LoginResponseDto
import com.kinetix.shop.dto.auth.LogoutRequestDto
import com.kinetix.shop.dto.auth.RefreshTokenRequestDto
import com.kinetix.shop.dto.auth.SessionTokensResponseDto
import com.kinetix.shop.dto.auth.TwoFactorCodePasswordRequestDto
import com.kinetix.shop.dto.auth.TwoFactorPasswordRequestDto
import com.kinetix.shop.dto.auth.TwoFactorSetupResponseDto
import com.kinetix.shop.dto.auth.TwoFactorStatusResponseDto
import com.kinetix.shop.dto.auth.VerifyTwoFactorRequestDto
import com.kinetix.shop.dto.profile.UserProfileResponseDto
import com.kinetix.shop.services.remote.IdentityApiService
import kotlinx.serialization.json.JsonObject
import retrofit2.Response

class FakeIdentityApiService : IdentityApiService {
    var registerHandler: (CredentialsRequestDto) -> Response<SessionTokensResponseDto> = {
        throw UnsupportedOperationException("registerHandler not configured")
    }
    var loginHandler: (CredentialsRequestDto) -> Response<LoginResponseDto> = {
        throw UnsupportedOperationException("loginHandler not configured")
    }
    var refreshHandler: (RefreshTokenRequestDto) -> Response<SessionTokensResponseDto> = {
        throw UnsupportedOperationException("refreshHandler not configured")
    }
    var verifyTwoFactorHandler: (VerifyTwoFactorRequestDto) -> Response<SessionTokensResponseDto> = {
        throw UnsupportedOperationException("verifyTwoFactorHandler not configured")
    }
    var meHandler: (String) -> Response<CurrentSessionResponseDto> = {
        throw UnsupportedOperationException("meHandler not configured")
    }
    var logoutHandler: (String, LogoutRequestDto) -> Response<Unit> = { _, _ ->
        Response.success(Unit)
    }
    var setupTwoFactorHandler: (Long, String, TwoFactorPasswordRequestDto) -> Response<TwoFactorSetupResponseDto> = { _, _, _ ->
        throw UnsupportedOperationException("setupTwoFactorHandler not configured")
    }
    var enableTwoFactorHandler: (Long, String, TwoFactorCodePasswordRequestDto) -> Response<TwoFactorStatusResponseDto> = { _, _, _ ->
        throw UnsupportedOperationException("enableTwoFactorHandler not configured")
    }
    var disableTwoFactorHandler: (Long, String, TwoFactorCodePasswordRequestDto) -> Response<TwoFactorStatusResponseDto> = { _, _, _ ->
        throw UnsupportedOperationException("disableTwoFactorHandler not configured")
    }
    var getProfileHandler: (Long, String) -> Response<UserProfileResponseDto> = { _, _ ->
        throw UnsupportedOperationException("getProfileHandler not configured")
    }
    var updateProfileHandler: (Long, String, JsonObject) -> Response<UserProfileResponseDto> = { _, _, _ ->
        throw UnsupportedOperationException("updateProfileHandler not configured")
    }

    override suspend fun register(
        request: CredentialsRequestDto,
    ): Response<SessionTokensResponseDto> {
        return registerHandler(request)
    }

    override suspend fun login(
        request: CredentialsRequestDto,
    ): Response<LoginResponseDto> {
        return loginHandler(request)
    }

    override suspend fun refresh(
        request: RefreshTokenRequestDto,
    ): Response<SessionTokensResponseDto> {
        return refreshHandler(request)
    }

    override suspend fun verifyTwoFactor(
        request: VerifyTwoFactorRequestDto,
    ): Response<SessionTokensResponseDto> {
        return verifyTwoFactorHandler(request)
    }

    override suspend fun me(
        bearerToken: String,
    ): Response<CurrentSessionResponseDto> {
        return meHandler(bearerToken)
    }

    override suspend fun logout(
        bearerToken: String,
        request: LogoutRequestDto,
    ): Response<Unit> {
        return logoutHandler(bearerToken, request)
    }

    override suspend fun setupTwoFactor(
        accountId: Long,
        bearerToken: String,
        request: TwoFactorPasswordRequestDto,
    ): Response<TwoFactorSetupResponseDto> {
        return setupTwoFactorHandler(accountId, bearerToken, request)
    }

    override suspend fun enableTwoFactor(
        accountId: Long,
        bearerToken: String,
        request: TwoFactorCodePasswordRequestDto,
    ): Response<TwoFactorStatusResponseDto> {
        return enableTwoFactorHandler(accountId, bearerToken, request)
    }

    override suspend fun disableTwoFactor(
        accountId: Long,
        bearerToken: String,
        request: TwoFactorCodePasswordRequestDto,
    ): Response<TwoFactorStatusResponseDto> {
        return disableTwoFactorHandler(accountId, bearerToken, request)
    }

    override suspend fun getProfile(
        accountId: Long,
        bearerToken: String,
    ): Response<UserProfileResponseDto> {
        return getProfileHandler(accountId, bearerToken)
    }

    override suspend fun updateProfile(
        accountId: Long,
        bearerToken: String,
        body: JsonObject,
    ): Response<UserProfileResponseDto> {
        return updateProfileHandler(accountId, bearerToken, body)
    }
}
