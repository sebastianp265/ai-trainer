package com.github.sebastianp265.aitrainer.shared.health

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

@Dao
internal interface HealthDao {
    @Query("SELECT * FROM health_day WHERE date BETWEEN :from AND :toInclusive ORDER BY date DESC")
    fun observeDays(from: LocalDate, toInclusive: LocalDate): Flow<List<HealthDayEntity>>

    @Query("SELECT * FROM health_day WHERE date BETWEEN :from AND :toInclusive")
    suspend fun getDays(from: LocalDate, toInclusive: LocalDate): List<HealthDayEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDays(days: List<HealthDayEntity>)

    @Query("DELETE FROM health_day WHERE date IN (:dates)")
    suspend fun deleteDays(dates: List<LocalDate>)

    @Query("SELECT * FROM health_workout ORDER BY startedAt DESC")
    fun observeWorkouts(): Flow<List<HealthWorkoutEntity>>

    @Query("DELETE FROM health_workout WHERE startedAt >= :from AND startedAt < :until")
    suspend fun deleteWorkoutsBetween(from: Instant, until: Instant)

    @Insert
    suspend fun insertWorkouts(workouts: List<HealthWorkoutEntity>)

    @Transaction
    suspend fun replaceDays(window: List<LocalDate>, days: List<HealthDayEntity>) {
        deleteDays(window)
        upsertDays(days)
    }

    @Transaction
    suspend fun replaceWorkouts(from: Instant, until: Instant, workouts: List<HealthWorkoutEntity>) {
        deleteWorkoutsBetween(from, until)
        insertWorkouts(workouts)
    }
}
