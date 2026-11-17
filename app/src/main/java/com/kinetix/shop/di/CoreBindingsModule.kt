package com.kinetix.shop.di

import com.kinetix.shop.core.time.Clock
import com.kinetix.shop.core.time.SystemClock
import com.kinetix.shop.repositories.auth.AuthRepository
import com.kinetix.shop.repositories.auth.AuthRepositoryImpl
import com.kinetix.shop.repositories.auth.SessionRepository
import com.kinetix.shop.repositories.auth.SessionRepositoryImpl
import com.kinetix.shop.repositories.auth.TwoFactorRepository
import com.kinetix.shop.repositories.auth.TwoFactorRepositoryImpl
import com.kinetix.shop.repositories.catalog.CatalogRepository
import com.kinetix.shop.repositories.catalog.CatalogRepositoryImpl
import com.kinetix.shop.repositories.pricing.PricingRepository
import com.kinetix.shop.repositories.pricing.PricingRepositoryImpl
import com.kinetix.shop.repositories.profile.ProfileRepository
import com.kinetix.shop.repositories.profile.ProfileRepositoryImpl
import com.kinetix.shop.repositories.review.ReviewRepository
import com.kinetix.shop.repositories.review.ReviewRepositoryImpl
import com.kinetix.shop.repositories.search.SearchRepository
import com.kinetix.shop.repositories.search.SearchRepositoryImpl
import com.kinetix.shop.services.session.RetrofitTokenRefreshTransport
import com.kinetix.shop.services.session.SessionRenewer
import com.kinetix.shop.services.session.SingleFlightSessionRenewer
import com.kinetix.shop.services.session.TokenRefreshTransport
import com.kinetix.shop.services.storage.KeyStoreSessionCipher
import com.kinetix.shop.services.storage.SessionCipher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CoreBindingsModule {
    @Binds
    @Singleton
    abstract fun bindClock(
        impl: SystemClock,
    ): Clock

    @Binds
    @Singleton
    abstract fun bindSessionCipher(
        impl: KeyStoreSessionCipher,
    ): SessionCipher

    @Binds
    @Singleton
    abstract fun bindTokenRefreshTransport(
        impl: RetrofitTokenRefreshTransport,
    ): TokenRefreshTransport

    @Binds
    @Singleton
    abstract fun bindSessionRenewer(
        impl: SingleFlightSessionRenewer,
    ): SessionRenewer

    @Binds
    @Singleton
    abstract fun bindSessionRepository(
        impl: SessionRepositoryImpl,
    ): SessionRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl,
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindTwoFactorRepository(
        impl: TwoFactorRepositoryImpl,
    ): TwoFactorRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        impl: ProfileRepositoryImpl,
    ): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindCatalogRepository(
        impl: CatalogRepositoryImpl,
    ): CatalogRepository

    @Binds
    @Singleton
    abstract fun bindPricingRepository(
        impl: PricingRepositoryImpl,
    ): PricingRepository

    @Binds
    @Singleton
    abstract fun bindSearchRepository(
        impl: SearchRepositoryImpl,
    ): SearchRepository

    @Binds
    @Singleton
    abstract fun bindReviewRepository(
        impl: ReviewRepositoryImpl,
    ): ReviewRepository
}
