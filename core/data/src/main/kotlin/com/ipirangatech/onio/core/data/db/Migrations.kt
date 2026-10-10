package com.ipirangatech.onio.core.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v1 -> v2: adds the favourites table (orders and cart are untouched). */
internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `favorites` (" +
                "`productId` INTEGER NOT NULL, " +
                "`title` TEXT NOT NULL, " +
                "`thumbnailUrl` TEXT NOT NULL, " +
                "`priceUsdCents` INTEGER NOT NULL, " +
                "`discountPercentage` REAL NOT NULL, " +
                "`rating` REAL NOT NULL, " +
                "`addedAtMillis` INTEGER NOT NULL, " +
                "PRIMARY KEY(`productId`))",
        )
    }
}
