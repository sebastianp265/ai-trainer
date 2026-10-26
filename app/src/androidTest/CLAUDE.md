# Integration tests

- Tests launch the real app (real Koin graph, real Room database) and tap through it the way I would. Use the `androidx.compose.ui.test.junit4.v2` rules (the non-v2 ones are deprecated).
- UI tests run in **English only**; don't repeat them per language.
- Use the shared helpers in `testing/`:
  - `AppUnderTest`: English locale, fresh DB/settings/secrets, launch/restart, optional `FakeHealthDataSource` swapped in through Koin.
  - `ComposeTestSteps.kt`: waiting, scrolling, workout steps, reading sets from a card.
- Add reusable steps to `testing/` instead of copying them between tests.
