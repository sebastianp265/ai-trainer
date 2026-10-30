package com.github.sebastianp265.aitrainer.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.github.sebastianp265.aitrainer.data.db.entity.ProfilePrefsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfilePrefsDao {
    @Query("SELECT * FROM profile_prefs WHERE id = ${ProfilePrefsEntity.SINGLETON_ID}")
    fun observe(): Flow<ProfilePrefsEntity?>

    @Query("SELECT * FROM profile_prefs WHERE id = ${ProfilePrefsEntity.SINGLETON_ID}")
    suspend fun get(): ProfilePrefsEntity?

    @Upsert
    suspend fun upsert(prefs: ProfilePrefsEntity)

    @Transaction
    suspend fun modifyInTransaction(default: ProfilePrefsEntity, change: (ProfilePrefsEntity) -> ProfilePrefsEntity) {
        upsert(change(get() ?: default))
    }
}
