package com.example.cielocase.di

import com.example.cielocase.data.local.AppDatabase
import com.example.cielocase.data.local.dao.EventDao
import com.example.cielocase.data.payment.PreferencesInFlightPaymentStore
import com.example.cielocase.data.repository.RoomEventRepository
import com.example.cielocase.data.repository.RoomPurchaseRepository
import com.example.cielocase.domain.EventRepository
import com.example.cielocase.domain.PurchaseRepository
import com.example.cielocase.domain.payment.InFlightPaymentStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindEventRepository(impl: RoomEventRepository): EventRepository

    @Binds
    abstract fun bindPurchaseRepository(impl: RoomPurchaseRepository): PurchaseRepository

    @Binds
    abstract fun bindInFlightPaymentStore(
        impl: PreferencesInFlightPaymentStore,
    ): InFlightPaymentStore

    companion object {
        @Provides
        @Singleton
        fun provideEventDao(database: AppDatabase): EventDao = database.eventDao()
    }
}
