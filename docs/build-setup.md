# Build setup

Read before changing Gradle files, plugins or library versions.

- The project was created from Android Studio's **Empty Activity (Compose)** template. Keep the generated Gradle wrapper, AGP and Kotlin setup; extend it rather than regenerating it.
- Add dependencies through the version catalog (`gradle/libs.versions.toml`). Use current stable versions compatible with the existing AGP/Kotlin; check rather than guess.
- AGP 9 uses **built-in Kotlin**: there is no `kotlin-android` plugin (don't add one), and Kotlin compiler options go in a top-level `kotlin { compilerOptions { } }` block.
- The `kotlin` catalog version (shared by the Compose and serialization plugins) was raised from 2.2.10 to 2.4.21 because Ktor 3.6, kotlinx.serialization 1.12 and Koin 4.2 need it. Don't lower it.
- KSP versions (2.3.0+) are independent of the Kotlin version.
