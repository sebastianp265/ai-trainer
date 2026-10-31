# Integration tests

- Tests launch the real app (real Koin graph, real Room database) and tap through it the way I would. Use the `androidx.compose.ui.test.junit4.v2` rules (the non-v2 ones are deprecated).
- UI tests run in **English only**; don't repeat them per language. The exception is tests of localization behaviour itself (e.g. `ExerciseSearchLocalizationITest`), which override `appLanguage` in their own class.

## Where helpers live

- **A test class holds only its tests** plus its test data (e.g. `today`, `daysAgo(...)`). Every UI step or assertion helper goes in a helper file.
- **Used by one test class:** a file next to it, named after it. `XxxITest` → `XxxComposeTestUtils.kt` (e.g. `feature/workout/ui/LogWorkoutFlowComposeTestUtils.kt`, `feature/progress/ui/ProgressFlowComposeTestUtils.kt`). Write helpers as `ComposeTestRule` extensions. A helper class used by one test (e.g. `HealthConnectPermissionScreen` for system screens) also sits next to that test.
- **Used by two or more test classes:** `testing/`. When a second class needs a helper, move it there instead of copying it.
  - `AbstractITest`: abstract base class every app flow test extends. Before each test it sets the app language (`appLanguage`, English by default) and starts from a fresh DB/settings/secrets. It provides the Compose rule, `launchApp()` (optionally with a `FakeHealthDataSource` swapped in through Koin) and `restartApp()`, and closes the app afterwards.
  - `ComposeTestUtils.kt`: the shared timeout (`TEST_TIMEOUT_MS`) and steps used by several classes (wait for text/matcher, open a tab).
  - Fakes of external systems (`FakeHealthDataSource`, later Polar/Claude) always live in `testing/`, even if only one test uses them so far.
