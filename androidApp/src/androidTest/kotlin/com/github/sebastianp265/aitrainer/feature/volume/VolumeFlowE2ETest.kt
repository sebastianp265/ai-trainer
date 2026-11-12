package com.github.sebastianp265.aitrainer.feature.volume

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sebastianp265.aitrainer.feature.progress.ProgressScreen
import com.github.sebastianp265.aitrainer.shared.e2e.AbstractE2ETest
import com.github.sebastianp265.aitrainer.shared.e2e.BENCH_PRESS
import com.github.sebastianp265.aitrainer.shared.e2e.sets
import org.junit.runner.RunWith
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class VolumeFlowE2ETest : AbstractE2ETest() {

    private val progress = ProgressScreen(compose)
    private val targets = VolumeTargetsScreen(compose)

    @Test
    fun `a target typed by hand replaces the default in progress until the defaults are restored`() {
        openProgressAfterABenchWorkout()
        progress.assertVolumeTarget("target 10–20")

        progress.openTargets()
        targets.assertShown()
        targets.setRange("CHEST", from = "6", to = "9")
        targets.assertSource("You ·")
        targets.back()
        progress.assertVolumeTarget("target 6–9")
        progress.openTargets()
        targets.restoreDefaults()
        targets.back()

        progress.assertVolumeTarget("target 10–20")
    }

    @Test
    fun `switching targets off leaves only the numbers`() {
        openProgressAfterABenchWorkout()

        progress.openTargets()
        targets.switchShowTargets()
        targets.back()

        progress.assertVolumeCardShown()
        progress.assertNoVolumeTargets()
    }


    private fun openProgressAfterABenchWorkout() {
        given.finishedWorkout(sets(BENCH_PRESS))
        launchApp()
        progress.open()
    }
}
