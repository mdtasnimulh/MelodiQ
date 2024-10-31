package com.tasnimulhasan.database.di

import android.content.Context
import androidx.room.Room
import com.tasnimulhasan.database.MelodiQDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun providesMmDatabase(
        @ApplicationContext context: Context
    ): MelodiQDatabase = Room.databaseBuilder(
        context,
        MelodiQDatabase::class.java,
        "mm_database"
    ).build()
}