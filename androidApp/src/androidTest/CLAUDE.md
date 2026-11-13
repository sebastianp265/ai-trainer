# End-to-end tests

- Tests launch the real app (real Koin graph, real Room database) and tap through it the way I would. Use the `androidx.compose.ui.test.junit4.v2` rules (the non-v2 ones are deprecated).
- **Each path through the UI is tapped by one test only.** Every other test that needs its result (a saved API key, a plan, a finished or running workout) gets that state from `given` before `launchApp()` and starts where its own path begins. Logic covered by unit tests is not checked again here.
- System screens (Health Connect permissions, file pickers) are never tapped: the fakes at the `integration` seam grant or refuse what the test needs.
- UI tests run in **English only**; don't repeat them per language. The exception is tests of localization behavior itself (e.g. `ExerciseSearchLocalizationE2ETest`), which override `appLanguage` in their own class and pass the translated texts to the screen objects they use.

## Screen objects

- **A test only simulates the user.** It calls methods on screen objects (`progress.open()`, `workout.addSet()`, `settings.assertClaudeApiKeySaved()`) and never touches a Compose rule, selector, matcher or UiAutomator element. Everything technical is hidden inside the screen object.
- **A test class holds** its tests, its test data (e.g. `today`, `daysAgo(...)`) and private user flows that only chain screen calls (e.g. `addBenchPress()`).
- **Screen objects** extend `Screen(compose)` (`shared/e2e/Screen.kt`, with the shared waits and click helpers) and are named after the screen (`ProgressScreen`, `SettingsScreen`, `LogWorkoutScreen`, `ExercisePickerScreen`). Methods are phrased as what the user does (`open`, `save`, `finish`) or sees (`assertWorkoutSaved`).
- **Used by one test class:** the screen object sits in the same package as the test, in a file named after the screen.
- **Used by two or more test classes:** it lives in `shared.e2e`. When a second class needs a screen object or helper, move it there instead of copying it.

## What lives in `shared.e2e`

- `AbstractE2ETest`: abstract base class every app flow test extends. Before each test it sets the app language (`appLanguage`, English by default) and starts from cleared workouts, settings and secrets (the database is emptied through its file, never recreated, so the seeded exercises stay). It provides the Compose rule, `launchApp()` (optionally with a `FakeHealthService` swapped in through Koin) and `restartApp()`, and closes the app afterwards.
- `Given`: sets up state through the `integration` services (`given.aiApiKey()`, `given.plan(...)`, `given.finishedWorkout(..., on = date)`, `given.runningWorkout(...)`), with `sets(...)` to describe the sets.
- `Screen`: base class of the screen objects.
- Screen objects used by several test classes (`LogWorkoutScreen`, `ExercisePickerScreen`).
- Fakes of external systems (`FakeHealthService`, `FakeAiClient`) always live here, even if only one test uses them so far.
