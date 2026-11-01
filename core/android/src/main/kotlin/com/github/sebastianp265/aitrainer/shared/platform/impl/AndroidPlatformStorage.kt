package com.github.sebastianp265.aitrainer.shared.platform.impl

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.room.RoomDatabase
import com.github.sebastianp265.aitrainer.integration.shared.BundledFiles
import com.github.sebastianp265.aitrainer.integration.shared.DatabaseBuilders
import com.github.sebastianp265.aitrainer.integration.shared.PreferencesStores
import kotlin.reflect.KClass

internal class AndroidBundledFiles : BundledFiles {
    override fun read(path: String): String =
        AndroidBundledFiles::class.java.classLoader!!.getResourceAsStream(path)!!.use { it.readBytes().decodeToString() }
}

internal class AndroidDatabaseBuilders(private val context: Context) : DatabaseBuilders {
    override fun <T : RoomDatabase> builder(database: KClass<T>, name: String): RoomDatabase.Builder<T> =
        Room.databaseBuilder(context, database.java, name)
}

internal class AndroidPreferencesStores(private val context: Context) : PreferencesStores {
    override fun create(name: String): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile(name) }
}
