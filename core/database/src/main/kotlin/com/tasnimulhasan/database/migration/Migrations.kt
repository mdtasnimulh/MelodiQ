package com.tasnimulhasan.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v2 -> v3: adds the local library cache and play-history tables (Tranche 2 - library
 * database foundation). Both are brand new tables (CREATE only, nothing existing is
 * altered), so this is a safe, non-destructive migration - unlike the fallback path used
 * for the v1 -> v2 bump, this one does not drop the user's playlists or favourites.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `library_song_table` (
                `songId` INTEGER NOT NULL,
                `title` TEXT NOT NULL,
                `titleKey` TEXT NOT NULL,
                `artist` TEXT NOT NULL,
                `artistKey` TEXT NOT NULL,
                `album` TEXT NOT NULL,
                `albumKey` TEXT NOT NULL,
                `albumId` INTEGER NOT NULL,
                `genre` TEXT,
                `durationMs` INTEGER NOT NULL,
                `dateAdded` INTEGER NOT NULL,
                `dateModified` INTEGER NOT NULL,
                `sizeBytes` INTEGER NOT NULL,
                `mimeType` TEXT,
                `folderPath` TEXT NOT NULL,
                `contentUriString` TEXT NOT NULL,
                PRIMARY KEY(`songId`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_library_song_table_titleKey` ON `library_song_table` (`titleKey`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_library_song_table_artistKey` ON `library_song_table` (`artistKey`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_library_song_table_albumKey` ON `library_song_table` (`albumKey`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_library_song_table_genre` ON `library_song_table` (`genre`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_library_song_table_folderPath` ON `library_song_table` (`folderPath`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_library_song_table_albumId` ON `library_song_table` (`albumId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_library_song_table_dateModified` ON `library_song_table` (`dateModified`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `play_history_table` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `songId` INTEGER NOT NULL,
                `playedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_play_history_table_songId` ON `play_history_table` (`songId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_play_history_table_playedAt` ON `play_history_table` (`playedAt`)")
    }
}
