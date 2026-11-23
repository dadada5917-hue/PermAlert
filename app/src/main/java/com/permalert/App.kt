package com.permalert

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.permalert.data.AppDb
import com.permalert.service.ensureAlarmChannel

class App : Application() {
    lateinit var db: AppDb

    override fun onCreate() {
        super.onCreate()
        ensureAlarmChannel(this)
        db = Room.databaseBuilder(this, AppDb::class.java, "alerts.db")
            // Удаляем старые таблицы сетевых источников. Локальная история тревог сохраняется.
            .addMigrations(MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()
    }

    companion object {
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS MessageEntity")
                database.execSQL("DROP TABLE IF EXISTS ChannelEntity")
            }
        }
    }
}
