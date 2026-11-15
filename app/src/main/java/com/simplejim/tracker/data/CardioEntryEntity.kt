package com.simplejim.tracker.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "cardio_entries")
internal data class CardioEntryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "recorded_at") val recordedAt: Long,
    val kind: String,
    @ColumnInfo(name = "duration_seconds") val durationSeconds: Long,
    @ColumnInfo(name = "distance_thousandths") val distanceThousandths: Long? = null,
    @ColumnInfo(name = "distance_unit") val distanceUnit: String? = null,
    @ColumnInfo(name = "incline_thousandths") val inclineThousandths: Long? = null,
    @ColumnInfo(name = "floor_thousandths") val floorThousandths: Long? = null,
    @ColumnInfo(name = "level_thousandths") val levelThousandths: Long? = null,
)

@Dao
internal interface CardioDao {
    @Query("SELECT * FROM cardio_entries ORDER BY recorded_at DESC, id DESC")
    suspend fun entries(): List<CardioEntryEntity>

    @Query("SELECT * FROM cardio_entries WHERE id = :id")
    suspend fun entry(id: String): CardioEntryEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: CardioEntryEntity): Long
}
