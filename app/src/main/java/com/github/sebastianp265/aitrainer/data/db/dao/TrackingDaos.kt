package com.github.sebastianp265.aitrainer.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.github.sebastianp265.aitrainer.data.db.entity.BodyWeightEntity
import com.github.sebastianp265.aitrainer.data.db.entity.DailyEnergyEntity
import com.github.sebastianp265.aitrainer.data.db.entity.PlanEntity
import com.github.sebastianp265.aitrainer.data.db.entity.ProfilePrefsEntity
import com.github.sebastianp265.aitrainer.domain.model.WeightSource
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

@Dao
interface DailyEnergyDao {
    @Upsert
    suspend fun upsert(entry: DailyEnergyEntity)

    @Query("SELECT * FROM daily_energy WHERE date = :date")
    suspend fun get(date: LocalDate): DailyEnergyEntity?

    @Query("SELECT * FROM daily_energy WHERE date BETWEEN :from AND :to ORDER BY date")
    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<DailyEnergyEntity>>
}

@Dao
interface BodyWeightDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoringDuplicates(entry: BodyWeightEntity): Long

    @Query("SELECT * FROM body_weight WHERE measuredAt >= :from ORDER BY measuredAt")
    fun observeSince(from: Instant): Flow<List<BodyWeightEntity>>

    @Query("SELECT * FROM body_weight WHERE source = :source ORDER BY measuredAt DESC LIMIT 1")
    suspend fun latestFromSource(source: WeightSource): BodyWeightEntity?

    @Query("DELETE FROM body_weight WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface ProfilePrefsDao {
    @Query("SELECT * FROM profile_prefs WHERE id = ${ProfilePrefsEntity.SINGLETON_ID}")
    fun observe(): Flow<ProfilePrefsEntity?>

    @Upsert
    suspend fun upsert(prefs: ProfilePrefsEntity)
}

@Dao
interface PlanDao {
    @Query("SELECT * FROM plan WHERE isActive = 1 LIMIT 1")
    fun observeActive(): Flow<PlanEntity?>

    @Query("SELECT COALESCE(MAX(version), 0) FROM plan")
    suspend fun latestVersion(): Int

    @Query("UPDATE plan SET isActive = 0 WHERE isActive = 1")
    suspend fun deactivateAll()

    @Insert
    suspend fun insert(plan: PlanEntity): Long

    @Transaction
    suspend fun activate(plan: PlanEntity): Long {
        deactivateAll()
        return insert(plan.copy(isActive = true))
    }
}
