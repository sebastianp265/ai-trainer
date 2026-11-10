# Decisions

Choices made without asking while working through [`plan.md`](plan.md). Date, question, choice, why.

## Phase 4

- 2026-11-10 · Which test dependencies for Room on the JVM? · `androidx.sqlite:sqlite-bundled`, `androidx.room:room-testing` and `kotlinx-coroutines-test` in `:core:common`'s `jvmTest` · Migration and sync tests need a real Room database on the JVM; they are Jetpack/Kotlin libraries of the stack already in use.
- 2026-11-10 · English name of the first tab · "Training" (Polish "Trening") · Matches the requirements' "Trening"; the route is still `LogRoute`.
- 2026-11-10 · Top bar on the tabs · only the gear icon, no title · A title would repeat the tab label in the bottom bar.
