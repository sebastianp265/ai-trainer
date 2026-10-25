package com.github.sebastianp265.aitrainer.data.demo

import androidx.room.withTransaction
import com.github.sebastianp265.aitrainer.data.db.AppDatabase
import com.github.sebastianp265.aitrainer.data.db.entity.BodyWeightEntity
import com.github.sebastianp265.aitrainer.data.db.entity.DailyEnergyEntity
import com.github.sebastianp265.aitrainer.data.db.entity.SetEntryEntity
import com.github.sebastianp265.aitrainer.data.db.entity.WorkoutSessionEntity
import com.github.sebastianp265.aitrainer.domain.model.EnergySource
import com.github.sebastianp265.aitrainer.domain.model.WeightSource
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.todayIn
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class DemoDataSeeder(
    private val db: AppDatabase,
    private val clock: Clock,
) {

    private class PlannedExercise(val name: String, val startKg: Double, val weeklyIncreaseKg: Double)

    private companion object {
        const val DAYS = 42
        const val DAYS_PER_WEEK = 7
        val WORKOUT_DAYS_OF_WEEK = setOf(0, 2, 4)
        const val SETS_PER_EXERCISE = 3
        const val REPS_PER_SET = 8
        val WORKOUT_START = LocalTime(18, 0)
        val WORKOUT_DURATION = 75.minutes
        val WEIGH_IN_TIME = LocalTime(7, 0)
        const val RANDOM_SEED = 42

        val WORKOUT_A = listOf(
            PlannedExercise("Back Squat", startKg = 80.0, weeklyIncreaseKg = 2.5),
            PlannedExercise("Barbell Bench Press", startKg = 60.0, weeklyIncreaseKg = 1.25),
            PlannedExercise("Barbell Row", startKg = 60.0, weeklyIncreaseKg = 1.25),
            PlannedExercise("Dumbbell Lateral Raise", startKg = 10.0, weeklyIncreaseKg = 0.0),
        )
        val WORKOUT_B = listOf(
            PlannedExercise("Conventional Deadlift", startKg = 100.0, weeklyIncreaseKg = 2.5),
            PlannedExercise("Overhead Press", startKg = 40.0, weeklyIncreaseKg = 1.25),
            PlannedExercise("Pull-Up", startKg = 0.0, weeklyIncreaseKg = 0.0),
            PlannedExercise("Hammer Curl", startKg = 14.0, weeklyIncreaseKg = 0.0),
        )
    }

    suspend fun replaceWithDemoData() {
        val timeZone = TimeZone.currentSystemDefault()
        val firstDay = clock.todayIn(timeZone).minus(DatePeriod(days = DAYS))
        val random = Random(RANDOM_SEED)

        db.withTransaction {
            db.workoutDao().deleteAllSessions()
            db.bodyWeightDao().deleteAll()
            db.dailyEnergyDao().deleteAll()

            val exerciseIdsByName = db.exerciseDao().getAll().associate { it.name to it.id }
            var workoutCount = 0
            repeat(DAYS) { dayIndex ->
                val date = firstDay.plus(DatePeriod(days = dayIndex))
                val isWorkoutDay = dayIndex % DAYS_PER_WEEK in WORKOUT_DAYS_OF_WEEK
                if (isWorkoutDay) {
                    val workout = if (workoutCount % 2 == 0) WORKOUT_A else WORKOUT_B
                    insertWorkout(date, timeZone, week = dayIndex / DAYS_PER_WEEK, workout, exerciseIdsByName)
                    workoutCount++
                }
                insertWeighIn(date, timeZone, dayIndex, random)
                insertDailyEnergy(date, isWorkoutDay, random)
            }
        }
    }

    private suspend fun insertWorkout(
        date: LocalDate,
        timeZone: TimeZone,
        week: Int,
        workout: List<PlannedExercise>,
        exerciseIdsByName: Map<String, Long>,
    ) {
        val startedAt = date.atTime(WORKOUT_START).toInstant(timeZone)
        val sessionId = db.workoutDao().insertSession(
            WorkoutSessionEntity(
                date = date,
                startedAt = startedAt,
                finishedAt = startedAt + WORKOUT_DURATION,
                planSessionRef = null,
                notes = null,
            ),
        )
        var positionInSession = 0
        workout.forEach { planned ->
            repeat(SETS_PER_EXERCISE) {
                db.workoutDao().insertSet(
                    SetEntryEntity(
                        sessionId = sessionId,
                        exerciseId = exerciseIdsByName.getValue(planned.name),
                        positionInSession = positionInSession++,
                        reps = REPS_PER_SET,
                        weightKg = planned.startKg + planned.weeklyIncreaseKg * week,
                        rpe = null,
                        isWarmup = false,
                    ),
                )
            }
        }
    }

    private suspend fun insertWeighIn(date: LocalDate, timeZone: TimeZone, dayIndex: Int, random: Random) {
        val trendKg = 84.0 - 0.04 * dayIndex
        db.bodyWeightDao().insertIgnoringDuplicates(
            BodyWeightEntity(
                measuredAt = date.atTime(WEIGH_IN_TIME).toInstant(timeZone),
                weightKg = (trendKg + random.nextDouble(-0.4, 0.4)).roundToOneDecimal(),
                source = WeightSource.MANUAL,
            ),
        )
    }

    private suspend fun insertDailyEnergy(date: LocalDate, isWorkoutDay: Boolean, random: Random) {
        val workoutBurnKcal = if (isWorkoutDay) 450 else 0
        db.dailyEnergyDao().upsert(
            DailyEnergyEntity(
                date = date,
                kcalBurned = 2400 + workoutBurnKcal + random.nextInt(-150, 151),
                kcalBurnedSource = EnergySource.POLAR,
                kcalEaten = 2300 + random.nextInt(-250, 251),
                proteinG = 150.0 + random.nextInt(-20, 21),
                carbsG = 260.0 + random.nextInt(-40, 41),
                fatG = 75.0 + random.nextInt(-12, 13),
                updatedAt = clock.now(),
            ),
        )
    }

    private fun Double.roundToOneDecimal() = (this * 10).roundToInt() / 10.0
}
