package com.moneykeeper.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.moneykeeper.app.data.database.dao.CategoryDao
import com.moneykeeper.app.data.database.dao.NotificationLogDao
import com.moneykeeper.app.data.database.dao.PendingEventDao
import com.moneykeeper.app.data.database.dao.RegexPatternDao
import com.moneykeeper.app.data.database.dao.TransactionDao
import com.moneykeeper.app.data.database.entity.CategoryEntity
import com.moneykeeper.app.data.database.entity.NotificationLogEntity
import com.moneykeeper.app.data.database.entity.PendingEventEntity
import com.moneykeeper.app.data.database.entity.RegexPatternEntity
import com.moneykeeper.app.data.database.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        PendingEventEntity::class,
        NotificationLogEntity::class,
        RegexPatternEntity::class,
    ],
    version = 10,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun pendingEventDao(): PendingEventDao
    abstract fun notificationLogDao(): NotificationLogDao
    abstract fun regexPatternDao(): RegexPatternDao

    companion object {
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN categoryType TEXT NOT NULL DEFAULT 'EXPENSE'")
                db.execSQL("INSERT OR IGNORE INTO categories (id,name,icon,colorHex,isSystem,sortOrder,categoryType) VALUES (9,'薪資','payments','#4CAF50',1,8,'INCOME')")
                db.execSQL("INSERT OR IGNORE INTO categories (id,name,icon,colorHex,isSystem,sortOrder,categoryType) VALUES (10,'獎金','star','#FFC107',1,9,'INCOME')")
                db.execSQL("INSERT OR IGNORE INTO categories (id,name,icon,colorHex,isSystem,sortOrder,categoryType) VALUES (11,'股票','trending_up','#2196F3',1,10,'INCOME')")
                db.execSQL("INSERT OR IGNORE INTO categories (id,name,icon,colorHex,isSystem,sortOrder,categoryType) VALUES (12,'基金','account_balance','#9C27B0',1,11,'INCOME')")
                db.execSQL("INSERT OR IGNORE INTO categories (id,name,icon,colorHex,isSystem,sortOrder,categoryType) VALUES (13,'被動收入','autorenew','#00BCD4',1,12,'INCOME')")
                db.execSQL("INSERT OR IGNORE INTO categories (id,name,icon,colorHex,isSystem,sortOrder,categoryType) VALUES (14,'其他收入','more_horiz','#78909C',1,13,'INCOME')")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN transactionType TEXT NOT NULL DEFAULT 'EXPENSE'")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notification_log ADD COLUMN parseVersion INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notification_log ADD COLUMN lastParsedAt INTEGER")
                db.execSQL("ALTER TABLE notification_log ADD COLUMN lineSenderType TEXT")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notification_log ADD COLUMN category TEXT NOT NULL DEFAULT 'UNKNOWN'")
                db.execSQL("ALTER TABLE notification_log ADD COLUMN isFiltered INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notification_log ADD COLUMN filteredReason TEXT")
                db.execSQL("ALTER TABLE notification_log ADD COLUMN parseTrace TEXT")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS regex_patterns (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        patternString TEXT NOT NULL,
                        patternType TEXT NOT NULL,
                        sourceBody TEXT NOT NULL,
                        sourcePackageName TEXT NOT NULL,
                        sourceAppLabel TEXT NOT NULL,
                        testPassed INTEGER,
                        note TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notification_log ADD COLUMN parseStatus TEXT NOT NULL DEFAULT 'UNPARSED'")
                db.execSQL("UPDATE notification_log SET parseStatus = 'PARSED_EXPENSE' WHERE parsedAmount IS NOT NULL")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN deletedAt INTEGER")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notification_log ADD COLUMN eventSource TEXT NOT NULL DEFAULT 'REAL_NOTIFICATION'")
                db.execSQL("ALTER TABLE pending_events ADD COLUMN eventSource TEXT NOT NULL DEFAULT 'REAL_NOTIFICATION'")
                db.execSQL("ALTER TABLE transactions ADD COLUMN eventSource TEXT NOT NULL DEFAULT 'REAL_NOTIFICATION'")
                db.execSQL("UPDATE transactions SET eventSource = 'MANUAL_INPUT' WHERE source = 'MANUAL'")
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS notification_log (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        packageName TEXT NOT NULL,
                        appLabel TEXT NOT NULL,
                        title TEXT NOT NULL,
                        body TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        parsedAmount REAL,
                        parserName TEXT,
                        confidence REAL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
