package com.github.sebastianp265.aitrainer.feature.ai.impl

import com.github.sebastianp265.aitrainer.integration.coach.WeeklyReviewSummary

internal fun WeeklyReviewSummary.asPrompt(): String = buildString {
    appendLine("WEEKLY REVIEW of $weekStart to $weekEnd. Write a short review of the week for the user, then, if it makes sense, propose next week's plans with propose_plans, in the order to do them. Mention what you checked.")
    appendLine("Numbers already shown to the user as a card:")
    appendLine("- gym workouts: $workouts, records: $records")
    weightFromKg?.let { appendLine("- weight: $it" + (weightToKg?.let { to -> " -> $to" } ?: "") + " kg") }
    averageBalanceKcal?.let { appendLine("- average calorie balance per day: $it kcal") }
    averageProteinG?.let { appendLine("- average protein: $it g") }
    averageSleepMinutes?.let { appendLine("- average sleep: $it min" + (sleepChangeMinutes?.let { c -> " (change vs week before: $c min)" } ?: "")) }
}.trimEnd()
