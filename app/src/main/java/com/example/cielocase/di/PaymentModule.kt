package com.example.cielocase.di

import com.example.cielocase.BuildConfig
import com.example.cielocase.data.payment.cielo.CieloCredentials
import com.example.cielocase.data.payment.cielo.CieloDeeplinkGateway
import com.example.cielocase.domain.payment.PaymentGateway
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PaymentModule {

    @Binds
    abstract fun bindPaymentGateway(impl: CieloDeeplinkGateway): PaymentGateway

    companion object {
        @Provides
        @Singleton
        fun provideCieloCredentials(): CieloCredentials = CieloCredentials(
            clientId = BuildConfig.CIELO_CLIENT_ID,
            accessToken = BuildConfig.CIELO_ACCESS_TOKEN,
            merchantCode = BuildConfig.CIELO_MERCHANT_CODE,
        )
    }
}
