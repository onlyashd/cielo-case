package com.example.cielocase.di

import javax.inject.Qualifier

/** Coroutine scope that lives as long as the process (payment callbacks outlive screens). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
