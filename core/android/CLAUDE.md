# :core:android

Android library: implementations of the platform interfaces declared in `:core:common`'s `integration` (`SecretStore`, `HealthDataSource`, `DatabaseBuilders`, `PreferencesStores`, `BundledFiles`, `AiClient`) and their Koin modules (`androidModules`).

## Layout

- `shared.security`: Keystore-backed `SecretStore` (`impl` has the cipher and the store).
- `shared.platform`: Room and DataStore builders and the bundled-file reader (`impl`).
- `shared.health`: the Health Connect data source (`impl`).
- `shared.ai`: `AiClient` on the official Anthropic Java SDK (`impl`); the key is read from `SecretStore` on every request.

## Secret storage

Jetpack **DataStore** + an AES-256-GCM key in the **Android Keystore** (non-exportable); each value encrypted with a random IV stored alongside. Do **not** use `androidx.security:security-crypto` / EncryptedSharedPreferences (deprecated).

## Tests

Unit tests (`UTest`) in `src/test` run on the JVM: `SdkClaudeClientUTest` sends real SDK requests to a local `MockWebServer`.

Integration tests (`ITest`) in `src/androidTest` need the emulator: `KeystoreSecretStoreITest` (next to the store), `SdkClaudeClientITest` (the merged manifest lets the client reach the internet) and `ExerciseSeedITest` (builds the real Koin graph and checks the seeded exercises).
