package com.tasnimulhasan.database.di

import android.content.Context
import androidx.room.Room
import com.tasnimulhasan.database.MmDatabase
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
    ): MmDatabase = Room.databaseBuilder(
        context,
        MmDatabase::class.java,
        "mm_database"
    ).build()
}