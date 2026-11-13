package com.github.sebastianp265.aitrainer.feature.workout.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.integration.plan.PlanAuthor
import com.github.sebastianp265.aitrainer.integration.workout.PlanDifference
import com.github.sebastianp265.aitrainer.integration.workout.WorkoutReport
import com.github.sebastianp265.aitrainer.shared.localization.formatLongWeekdayDate
import com.github.sebastianp265.aitrainer.shared.localization.formatNumber
import kotlin.math.abs

internal fun differenceIsWorse(difference: PlanDifference): Boolean =
    difference.setCountDelta < 0 || difference.weightDeltaKg < 0 || difference.repsDelta < 0

@Composable
internal fun differenceText(difference: PlanDifference): String {
    if (difference.addedToPlan) return stringResource(R.string.diff_added)
    val parts = buildList {
        if (difference.setCountDelta != 0) {
            add(
                pluralStringResource(
                    R.plurals.diff_sets,
                    abs(difference.setCountDelta),
                    formatNumber(difference.setCountDelta.toDouble(), signed = true),
                ),
            )
        }
        if (abs(difference.weightDeltaKg) >= MIN_WEIGHT_DIFFERENCE_KG) {
            add(stringResource(R.string.diff_kg, formatNumber(difference.weightDeltaKg, 1, signed = true)))
        }
        if (difference.repsDelta != 0) {
            add(stringResource(R.string.diff_reps, formatNumber(difference.repsDelta.toDouble(), signed = true)))
        }
    }
    return if (parts.isEmpty()) stringResource(R.string.diff_on_plan) else parts.joinToString(", ")
}

private const val MIN_WEIGHT_DIFFERENCE_KG = 0.05

@Composable
internal fun reportSubtitle(report: WorkoutReport): String {
    val name = report.plan?.name ?: stringResource(R.string.workout_empty_title)
    val origin = when (report.plan?.author) {
        null -> stringResource(R.string.workout_empty_subtitle)
        PlanAuthor.USER -> stringResource(R.string.workout_from_own_plan)
        PlanAuthor.COACH -> stringResource(R.string.workout_from_coach_plan)
    }
    return stringResource(R.string.report_subtitle, name, formatLongWeekdayDate(report.session.date), origin)
}

@Composable
internal fun reportDetailSubtitle(report: WorkoutReport): String {
    val origin = when (report.plan?.author) {
        null -> stringResource(R.string.workout_empty_subtitle)
        PlanAuthor.USER -> stringResource(R.string.workout_from_own_plan)
        PlanAuthor.COACH -> stringResource(R.string.workout_from_coach_plan)
    }
    return stringResource(R.string.detail_subtitle, formatLongWeekdayDate(report.session.date), origin)
}
