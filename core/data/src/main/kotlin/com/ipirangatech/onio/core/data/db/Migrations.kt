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

/** v2 -> v3: coupons table, plus discount info on orders (existing orders keep working with zero discount). */
internal val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `coupons` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`code` TEXT NOT NULL, " +
                "`kind` TEXT NOT NULL, " +
                "`value` INTEGER NOT NULL, " +
                "`minSubtotalCents` INTEGER NOT NULL, " +
                "`expiresAtMillis` INTEGER NOT NULL, " +
                "`usedAtMillis` INTEGER, " +
                "`grantedAtMillis` INTEGER NOT NULL)",
        )
        db.execSQL("ALTER TABLE `orders` ADD COLUMN `discountUsdCents` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `orders` ADD COLUMN `couponCode` TEXT")
        db.execSQL("ALTER TABLE `orders` ADD COLUMN `coinsUsed` INTEGER NOT NULL DEFAULT 0")
    }
}
