# AI Trainer — Android app

Personal, single-user Android app that acts as an AI personal trainer. It collects my gym workouts, daily calorie burn and body weight (Polar), calorie intake (Fitatu via Health Connect), plus my preferences, and uses the Claude API to generate and adjust a flexible training + nutrition plan.

## Current phase

**Phase 3: Health Connect integration.** Finish each phase in a working, runnable state (builds, tests pass, app launches), then summarize what was done and what I should check manually.

`docs/status.md` tells me how to run the app, what works and what doesn't. Update it at the end of each phase and whenever a change affects what I'd see when running the app (new or removed screens/features, known bugs, things to verify manually).

## Hard constraints

- **No remote storage of my data.** All data lives on the device (Room).
- Single user, no accounts, no login of our own. I may share the app with a few similar users later, so don't hardcode anything specific to me: each user enters their own API key and Polar credentials in Settings.

## Secrets handling (important)

- **No secrets anywhere in the project directory.** The Claude API key, Polar client ID/secret and Polar tokens are entered at runtime in Settings and stored on the device only, via `SecretStore`.
- Do **not** use the `local.properties` → `BuildConfig` pattern for keys, do not create `.env` files, and do not hardcode keys, even temporarily or in tests. Tests use fake values like `"test-key"`.
- Never ask me to paste a key into the chat. Use placeholders like `<CLAUDE_API_KEY>`.
- Don't print, dump or read the app's private storage (e.g. `adb shell run-as`, `adb backup`) unless I explicitly ask.

## Gradle modules

One shared library (the `core` modules) plus one application module per platform. Each module has its own `CLAUDE.md` with its details.

| Gradle module | Holds |
|---|---|
| `:core:common` (`core/common/`) | Pure Kotlin Multiplatform library (JVM target for now): the `integration` contract, feature logic, the Room database, Koin modules. Depends on no Gradle module. |
| `:core:android` (`core/android/`) | Android implementations of the platform interfaces from `:core:common`'s `integration` (Keystore, Health Connect, Room/DataStore builders). Depends on `:core:common`. |
| `:androidApp` (`androidApp/`) | Android application, frontend only: screens, ViewModels, navigation, resources. Depends on both `core` modules, but uses only their `integration` package. |
| `:architectureTest` (`architectureTest/`) | Kotlin/JVM, tests only: the Konsist architecture rules. Depends on no other Gradle module. |

## Package modules (the same in every Gradle module)

Terms used below: the **base package** is `com.github.sebastianp265.aitrainer` (written `…aitrainer`). A **package module** is one package `…aitrainer.<group>.<name>` and its subpackages. Its **root** is that package itself (`…aitrainer.<group>.<name>`); the subpackage `impl` is **hidden**. The same layout applies in every Gradle module. The structure is declared in `architectureTest` (`GradleModule`, `PackageModule`) and enforced by the architecture tests; keep them passing.

### Layout

There are exactly three groups directly under the base package: `feature`, `shared` and `integration`. Nothing else lives there. The app shell is the package module `feature.app` (only in `androidApp`); test helpers are `shared.e2e` and `shared.architecture`.

```
…aitrainer/
  feature/<domain>/        logic (core) or UI (androidApp); root = Koin module file (XModule.kt, XUiModule.kt) and screen entry points
    impl/                  hidden: services, ViewModels, content composables
  shared/<name>/           data access and helpers; root = what others use (entities, DAOs, formatDate, …)
    impl/                  hidden
  integration/<domain>/    the contract: interfaces, enums, data classes without functions, constants
  integration/shared/      contract for platform capabilities (SecretStore, DatabaseBuilders, …)
  integration/module/      the one exception: each core module's Koin modules as public values
```

Every domain has the same name in all three groups: `integration.workout`, `shared.workout`, `feature.workout`. Groups themselves (`feature`, `shared`, `integration`) contain no files.

### Who may depend on whom

| From | May depend on |
|---|---|
| `feature.*` | `feature`, `shared`, `integration` |
| `shared.*` | `shared`, `integration` |
| `integration.*` | `integration` |
| `feature.app`, `integration.module` | everything of their Gradle module |

Nothing may depend on `feature.app` or `integration.module`. No cycles between package modules.

### Where to put new code

| New thing | Goes to |
|---|---|
| Room entity or DAO | `shared.<domain>` in `core:common` |
| Service implementation | `feature.<domain>.impl` in `core:common` |
| Interface another Gradle module implements or calls | `integration.<domain>` (platform capability: `integration.shared`) |
| Android implementation of such an interface | `shared.<name>.impl` in `core:android` |
| Screen, ViewModel, composable | `feature.<domain>` (root: entry point, `impl`: the rest) in `androidApp` |
| Koin module of a `core` module | a value in that module's `integration.module` list |

## Testing

- **End-to-end tests** (`E2ETest`, `androidApp/src/androidTest/`) launch the real app and tap through it. **Integration tests** (`ITest`) cover Android code the JVM can't run, e.g. `KeystoreSecretStoreITest`. **Unit tests** (`UTest`) cover pure logic and the architecture rules.
- A test sits in the package of the class it tests.
- Test function names are plain sentences in backticks, e.g. `` `written secret is read back` ``.
- Unit tests fake only what needs a running emulator (Health Connect, Keystore, the Android framework). Our own classes are never mocked. E2E tests fake at the `integration` seam (`FakeHealthService`), not below it.

## Build and verify

Run Gradle from the project root. On this Windows machine the wrapper is `./gradlew.bat` (also from Git Bash); add `--console=plain` when the output is read by a tool.

| Goal | Command |
|---|---|
| **Fast check while working:** every quick JVM test (architecture rules + app unit tests) | `./gradlew.bat unitTest` |
| Build everything and check it (compiles every module, unit tests, architecture tests, lint, release build); slow, run it before a commit, not after every change | `./gradlew.bat build` |
| Run the app on a device or emulator | Android Studio: pick a device, run the `androidApp` configuration. Terminal: `./gradlew.bat :androidApp:installDebug` |
| Architecture rules only | `./gradlew.bat :architectureTest:test` |
| App unit tests (Koin graph, UI logic) | `./gradlew.bat :androidApp:testDebugUnitTest` |
| Instrumented tests of the Android core (Keystore, exercise seed); emulator only | `./gradlew.bat :core:android:connectedDebugAndroidTest` |
| End-to-end tests; emulator only, they uninstall the app and delete its data | `./gradlew.bat :androidApp:connectedDebugAndroidTest` |
| Compile the instrumented tests without a device | `./gradlew.bat :androidApp:compileDebugAndroidTestKotlin :core:android:compileDebugAndroidTestKotlin` |
| One test class | add `--tests "*ClassName"` to a test task |

Don't run `clean` by default: it throws away Gradle's incremental work and makes every build take minutes. Use it only when a build looks stale or after moving files between modules. After a change run the narrowest task that covers it (`unitTest`, or one module's compile task).

The SDK levels (`compileSdk`, `minSdk`, `targetSdk`) and the Java version are set once, in the root `build.gradle.kts`; module build files don't repeat them.

The emulator needs API 34+ with a *Google Play* image (Health Connect is built in). What the app does and what to check by hand is in `docs/status.md`.

## Conventions

- Placing multiple declarations (classes, top-level functions or properties) in the same Kotlin source file is encouraged as long as these declarations are closely related to each other semantically and the file size remains reasonable (not exceeding a few hundred lines).
- **No comments in code** (Kotlin, XML, Gradle, tests). When something isn't clear, rename, extract a well-named function/constant/type, or restructure. Explain Android concepts and design reasons in your reply.
- Commit-sized changes; don't refactor unrelated code.
- Never commit secrets, keystores or `local.properties`.
- `.idea` is a whitelist in `.gitignore` (`/.idea/*` plus `!` exceptions); add a line there to share another IDE file.