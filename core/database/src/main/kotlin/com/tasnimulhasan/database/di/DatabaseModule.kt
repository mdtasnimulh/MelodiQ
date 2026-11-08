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
        "melodiq_database"
    ).addMigrations(com.tasnimulhasan.database.migration.MIGRATION_2_3, com.tasnimulhasan.database.migration.MIGRATION_3_4)
        // Only the never-shipped schema v1 may be wiped. Any other missing migration must FAIL loudly in testing
        // instead of silently deleting a user's playlists and favourites after an update.
        .fallbackToDestructiveMigrationFrom(true, 1).build()
}