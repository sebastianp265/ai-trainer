package com.github.sebastianp265.aitrainer.feature.coach.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.integration.coach.WeeklyReviewSummary
import com.github.sebastianp265.aitrainer.shared.localization.formatNumber
import com.github.sebastianp265.aitrainer.shared.localization.formatWeekRange
import com.github.sebastianp265.aitrainer.shared.localization.formatWeight

@Composable
internal fun ReviewCard(review: WeeklyReviewSummary) {
    Card(Modifier.fillMaxWidth().padding(end = 24.dp).testTag(CoachTags.REVIEW)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(R.string.review_kind, formatWeekRange(review.weekStart, review.weekEnd).uppercase()),
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.3.sp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            ReviewRow(R.string.review_workouts, workoutsText(review))
            if (review.records > 0) ReviewRow(R.string.review_strength, pluralStringResource(R.plurals.review_records, review.records, review.records))
            review.weightFromKg?.let { from ->
                val to = review.weightToKg
                ReviewRow(
                    R.string.review_weight,
                    if (to != null) stringResource(R.string.review_weight_change, formatWeight(from), formatWeight(to)) else stringResource(R.string.value_kg, formatWeight(from)),
                )
            }
            review.averageBalanceKcal?.let { balance ->
                val protein = review.averageProteinG?.let { stringResource(R.string.review_protein, it) }
                ReviewRow(
                    R.string.review_calories,
                    listOfNotNull(stringResource(R.string.review_balance, formatNumber(balance.toDouble(), signed = true)), protein).joinToString(", "),
                )
            }
            review.averageSleepMinutes?.let { minutes ->
                val change = review.sleepChangeMinutes?.let { stringResource(R.string.review_sleep_change, formatNumber(it.toDouble(), signed = true)) }
                ReviewRow(
                    R.string.review_sleep,
                    listOfNotNull(stringResource(R.string.review_sleep_average, minutes / 60, minutes % 60), change).joinToString(", "),
                )
            }
        }
    }
}

@Composable
private fun workoutsText(review: WeeklyReviewSummary): String {
    val base = review.plannedWorkouts?.let { pluralStringResource(R.plurals.review_workouts_of_planned, it, review.workouts, it) } ?: review.workouts.toString()
    return base
}

@Composable
private fun ReviewRow(label: Int, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(label), Modifier.width(76.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
