# Decisions

Choices made without asking while working through [`plan.md`](plan.md). Date, question, choice, why.

## Phase 4

- 2026-11-10 · Which test dependencies for Room on the JVM? · `androidx.sqlite:sqlite-bundled`, `androidx.room:room-testing` and `kotlinx-coroutines-test` in `:core:common`'s `jvmTest` · Migration and sync tests need a real Room database on the JVM; they are Jetpack/Kotlin libraries of the stack already in use.
- 2026-11-10 · English name of the first tab · "Training" (Polish "Trening") · Matches the requirements' "Trening"; the route is still `LogRoute`.
- 2026-11-10 · Top bar on the tabs · only the gear icon, no title · A title would repeat the tab label in the bottom bar.
- 2026-11-10 · Instrumented tests between 4.3 and 4.6 · The old Progress end-to-end tests are rewritten in 4.6, so they do not compile in the commits for 4.3 to 4.5 · The old screen and its fake are replaced by the stored-data Progress; keeping both alive would be throwaway work.
- 2026-11-10 · What counts as "connected" · at least one of the 8 data types granted · All types are optional (requirements 7).
- 2026-11-10 · Workout details (distance, heart rate) · two extra read permissions are requested together with the other types; they have no row of their own, and kcal comes from the "calories burned" permission when granted · The eight rows in the plan are the data types; details are best-effort.
- 2026-11-10 · "Zmień uprawnienia w Health Connect" · opens Health Connect's own settings · The system permission dialog does not reappear after repeated denials.
- 2026-11-12 · Health Connect switches · one switch per type in Settings replaces the Health Connect screen; switching off is an app-level switch, the Health Connect permission stays · Android lets an app revoke only all of its permissions at once, not one.
- 2026-11-12 · No copy of Health Connect data · screens and the coach read Health Connect when they need it; the tables `health_day` and `health_workout` are dropped (migration 15 to 16), and there is no sync, no sync button and no "last year" button · The owner wants no health data stored in the app; the history is as long as Health Connect keeps it.
- 2026-11-12 · Data older than 30 days · a row with "Zezwól" asks for Health Connect's history permission; with it reads have no time limit · Health Connect itself has no limit once the permission is granted.
- 2026-11-12 · Workout details · the list of all workouts shows type, date and duration only; distance, kcal and heart rate are read for one workout at a time · One aggregate read per workout would hit Health Connect's read limits on a list of several years.
- 2026-11-10 · Sleep amount · duration from start to end of the session, sessions ending on the same day are summed · Requirements say only "duration, assigned to the day of waking up".
- 2026-11-10 · Today in the progress averages · calorie balance, macros and steps leave out today (the day is not over); weight, sleep, heart rate and HRV include it · Today's partial eating or steps would distort the averages.
- 2026-11-10 · "7-day average" of weight and resting heart rate · mean of the measurements in the 7 days up to each measurement day; the change over the range is last average minus first · Simple and stable with missing days.
- 2026-11-10 · Calorie balance weekly bars · average balance per Monday-to-Sunday week that has days with both eaten and burned · Matches "weekly bars" in requirements 8.
- 2026-11-10 · Unknown Health Connect workout types · shown as "Other activity" · No English-only fallback text; the common types have their own Polish and English names.
- 2026-11-10 · Strength rows on Progress · up to four exercises with the most working sets in the range, bodyweight sets and sets above 12 reps left out, a row is not tappable yet · The exercise history screen is built in phase 5 (task 5.7).
- 2026-11-10 · Where "All workouts" opens from · the "All" link next to "Recent workouts" on the Training tab · The mockups link it from the same place.
- 2026-11-10 · Identity of a workout from another app · its start time in milliseconds (the route argument) · The stored rows are rewritten on every sync, so their ids are not stable.
- 2026-11-10 · Gym workouts in the list · titled "Workout" and not tappable · Names and the detail screen come with plans in phase 5 (WorkoutDetail).

## Phase 5

- 2026-11-10 · Which muscle groups? · The six of requirements 3 (chest, back, legs, shoulders, arms, abs) replace the 18 fine groups through migration 7→8; an exercise keeps its first primary group as the one primary and the others become secondary · Requirements win over the old seed; one primary per exercise.
- 2026-11-10 · Equipment of a custom exercise · migration 7→8 also adds the equipment "Other" (Inne) · An exercise needs an equipment row and the create form asks only for name and groups.
- 2026-11-10 · Task 5.2 schema · no migration; `set_entry.rpe` already exists · Only the picker is missing.
- 2026-11-10 · Lint of JVM test files · The IDE reports unresolved references in `jvmTest` files (also in untouched lines), so Gradle compilation is the check there · IDE module sync of the multiplatform test source set is stale.
- 2026-11-10 · Plan tables · `plan` and `planned_set` only (a planned exercise is the group of its sets, like in `set_entry`) · Same shape as a workout makes starting from a plan a plain copy.
- 2026-11-10 · Workout screen and navigation · A running workout is its own full-screen route (no bottom bar, no app top bar); the app opens it whenever a session is running and closes it when the session ends · One source of truth, survives closing the app. Back moves the app to the background.
- 2026-11-10 · Exercise cards in a running workout · all expanded by default; tapping the header collapses one to "N sets" · The mockup shows collapsed cards but not what expands them.
- 2026-11-10 · "More" button in the workout top bar · left out · The mockup defines nothing behind it ("Odrzuć" is at the bottom already).
- 2026-11-10 · RPE picker values · all nine steps 6 to 10 by 0.5 (the mockup omits 6.5) · Requirements 4.
- 2026-11-10 · Bodyweight sets · weight 0 is shown as "–" and counts as bodyweight in the last-time label · No separate flag in the data.
- 2026-11-10 · "+N" in the last-time label · The text is measured; a group is dropped until two lines fit and its sets are counted in "+N"; spaces inside a group do not break · Compose's flow-row overflow API is deprecated.
- 2026-11-10 · Exercise picker · a full-screen route; the chosen exercise key is handed back to the previous screen through its saved state; a newly created exercise is picked at once · Plan editing (5.9) reuses the picker.
- 2026-11-10 · Muscle filter in the picker · by the exercise's primary group; the chip row lists the six groups · Mockup shows five chips; requirements name six.
- 2026-11-10 · Custom exercise names · stored in English only (the typed name), shown in every language · One name field in the form.
- 2026-11-10 · Last-time text helpers · moved to `shared.workout` in `androidApp` · Used by the workout card and the picker.
- 2026-11-10 · Exercise history numbers · "est. 1RM" is the best set of the latest session; "best set" is the highest estimate of all sessions; "1RM in 3 months" is the latest minus the first estimate within three months (needs two); "record" marks a session beating all earlier ones (the first session is never a record) · Mockup gives no definitions.
- 2026-11-10 · RPE chart caption · a general sentence instead of the mockup's data-specific one · The mockup text describes sample data.
- 2026-11-10 · "Zapytaj trenera o to ćwiczenie" on the exercise history · added with the coach in phase 6 · Requirements 2: no coach elements until then.
- 2026-11-10 · Session rows of the exercise history · open the finished workout once its detail exists (5.13) · The detail screen does not exist yet.
- 2026-11-10 · A plan is done when · a finished workout has its id (`workout_session.planId`, replacing the unused `planSessionRef`); deleting that workout makes the plan upcoming again · One source of truth, no copy of the state in the plan; schema 8→9 is an auto-migration.
- 2026-11-10 · Sets in plan summaries · working sets only (warm-up sets are not counted in "N serii") · Requirements 4: statistics use working sets.
- 2026-11-10 · Plan editing · the same screen for new and existing plans; "Zapisz" for a new plan opens its detail, for an existing one returns to it · Matches the mockup links.
- 2026-11-10 · Coach elements on Plan screens (coach note, "Zmień z trenerem", "Poproś trenera o plan", "Wolisz gotowy plan?") · added with the coach in phase 6 · Requirements 2.
- 2026-11-10 · Task 5.11 start of a workout from a plan · `WorkoutService.startSession(planId)` is built in 5.9 so "Zacznij trening" on the plan detail works; its end-to-end test comes with 5.11 · The button needs it.
- 2026-11-10 · Instrumented test names · must not contain commas (dex rejects them) · Found when compiling the plan tests.
- 2026-11-10 · Commits of 5.10 to 5.17 and 6.1 to 6.5 · made as one commit; the tasks share files (services, navigation, strings, schema 10 and 11) and could not be separated without throwaway work · Deviation from "one commit per task".
- 2026-11-10 · Exercise lists in screens · `ExerciseService` exposes the exercises as a flow (all seven tables combined, half-written exercises skipped) · A created exercise must reach screens that are already open.
- 2026-11-10 · Coach switch default · off until phase 6.9 · Requirements 2: until the coach exists the app behaves as with the coach off.
- 2026-11-10 · Rows "Kopia danych" and "Debug" in Settings · added with 7.2 and 6.11 · Their screens do not exist yet.
- 2026-11-10 · Group names in "Plans for this week do not reach the target" · nominative names (Nogi, Brzuch) instead of the mockup's genitive (nóg, brzucha) · Names come from the translation tables.
- 2026-11-10 · Default of "Pokazuj cele" · on · Mockup shows it on.
- 2026-11-10 · Editing a finished workout · it is re-opened as the running workout (its start is moved so the duration stays); discarding it then deletes it · Reuses the whole workout screen.
- 2026-11-10 · Claude client · Ktor (named in the plan) and raw HTTP instead of the Java SDK, behind `integration.coach.ClaudeClient` · `:core:common` is multiplatform and the SDK is JVM-only; request shape follows the claude-api skill (`x-api-key`, `anthropic-version`, tools with `input_schema`, no `temperature`, no forced `tool_choice`).
- 2026-11-10 · Thinking for the coach · `thinking: {type: "between_tools"}` (thinking off, accepted by claude-sonnet-5-5 at effort high or below) · Stored conversations keep only text; replaying earlier thinking blocks would run into preserved thinking's history-editing check. Unknown content blocks inside one tool loop are passed back unchanged.
- 2026-11-10 · API key check · `GET /v1/models?limit=1` with the typed key · Validates the key without spending tokens.

## Phase 6

- 2026-11-10 · Coach switch default · on from 6.9 (it was off while the coach did not exist) · Requirements 2.
- 2026-11-10 · Debug fakes · two switches in the debug screen (fake coach, fake Health Connect), debuggable builds only · Lets the whole app be tried without a key or data.
- 2026-11-10 · `DefaultCoachService` gets the Claude client through a provider · the service is a single, so tests that swap the client in Koin must reach it · Found when the e2e suite kept the first client.

## Phase 7

- 2026-11-10 · Weekly review trigger · when the app comes to the foreground, the coach is on, a key is stored, the last Monday-to-Sunday week has data and was not reviewed; a failed review is tried again on a later foreground · No background work needed.
- 2026-11-10 · Review prompt · a transient user message that is not stored; the stats card is computed locally and stored with the coach's reply · The chat shows the card and the reply, not the prompt.
- 2026-11-10 · Backup format · a copy of the Room database file (VACUUM INTO), without the API key (it is in the Keystore store) · Import checks the file, shows its counts, replaces the database and restarts the app.
- 2026-11-10 · Coach shortcuts on screens · shown only when the coach is on, through `LocalCoachEnabled` and `LocalOpenCoach` · Requirements 2.

## After phase 7

- 2026-11-10 · Debug mode · removed on the owner's request: the debug screen and its Settings row, the fake coach and fake Health Connect switches, the log of requests to Claude (table `coach_log`, dropped by migration 12→13) and the debug end-to-end test · The app holds only production code; fakes live in the tests. This replaces the phase 6 decision "Debug fakes".
- 2026-11-10 · Look of the app · the colors, icons, type and shapes of the mockups replace Material 3 dynamic color, on the owner's request; the dark scheme is derived from the same green · The owner wants the app to look like the design.
- 2026-11-10 · Coach note on a plan (PlanDetail mockup) · the summary of the accepted proposal is stored as `plan.coachNote` (schema 14); a manual edit keeps it, a later coach change replaces it; shown only with the coach on · The proposal summary is the coach's reason for the plan.
- 2026-11-10 · Top bar on the tabs · the tab name as the title, as in the mockups (replaces the phase 4 choice "no title") · The design shows it.
- 2026-11-11 · Undo of accepted proposals · each accepted proposal stores what it replaced (`coach_proposal.undo`, schema 15); "Cofnij" is offered while no workout was started from its plans, its target is unchanged and a changed running workout still runs; a new exercise cannot be undone because exercises cannot be deleted · The owner asked for undo and did not answer how long it should last; this keeps undo safe without overwriting later work.
- 2026-11-11 · Manual plan edit and the coach note · editing a plan by hand now clears the coach's note (replaces the 2026-11-10 choice to keep it) · The note no longer fits a plan the user changed.
- 2026-11-11 · Coach during a workout · the coach may change the sets of the running workout, add exercises and remove them, through one proposal that lists all sets of each named exercise; the same tool fixes a finished workout · The owner did not answer whether exercises may be added or removed; a full list per exercise is the simplest change the user can check on the card.
- 2026-11-11 · Topic of a message · opening the coach from a plan, workout, exercise or other app's workout attaches that item to the next message (`coach_message.topic_*`) and opens the chat with a back arrow · The coach should not have to guess what the user is looking at.
- 2026-11-11 · Nutrition target · a daily calorie and protein target set only through a coach proposal, stored in Room (`nutrition_target`) so it is part of the backup · Health Connect stays read-only.
- 2026-11-11 · Voice entry of a set · the phone's speech recognition screen, then a local parser ("80 na 8, RPE 8"); nothing is sent to Claude · Works offline and costs no tokens.
- 2026-11-12 · Voice entry of a set removed · not wanted; sets are typed only · Less UI on the exercise card.
- 2026-11-10 · Debug in the mockups · the mockup `Debug.dc.html` and the Debug row in `Settings.dc.html` and `SettingsNoCoach.dc.html` are deleted · The mockups follow the requirements.
- 2026-11-12 · No Plan tab · Trening shows the week strip, the next plan, the next three plans with "Wszystkie plany", "Napisz własny plan" and the sets this week; the list of all plans is a screen opened from there · The owner found a separate tab unnecessary; the two tabs repeated the next plan and the proposal card.
- 2026-11-12 · Mockups removed · `docs/design/` is deleted and the app itself is the reference for the look · The owner is happy with the design and does not want to maintain the mockups.
- 2026-11-12 · Workouts from other apps removed · no Health Connect workout type, no exercise, distance or heart rate permissions, no read-only workout screen, no filter on the list of all workouts, no marker on the week strip, no coach tool and no count in the weekly review; chat topics about such a workout are cleared by migration 15 to 16 · The owner does not use it and cannot test it.
- 2026-11-12 · No title on the tabs · the top bar of Trening, Trener and Postęp shows only its icons; nested screens keep their title next to the back arrow · The owner finds the title redundant with the selected tab (replaces "the tab name as the title").
- 2026-11-12 · Settings as a tab · Settings is the fourth bottom tab and the tabs have no top bar; the coach memory icon on the Coach tab is removed ("Co trener o mnie wie" stays in Settings) · The owner wants the space of the empty top bar back.
- 2026-11-12 · No weekly sets on Training · the card with this week's done and planned sets is removed, with the planned sets it alone used; Progress keeps the weekly average against the targets · A single calendar week is too noisy to judge volume, and the coach plans with the targets anyway.
- 2026-11-12 · Starting a workout · Training has one way to start: "Zacznij" for the next plan and "Zacznij inny trening" for the sheet with every plan and an empty workout; the "Kolejne plany" section is gone and a single "Plany" row opens all plans for editing · The owner wants one entry point and no list of plans on Training.
- 2026-11-12 · One start button · Training has only "Zacznij trening"; the next plan card, the "Plany" row and the list of all plans are gone; the start sheet lists every plan (info icon opens the plan) plus "Pusty trening" and "Nowy plan" · The owner wants everything about starting and choosing a workout under one button (replaces the two previous entries of today).
- 2026-11-12 · Claude client on the Java SDK · `ClaudeClient` is implemented in `:core:android` (`shared.claude`) with the official `anthropic-java` SDK; Ktor is gone · iOS is unlikely, and the SDK brings typed requests, retries and new API features (replaces the 2026-11-10 "Claude client" decision).
- 2026-11-13 · The coach does not know what answers · `CoachService` is only the chat; replies come from a `CoachResponder`, implemented by the plug-in domain `ai` (Claude tool loop, key, model, tuning, usage, `shared.ai`/`integration.ai`/`feature.ai`); memory stays in the coach as `CoachMemoryService`; `ClaudeClient` becomes `AiClient`; Settings has one "AI" row · A model, a person or a rule engine can answer without touching the coach, and an architecture test keeps the coach from importing `ai`.
