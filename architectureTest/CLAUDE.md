# :architectureTest

Kotlin/JVM, tests only, **Konsist** on JUnit 5. It reads the other modules' source files as text, so it depends on no other Gradle module; its `test` task declares all source files as inputs, so Gradle reruns it whenever any of them changes.

## Where the structure is declared

- `GradleModule`: every Gradle module with code, its directory, the package modules it contains and which Gradle modules it depends on.
- `PackageModule` and `Group`: every package module as `<group>.<name>` (groups `feature`, `shared`, `integration`). Adding a package module means adding an entry there and listing it in its `GradleModule`.
- A new Gradle module must be declared in `GradleModule`, otherwise `every Gradle module with code is declared` fails.

## The rules

The rules are the test names in `ArchitectureUTest`.

## Principles

- Keep the rules few and generic: no exclusion lists, and no tighter than they're worth. Prefer a rule the compiler or build enforces (module boundaries, the JVM target keeping `android.*` out of `:core:common`) over a test.
- `explicitApi` was considered for rule 7 and rejected; the test stays.
- Rule names are plain sentences that state the rule.
