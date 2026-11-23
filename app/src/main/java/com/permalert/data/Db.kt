package com.permalert.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

/** Локальная история сработавших оповещений. */
@Entity
data class AlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ts: Long,
    val type: String,
    val source: String,
    val text: String,
    val link: String
)

@Dao
interface AlertDao {
    @Insert suspend fun insert(alert: AlertEntity)
    @Query("SELECT * FROM AlertEntity ORDER BY ts DESC LIMIT 1") suspend fun last(): AlertEntity?
    @Query("SELECT * FROM AlertEntity ORDER BY ts DESC LIMIT 200") suspend fun recent(): List<AlertEntity>
}

@Database(entities = [AlertEntity::class], version = 3, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun alerts(): AlertDao
}
