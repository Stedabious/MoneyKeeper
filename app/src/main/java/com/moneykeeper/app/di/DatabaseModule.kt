package com.moneykeeper.app.di

import android.content.Context
import androidx.room.Room
import com.moneykeeper.app.data.database.AppDatabase
import com.moneykeeper.app.data.database.dao.CategoryDao
import com.moneykeeper.app.data.database.dao.NotificationLogDao
import com.moneykeeper.app.data.database.dao.PendingEventDao
import com.moneykeeper.app.data.database.dao.RegexPatternDao
import com.moneykeeper.app.data.database.dao.TransactionDao
import com.moneykeeper.app.data.database.entity.toEntity
import com.moneykeeper.app.domain.model.DefaultCategories
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        val db = Room.databaseBuilder(context, AppDatabase::class.java, "moneykeeper.db")
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9, AppDatabase.MIGRATION_9_10, AppDatabase.MIGRATION_10_11)
            .build()
        CoroutineScope(Dispatchers.IO).launch {
            if (db.categoryDao().count() == 0) {
                db.categoryDao().insertAll(
                    DefaultCategories.mapIndexed { index, category -> category.toEntity(index) }
                )
            }
        }
        return db
    }

    @Provides fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()
    @Provides fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
    @Provides fun providePendingEventDao(db: AppDatabase): PendingEventDao = db.pendingEventDao()
    @Provides fun provideNotificationLogDao(db: AppDatabase): NotificationLogDao = db.notificationLogDao()
    @Provides fun provideRegexPatternDao(db: AppDatabase): RegexPatternDao = db.regexPatternDao()
}
