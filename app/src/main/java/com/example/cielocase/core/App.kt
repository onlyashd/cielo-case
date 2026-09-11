package com.example.cielocase.core

import android.app.Application
import androidx.lifecycle.lifecycleScope
import com.example.cielocase.database.AppDatabase
import com.example.cielocase.util.extensions.getActivity
import com.example.cielocase.util.observability.SentryConfig
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Provider

@HiltAndroidApp
class App: Application() {
    @Inject
    lateinit var database: Provider<AppDatabase>

    @Inject
    lateinit var sentryConfig: Provider<SentryConfig>

    @Inject
    lateinit var mainViewModel: Provider<MainViewModel>

    override fun onCreate() {
        super.onCreate()

        getActivity()?.lifecycleScope?.launch {
            withContext(Dispatchers.IO) {
                mainViewModel.get().init(
                    database.get(),
                    sentryConfig.get(),
                )
            }
        }
    }
}
