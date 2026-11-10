# Decisions

Choices made without asking while working through [`plan.md`](plan.md). Date, question, choice, why.

## Phase 4

- 2026-11-10 · Which test dependencies for Room on the JVM? · `androidx.sqlite:sqlite-bundled`, `androidx.room:room-testing` and `kotlinx-coroutines-test` in `:core:common`'s `jvmTest` · Migration and sync tests need a real Room database on the JVM; they are Jetpack/Kotlin libraries of the stack already in use.
- 2026-11-10 · English name of the first tab · "Training" (Polish "Trening") · Matches the requirements' "Trening"; the route is still `LogRoute`.
- 2026-11-10 · Top bar on the tabs · only the gear icon, no title · A title would repeat the tab label in the bottom bar.
- 2026-11-10 · Instrumented tests between 4.3 and 4.6 · The old Progress end-to-end tests are rewritten in 4.6, so they do not compile in the commits for 4.3 to 4.5 · The old screen and its fake are replaced by the stored-data Progress; keeping both alive would be throwaway work.
- 2026-11-10 · What counts as "connected" · at least one of the 8 data types granted · All types are optional (requirements 7).
- 2026-11-10 · Workout details (distance, heart rate) · two extra read permissions are requested together with the other types; they have no row of their own, and kcal comes from the "calories burned" permission when granted · The eight rows in the plan are the data types; details are best-effort.
- 2026-11-10 · "Zmień uprawnienia w Health Connect" · opens Health Connect's own settings · The system permission dialog does not reappear after repeated denials.
- 2026-11-10 · History permission after the first sync · a first sync without it reads 30 days and a later grant does not trigger a one-year backfill · Not in the requirements; keeps the sync rule simple.
- 2026-11-10 · Sleep amount · duration from start to end of the session, sessions ending on the same day are summed · Requirements say only "duration, assigned to the day of waking up".
- 2026-11-10 · Today in the progress averages · calorie balance, macros and steps leave out today (the day is not over); weight, sleep, heart rate and HRV include it · Today's partial eating or steps would distort the averages.
- 2026-11-10 · "7-day average" of weight and resting heart rate · mean of the measurements in the 7 days up to each measurement day; the change over the range is last average minus first · Simple and stable with missing days.
- 2026-11-10 · Calorie balance weekly bars · average balance per Monday-to-Sunday week that has days with both eaten and burned · Matches "weekly bars" in requirements 8.
- 2026-11-10 · Unknown Health Connect workout types · shown as "Other activity" · No English-only fallback text; the common types have their own Polish and English names.
- 2026-11-10 · Strength rows on Progress · up to four exercises with the most working sets in the range, bodyweight sets and sets above 12 reps left out, a row is not tappable yet · The exercise history screen is built in phase 5 (task 5.7).
- 2026-11-10 · Where "All workouts" opens from · the "All" link next to "Recent workouts" on the Training tab · The mockups link it from the same place.
- 2026-11-10 · Identity of a workout from another app · its start time in milliseconds (the route argument) · The stored rows are rewritten on every sync, so their ids are not stable.
- 2026-11-10 · Gym workouts in the list · titled "Workout" and not tappable · Names and the detail screen come with plans in phase 5 (WorkoutDetail).

## Phase 5

- 2026-11-10 · Which muscle groups? · The six of requirements 3 (chest, back, legs, shoulders, arms, abs) replace the 18 fine groups through migration 7→8; an exercise keeps its first primary group as the one primary and the others become secondary · Requirements win over the old seed; one primary per exercise.
- 2026-11-10 · Equipment of a custom exercise · migration 7→8 also adds the equipment "Other" (Inne) · An exercise needs an equipment row and the create form asks only for name and groups.
- 2026-11-10 · Task 5.2 schema · no migration; `set_entry.rpe` already exists · Only the picker is missing.
- 2026-11-10 · Lint of JVM test files · The IDE reports unresolved references in `jvmTest` files (also in untouched lines), so Gradle compilation is the check there · IDE module sync of the multiplatform test source set is stale.
- 2026-11-10 · Plan tables · `plan` and `planned_set` only (a planned exercise is the group of its sets, like in `set_entry`) · Same shape as a workout makes starting from a plan a plain copy.
- 2026-11-10 · Workout screen and navigation · A running workout is its own full-screen route (no bottom bar, no app top bar); the app opens it whenever a session is running and closes it when the session ends · One source of truth, survives closing the app. Back moves the app to the background.
- 2026-11-10 · Exercise cards in a running workout · all expanded by default; tapping the header collapses one to "N sets" · The mockup shows collapsed cards but not what expands them.
- 2026-11-10 · "More" button in the workout top bar · left out · The mockup defines nothing behind it ("Odrzuć" is at the bottom already).
- 2026-11-10 · RPE picker values · all nine steps 6 to 10 by 0.5 (the mockup omits 6.5) · Requirements 4.
- 2026-11-10 · Bodyweight sets · weight 0 is shown as "–" and counts as bodyweight in the last-time label · No separate flag in the data.
- 2026-11-10 · "+N" in the last-time label · The text is measured; a group is dropped until two lines fit and its sets are counted in "+N"; spaces inside a group do not break · Compose's flow-row overflow API is deprecated.
