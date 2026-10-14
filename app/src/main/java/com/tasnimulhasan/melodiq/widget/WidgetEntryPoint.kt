package com.tasnimulhasan.melodiq.widget

import android.content.Context
import com.tasnimulhasan.domain.localusecase.library.LibraryUseCases
import com.tasnimulhasan.domain.localusecase.player.PlayerUseCases
import com.tasnimulhasan.domain.repository.local.FavouriteRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Glance widgets, their receivers and ActionCallbacks are instantiated by the framework, not
 * by Hilt, so they can't use @Inject. This entry point hands them the same singletons the
 * rest of the app uses - which is what keeps the widget in sync with the mini/full player.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface WidgetEntryPoint {
    fun playerUseCases(): PlayerUseCases
    fun favouriteRepository(): FavouriteRepository
    fun libraryUseCases(): LibraryUseCases
    fun widgetUpdater(): WidgetUpdater
}

internal fun Context.widgetEntryPoint(): WidgetEntryPoint =
    EntryPointAccessors.fromApplication(applicationContext, WidgetEntryPoint::class.java)
