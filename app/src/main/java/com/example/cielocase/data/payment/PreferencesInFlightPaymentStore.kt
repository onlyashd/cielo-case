package com.example.cielocase.data.payment

import android.content.Context
import androidx.core.content.edit
import com.example.cielocase.domain.payment.InFlightPaymentStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesInFlightPaymentStore @Inject constructor(
    @ApplicationContext context: Context,
) : InFlightPaymentStore {

    private val preferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    override fun get(): String? = preferences.getString(KEY_REFERENCE, null)

    override fun set(reference: String) = preferences.edit { putString(KEY_REFERENCE, reference) }

    override fun clear() = preferences.edit { remove(KEY_REFERENCE) }

    private companion object {
        const val NAME = "payments"
        const val KEY_REFERENCE = "in_flight_reference"
    }
}
