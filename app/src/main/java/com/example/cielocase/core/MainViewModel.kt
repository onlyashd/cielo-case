package com.example.cielocase.core

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import com.example.cielocase.database.AppDatabase
import com.example.cielocase.util.observability.SentryConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {
    private val _db = MutableLiveData<AppDatabase>()
    val db: LiveData<AppDatabase> = _db

    private val _sentry = MutableLiveData<SentryConfig>()
    val sentry: LiveData<SentryConfig> = _sentry

    private val _navController = MutableLiveData<NavHostController>()
    val navController: LiveData<NavHostController> = _navController

    fun init(
        database: AppDatabase,
        sentryConfig: SentryConfig
    ) {
        _db.postValue(database)
        _sentry.postValue(sentryConfig)
    }

    fun saveNavController(navController: NavHostController) {
        _navController.postValue(navController)
    }
}
