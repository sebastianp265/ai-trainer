# :core:android

Android library: implementations of the platform interfaces declared in `:core:common`'s `integration` (`SecretStore`, `HealthDataSource`, `DatabaseBuilders`, `PreferencesStores`, `BundledFiles`) and their Koin modules (`androidModules`).

## Layout

- `shared.security`: Keystore-backed `SecretStore` (`impl` has the cipher and the store).
- `shared.platform`: Room and DataStore builders and the bundled-file reader (`impl`).
- `shared.health`: the Health Connect data source (`impl`).

## Secret storage

Jetpack **DataStore** + an AES-256-GCM key in the **Android Keystore** (non-exportable); each value encrypted with a random IV stored alongside. Do **not** use `androidx.security:security-crypto` / EncryptedSharedPreferences (deprecated).

## Tests

Integration tests (`ITest`) in `src/androidTest` need the emulator: `KeystoreSecretStoreITest` (next to the store) and `ExerciseSeedITest` (builds the real Koin graph and checks the seeded exercises).
