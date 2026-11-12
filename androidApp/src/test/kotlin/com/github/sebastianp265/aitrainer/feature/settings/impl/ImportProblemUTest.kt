package com.github.sebastianp265.aitrainer.feature.settings.impl

import com.github.sebastianp265.aitrainer.integration.shared.BackupContents
import com.github.sebastianp265.aitrainer.integration.shared.DATABASE_SCHEMA_VERSION
import com.github.sebastianp265.aitrainer.integration.shared.OLDEST_MIGRATED_SCHEMA_VERSION
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ImportProblemUTest {

    @Test
    fun `a file that is not a backup is invalid`() {
        assertEquals(BackupMessage.INVALID_FILE, importProblem(null))
    }

    @Test
    fun `a backup from a newer app is refused`() {
        assertEquals(BackupMessage.FROM_NEWER_APP, importProblem(backupOfSchema(DATABASE_SCHEMA_VERSION + 1)))
    }

    @Test
    fun `a backup older than the oldest migration is refused`() {
        assertEquals(BackupMessage.TOO_OLD, importProblem(backupOfSchema(OLDEST_MIGRATED_SCHEMA_VERSION - 1)))
    }

    @Test
    fun `a backup of the current or an older migrated schema can be imported`() {
        assertNull(importProblem(backupOfSchema(DATABASE_SCHEMA_VERSION)))
        assertNull(importProblem(backupOfSchema(OLDEST_MIGRATED_SCHEMA_VERSION)))
    }

    private fun backupOfSchema(version: Int) = BackupContents(version, workouts = 1, plans = 0, coachNotes = 0, coachMessages = 0)
}
