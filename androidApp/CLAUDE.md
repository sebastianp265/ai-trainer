# :androidApp

Android application, frontend only: entry points, navigation, theme, screens, ViewModels, `…UiModule`s (Koin modules with the ViewModels), resources, manifest.

## Platform and libraries

- My phone runs Android 16 and `minSdk` is 34: don't add version checks or compatibility code for APIs below 34.
- UI is **Jetpack Compose** + **Material 3** with the app's own colors, shapes and type (`AITrainerTheme`, light and dark; no dynamic color). Icons come from `shared.design.DesignIcons` (line icons), not from `material-icons`. Pieces used by several screens (section header, tag, list row, icon circle) live in `shared.design`.
- Navigation routes are `@Serializable` types.

## Layout

- `feature.app`: the app shell (`impl` has `Application`, `MainActivity`, theme, `AppModules`, navigation).
- `feature.<name>`: the root holds the screen entry point and the `…UiModule`; `impl` holds the content composables, ViewModels and test tags. `shared.localization` and `shared.state` hold helpers used by several features.
- A ViewModel that saves edits which must not be lost when the screen closes (settings, secrets) uses `NonCancellable` (see `SettingsViewModel.saveEvenIfScreenIsLeft`).

## Previews

- A `@PreviewLightDark` preview lives in the same file as the composable it previews, at the end of the file.
- Everything a preview needs (fake actions, dates, sample data) is defined inside the preview function itself (e.g. `object : SomeActions { … }`), never as a top-level helper next to it.

## Localization

The app follows the device language: **English** (`res/values/`, fallback) and **Polish** (`res/values-pl/`). Languages are listed in `res/xml/locales_config.xml` (manifest `android:localeConfig`); add new ones there. Don't use AGP's `generateLocaleConfig`.

- Every user-facing string (including content descriptions and snackbar messages) goes in `strings.xml`, in English and Polish. No hardcoded UI text in Kotlin. Lint's `MissingTranslation` must stay clean.
- Counts use `<plurals>`. Numbers and dates are formatted for the current locale (`shared/localization/Localization.kt`), e.g. `7,5` in Polish.
- ViewModels expose typed messages (e.g. a sealed `LogMessage`), not text; the UI turns them into strings.
- Exercise, muscle group and equipment names come only from the translation tables, not `strings.xml`. A new language needs its rows in `seed.sql` (in `:core:common`), the usual `locales_config.xml` entry, and the language added to `ExerciseSeedITest`. Display via `Exercise.displayName(locale)` (falls back to English; `englishName()` for the English one); sort by the displayed name. Search matches the displayed (native) name first, then the English name, for every supported locale.
- Other enums are shown through string resources (e.g. `labelRes()`), never `enum.name`.

## Tests

Unit tests in `src/test` (e.g. `ExerciseListRulesUTest`, `KoinModulesUTest` for the Koin graph). End-to-end tests: see `src/androidTest/CLAUDE.md`.
