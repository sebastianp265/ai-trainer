package com.github.sebastianp265.aitrainer.feature.workout.impl

internal data class SpokenSet(val weightKg: Double, val reps: Int, val rpe: Double?)

private val NUMBER = Regex("""(\d+(?:\.\d+)?)\s*(kg|kilo\w*)?""")
private val RPE = Regex("""rpe\s*(\d+(?:\.\d+)?)""")
private const val MAX_REPS = 100
private const val MAX_WEIGHT_KG = 500.0
private val RPE_RANGE = 6.0..10.0

internal fun parseSpokenSet(text: String): SpokenSet? {
    val normalized = text.lowercase().replace(',', '.')
    val rpeMatch = RPE.find(normalized)
    val rpe = rpeMatch?.groupValues?.get(1)?.toDouble()
    val rest = rpeMatch?.let { normalized.removeRange(it.range) } ?: normalized
    val numbers = NUMBER.findAll(rest).map { it.groupValues[1].toDouble() to it.groupValues[2].isNotEmpty() }.toList()
    if (numbers.size != 2) return null
    val weightIndex = numbers.indexOfFirst { it.second }.takeIf { it >= 0 } ?: 0
    val weight = numbers[weightIndex].first
    val reps = numbers[1 - weightIndex].first
    val valid = reps % 1.0 == 0.0 && reps.toInt() in 1..MAX_REPS && weight in 0.0..MAX_WEIGHT_KG &&
        (rpe == null || rpe in RPE_RANGE && (rpe * 2) % 1.0 == 0.0)
    return if (valid) SpokenSet(weight, reps.toInt(), rpe) else null
}
