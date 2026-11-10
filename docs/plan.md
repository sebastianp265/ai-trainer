# Plan

The work left, in order. Each task is small enough to finish and verify in one go. Tick a task
(`[x]`) in the same change that completes it. What the app must do is in
[`requirements.md`](requirements.md); what the screens contain is in [`design/`](design/README.md).

## How to work through this plan

1. Take the first unticked task. Read the requirement sections and screen files it names.
2. Write the test from the task's "Done when" first, where it is a test. Then the code.
3. Run the narrowest Gradle task that covers the change, then `lint_files` on the changed files.
4. Tick the task. Update `status.md` when the change is visible in the running app.
5. Go to the next task. Don't stop to report between tasks.

At the end of a phase: run `./gradlew.bat build`, run the instrumented tests if an emulator is
running (otherwise list them in `status.md` under "Known gaps"), update `status.md` and the
current phase in `CLAUDE.md`, then summarize what was done and what to check by hand.

### Deciding without asking

| Situation | Do |
|---|---|
| Requirements and a mockup disagree | Follow the requirements |
| Both are silent on a detail | Pick the simplest option consistent with the rest, add one line to `decisions.md`, continue |
| A mockup detail is awkward in Compose | Use the standard Material 3 component, continue |
| A task turns out to need a later task | Do the needed part now, note it under that task |
| A test can't run without an emulator | Compile it, note it in `status.md`, continue |
| A build or test fails after three different attempts to fix it | Note what was tried in `decisions.md`, skip to the next independent task |

`decisions.md` is a list: date, the question, the choice, one line of why. The owner reviews it
at the end of each phase.

### Stop and ask only when

- a hard constraint in `CLAUDE.md` would have to be broken,
- a secret, a real API key or a real device is needed,
- data would be deleted or history rewritten (`git reset --hard`, force push, dropping tables
  without a migration),
- a new third-party library is needed beyond those named in this plan,
- no independent task is left to move on to.

### Commits

Commit only when the owner has said so for this run. When they have: work on a branch named after
the phase, one commit per ticked task, never push.

### Every task also means

- Strings in both `values/strings.xml` (English) and `values-pl/strings.xml` (Polish, from the mockups).
- Every schema change has a migration and a migration test; the schema JSON is committed.
- New logic has a unit test. Each new screen is reached by at least one end-to-end test that
  uses fakes at the `integration` seam.
- Architecture tests pass.

## Phase 4: Health Connect data and Progress

Requirements 2, 7, 8. Screens: `Progress`, `ProgressNoHealth`, `ProgressMetric`, `DayDetail`,
`History`, `ExternalWorkout`, `Settings`, `HealthConnect`, `HealthConnectOff`.

- [x] 4.1 Navigation: tabs Trening · Plan · Postęp, Settings behind the gear icon. Done when: an
      end-to-end test opens Settings from the gear on each tab and returns.
- [x] 4.2 Tables `health_day` (one row per date, every column nullable) and `health_workout`,
      in `shared.health`, with an auto-migration from schema 6. Done when: the migration test passes.
- [x] 4.3 `HealthDataSource` reads each data type separately and reports which permissions are
      granted, including the history permission. Done when: `FakeHealthService` can grant any subset.
- [x] 4.4 Sync service: first sync one year or 30 days, later syncs the last 14 days, overwrite
      by date, skip types without permission, ignore this app's own workouts. Done when: unit tests
      cover first sync, re-sync overwriting an edited day, a deleted day, and a missing permission.
- [x] 4.5 Day rules: weight = last of the day, sleep on the waking day, resting HR and HRV as
      daily averages. Done when: unit tests cover each rule.
- [x] 4.6 Sync on app foreground and on "Synchronizuj teraz"; last sync time stored.
- [x] 4.7 Health Connect screens in Settings: state per type, change permissions, history state,
      not connected, unavailable or outdated. The Settings row shows "X z 8".
- [x] 4.8 Progress: range switch and the cards for weight, calorie balance, macros, steps, sleep,
      resting HR, HRV, days. Charts drawn with Compose `Canvas`. Hidden when their data is missing.
      Done when: unit tests cover each card's numbers; an end-to-end test shows cards for a fake
      with partial permissions.
- [x] 4.9 Progress without any Health Connect data.
- [x] 4.10 Metric detail and day detail.
- [x] 4.11 Strength card (estimated 1RM per exercise, requirement 4 "Records and estimates").
- [x] 4.12 All workouts list with workouts from other apps, its filter, and the read-only detail.
- [x] 4.13 Remove the old Progress list and the source-app line. `status.md` updated.

## Phase 5: training, plans and weekly sets, without the coach

Requirements 3, 4, 5, 6. Screens: `Main`, `StartSheet`, `ActiveWorkout`, `WorkoutSummary`,
`WorkoutDetail`, `TrainingEmpty`, `ExercisePicker`, `ExerciseHistory`, `MainNoCoach`, `Plan`,
`PlanDetail`, `PlanEdit`, `PlanEmpty`, `PlanNew`, `VolumeTargets`, `SettingsNoCoach`.

- [x] 5.1 Exercises get a primary and secondary muscle groups; the migration fills them for the
      seeded exercises. Done when: a test asserts every seeded exercise has a primary group.
- [x] 5.2 RPE per set (migration), picker under the set row.
- [x] 5.3 Set row as designed: typed weight, reps stepper, RPE, delete; several warm-up sets.
- [x] 5.4 "Last time" label with the format rules. Done when: unit tests cover equal sets,
      different sets, mixed RPE, no RPE, bodyweight, overflow to `+N`, and no previous workout.
- [x] 5.5 Discard confirmation.
- [x] 5.6 Exercise picker: search, muscle filter, recently used, create an exercise.
- [x] 5.7 Exercise history: stats, 1RM chart, RPE chart, past sessions.
- [x] 5.8 Plan tables (plan, planned exercise, planned set; author; optional date; done link to
      a workout) with a migration.
- [x] 5.9 Plan list, empty state, details, new, edit, delete with confirmation.
- [ ] 5.10 Start screen with the next plan, the plan picker sheet, starting empty, first launch.
- [ ] 5.11 Starting a workout from a plan fills it; saving links the workout and marks the plan done.
      Done when: an end-to-end test starts from a plan, removes a set, changes a weight, finishes,
      and sees the changed sets in the finished workout and the plan gone from the list.
- [ ] 5.12 Summary after finishing: stats, records, comparison with the plan.
- [ ] 5.13 Finished workout: details with plan comparison, edit, delete.
- [ ] 5.14 Weekly sets: fractional counting, Monday to Sunday. Done when: unit tests cover
      primary and secondary counting, warm-ups excluded, week boundaries.
- [ ] 5.15 Weekly targets: table with defaults 10–20, source, date, reason; targets screen with
      manual edit, restore defaults, show-targets switch.
- [ ] 5.16 Sets-per-group card in Progress with the info box; week strip and "sets this week"
      card in Plan.
- [ ] 5.17 Coach switch in Settings (stored). With it off: three tabs, no coach elements.

## Phase 6: the coach

Requirement 9, 10. Screens: `Coach`, `CoachEmpty`, `CoachNoKey`, `CoachError`, `CoachProposal`,
`CoachTargets`, `CoachMemory`, `ApiKey`, `ModelPicker`, `Debug`.

- [ ] 6.1 Claude client behind an `integration` interface, with a fake. Use the `claude-api`
      skill for the request shape, tool use and model ids. HTTP with Ktor.
- [ ] 6.2 Tables: conversation messages, proposals (type, payload, state), coach notes, profile.
- [ ] 6.3 Request builder: profile, notes, targets, data summary, conversation. Done when: a unit
      test asserts the request for a known database state.
- [ ] 6.4 Read tools. Done when: each tool has a unit test against an in-memory database.
- [ ] 6.5 Proposal tools and lifecycle: pending, accepted, superseded; accepting applies the change.
      Done when: unit tests cover accept, supersede, and a target change leaving plans untouched.
- [ ] 6.6 Memory tools and the memory screen.
- [ ] 6.7 Chat screen: messages, proposal cards, "Sprawdzono" line, tool-in-progress indicator.
      Done when: an end-to-end test with the fake client asks for a plan, accepts it, and finds it
      on the Plan tab.
- [ ] 6.8 Proposal preview with changes marked; pending proposal card on Trening, Plan, targets.
- [ ] 6.9 Fourth tab; first conversation; no key; API key screen with a check call; model screen
      with the local usage counter.
- [ ] 6.10 Errors: no network, rejected key, no answer.
- [ ] 6.11 Debug screen.

## Phase 7: weekly review, backup, polish

Requirements 9 (weekly review), 10 (backup). Screens: `CoachWeekly`, `DataBackup`.

- [ ] 7.1 Weekly review, with several plans in one proposal and "Akceptuj wszystkie".
- [ ] 7.2 Export and import.
- [ ] 7.3 Loading states for sync, charts and chat.
- [ ] 7.4 Full pass over empty and error states on every screen.
