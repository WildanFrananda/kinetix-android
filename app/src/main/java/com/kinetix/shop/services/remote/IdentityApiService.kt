package com.kinetix.shop.services.remote

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
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface IdentityApiService {
    @POST("api/v1/auth/register")
    suspend fun register(
        @Body request: CredentialsRequestDto,
    ): Response<SessionTokensResponseDto>

    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: CredentialsRequestDto,
    ): Response<LoginResponseDto>

    @POST("api/v1/auth/refresh")
    suspend fun refresh(
        @Body request: RefreshTokenRequestDto,
    ): Response<SessionTokensResponseDto>

    @POST("api/v1/auth/2fa/verify")
    suspend fun verifyTwoFactor(
        @Body request: VerifyTwoFactorRequestDto,
    ): Response<SessionTokensResponseDto>

    @GET("api/v1/auth/me")
    suspend fun me(
        @Header("Authorization") bearerToken: String,
    ): Response<CurrentSessionResponseDto>

    @POST("api/v1/auth/logout")
    suspend fun logout(
        @Header("Authorization") bearerToken: String,
        @Body request: LogoutRequestDto,
    ): Response<Unit>

    @POST("api/v1/auth/2fa/setup/{id}")
    suspend fun setupTwoFactor(
        @Path("id") accountId: Long,
        @Header("Authorization") bearerToken: String,
        @Body request: TwoFactorPasswordRequestDto,
    ): Response<TwoFactorSetupResponseDto>

    @POST("api/v1/auth/2fa/enable/{id}")
    suspend fun enableTwoFactor(
        @Path("id") accountId: Long,
        @Header("Authorization") bearerToken: String,
        @Body request: TwoFactorCodePasswordRequestDto,
    ): Response<TwoFactorStatusResponseDto>

    @POST("api/v1/auth/2fa/disable/{id}")
    suspend fun disableTwoFactor(
        @Path("id") accountId: Long,
        @Header("Authorization") bearerToken: String,
        @Body request: TwoFactorCodePasswordRequestDto,
    ): Response<TwoFactorStatusResponseDto>

    @GET("api/v1/users/{id}/profile")
    suspend fun getProfile(
        @Path("id") accountId: Long,
        @Header("Authorization") bearerToken: String,
    ): Response<UserProfileResponseDto>

    @PUT("api/v1/users/{id}/profile")
    suspend fun updateProfile(
        @Path("id") accountId: Long,
        @Header("Authorization") bearerToken: String,
        @Body body: JsonObject,
    ): Response<UserProfileResponseDto>
}
