# AI Trainer — product spec

The requirements behind each build phase. `CLAUDE.md` holds the rules that apply to every change; this file describes *what* to build. Read the relevant sections before starting a phase or a feature in that area.

## Build phases

Finish each phase in a working, runnable state (builds, tests pass, app launches) before starting the next. At the end of each phase, summarize what was done and what I should check manually.

1. **Foundation** (done): add Koin, KSP, Room, Navigation, Ktor, serialization and kotlinx-datetime to the existing project; `Application` class starting Koin; entities, DAOs, seed exercise library; app shell with bottom navigation; workout logging screen; disable backups; Koin module verification test.
2. **Settings + secrets** (done): `SecretStore` (DataStore + Keystore AES-GCM, with a unit/instrumented test for round-trip encryption), settings screen, profile/preferences, debug screen with seed demo data.
3. **Health Connect integration**: permissions, on-demand read of the last 30 days, shown as a plain list on the Progress tab to verify the data. Nothing is stored.
4. **Polar integration**: OAuth flow, user registration, daily activity and body weight, shown as plainly as possible to verify the data.
5. **Claude integration**: client (real + fake), API key and model from Settings, error handling, a simple test request to verify the connection.
6. **Design** (together, once the real data shapes are known): what to store and how, domain stats, Progress and Plan screens, plan generation, "Adjust today".
7. **Polish**: weekly review worker, DB export/import, error states, empty states. Switch from the prototype's destructive DB recreation to real migrations, starting again from version 1 (steps in `docs/prototype-database.md`).

## Screens (MVP)

1. **Log workout**: start a session (from the plan or ad hoc), add exercises, log sets quickly ("same as last time", +/- buttons), finish.
2. **Plan**: designed in phase 6 (ideas: current week, today's session highlighted, daily kcal/protein target, "Adjust today" button).
3. **Progress**: until phase 6, a plain list of the data read from the integrations. Final presentation designed in phase 6 (ideas: weight chart, calorie balance, weekly sets per muscle group).
4. **Settings**: goal and preferences, weak muscle groups, equipment, exercise preferences, Claude API key and model, Polar connect/disconnect, Health Connect permissions, DB export/import.
5. **Debug** (debug builds only, code in `src/debug/`): switch between real and fake Health Connect / Polar / Claude.

## Data model (Room)

The original design. The `…Entity` classes in each `feature/<name>/data/` package are the source of truth once a table exists.

- `exercise` — id, key (`ExerciseKey`, unique; names come from string resources), primaryMuscles, secondaryMuscles, equipment, preference (FAVOURITE / NEUTRAL / AVOID). Later: `customName` with a null key for user-created exercises.
- `workout_session` — id, date, startedAt, finishedAt, planSessionRef (nullable), notes
- `set_entry` — id, sessionId, exerciseId, positionInSession, reps, weightKg, rpe (nullable), isWarmup
- `profile_prefs` — goal (CUT / MAINTAIN / BULK / RECOMP), sessionsPerWeek, sessionLengthMin, weakMuscleGroups, availableEquipment, free-text notes
- Calorie, body weight and plan storage: designed in phase 6, once the integrations show what the data looks like.

## Domain calculations

Local, deterministic and unit-tested, computed in `domain/`. Claude should not do the arithmetic.

- Weekly sets per muscle group (primary = 1, secondary = 0.5)
- Adherence: planned vs completed sessions
- Weight trend: 7-day moving average and kg/week slope
- Average daily intake, average burn, average deficit/surplus
- **Empirical TDEE** from the weight trend vs logged intake over 2–4 weeks (wearable calorie burn is imprecise; prefer this when enough data exists)
- Per-exercise progression info (best set, estimated 1RM, last session's load)

## Data sync

- **Health Connect** (read on demand when the Progress tab is shown; nothing is stored; the Progress screen shows the Health Connect card only until it's connected): read nutrition only (kcal, macros) per day (`aggregateGroupByPeriod`, last 30 days), with the source apps of each day. Calories burned and body weight come from Polar, not Health Connect: Health Connect's total calories burned is filled with a constant estimate on days no app wrote any, so it isn't reliable. Request permissions with a clear UI. Health Connect is part of the OS on API 34+, but still check `HealthConnectClient.getSdkStatus()` and handle "unavailable" / "update required" gracefully. Read-only: the app never writes to Health Connect.
- **Polar AccessLink**: OAuth 2.0 authorization code flow via Custom Tabs, with a redirect back into the app (custom scheme, e.g. `aitrainer://polar-callback`; verify Polar accepts it, otherwise propose an alternative). Exchange the code for a token, register the user with AccessLink once, then pull daily activity / calories daily. **Verify endpoints and flows against the official AccessLink v3 documentation** rather than assuming them.
- **Body weight from Polar**: read it from AccessLink's physical information data (weight is part of the user's physical settings in Polar Flow). Before building this, check in the docs how weight history is exposed (a new entry per change vs only the current value) and report back.
- Manual weight entry and how to combine data from several sources: decided in phase 6.

## Claude integration

- Model id configurable in Settings (default: `claude-sonnet-5-5`).
- API key read from encrypted storage at call time.
- Handle errors gracefully: 401 (bad key), 429 / rate limit, spend limit or credits exhausted, network errors → show "AI trainer unavailable" with the reason, and keep the rest of the app working.
- **Plan generation**: build a compact prompt from profile_prefs + domain stats (last 4 weeks) + exercise library (filtered by preference and equipment). Require output in a strict JSON schema:
  - `weeks` → `sessions` → `exercises` → `{exerciseName, sets, repRange, targetRpe, restSec, notes}`
  - `nutrition` → `{dailyKcal, proteinG, notes}`
  - `rationale` (short text)
- Validate the response with kotlinx.serialization plus sanity checks (exercises exist in the library, sensible ranges). On failure, retry once including the validation error; then surface the error.
- Ask for free text (rationale, notes, "adjust today" replies) in the app's current language; exercise names in the plan JSON stay the English keys.
- **"Adjust today"**: send today's planned session plus my free-text note (e.g. "only 40 min, sore shoulder") → receive a modified session in the same schema.
- **Weekly review** (WorkManager, toggle in Settings, off by default): regenerate next week's plan from the past week's data.
- All prompts live in one place (`PromptBuilder`) so they're easy to iterate on.

## Fakes

- `FakeClaudeClient` (returns a canned valid plan), `FakePolarClient` and a fake health data source with ~6 weeks of realistic daily totals, swappable via Koin so I can develop the UI on the emulator offline and without API costs. They live in `src/debug/` only.
- No demo data seeding: integration data always enters the app through an integration (real or fake) and the real sync path. Workouts come only from using the app.
