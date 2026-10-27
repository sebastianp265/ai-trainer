# AI Trainer — product spec

The requirements behind each build phase. `CLAUDE.md` holds the rules that apply to every change; this file describes *what* to build. Read the relevant sections before starting a phase or a feature in that area.

## Build phases

Finish each phase in a working, runnable state (builds, tests pass, app launches) before starting the next. At the end of each phase, summarize what was done and what I should check manually.

1. **Foundation** (done): add Koin, KSP, Room, Navigation, Ktor, serialization and kotlinx-datetime to the existing project; `Application` class starting Koin; entities, DAOs, seed exercise library; app shell with bottom navigation; workout logging screen; disable backups; Koin module verification test.
2. **Settings + secrets** (done): `SecretStore` (DataStore + Keystore AES-GCM, with a unit/instrumented test for round-trip encryption), settings screen, profile/preferences, debug screen with seed demo data.
3. **Health Connect + progress**: permissions, sync worker, manual weight entry, progress screen with charts.
4. **Polar**: OAuth flow, user registration, daily activity sync, body weight sync.
5. **Claude**: domain stats, prompt builder, client (real + fake), plan generation, plan screen, "adjust today".
6. **Polish**: weekly review worker, DB export/import, error states, empty states.

## Screens (MVP)

1. **Log workout**: start a session (from the plan or ad hoc), add exercises, log sets quickly ("same as last time", +/- buttons), finish.
2. **Plan**: current week, today's session highlighted, daily kcal/protein target, "Adjust today" button.
3. **Progress**: weight chart (raw + 7-day average), daily/weekly calorie balance bars, weekly sets per muscle group.
4. **Settings**: goal and preferences, weak muscle groups, equipment, exercise preferences, Claude API key and model, Polar connect/disconnect, Health Connect permissions, DB export/import.
5. **Debug** (debug builds only): seed demo data, switch between real and fake Claude/Polar clients.

## Data model (Room)

The original design. The entities in `data/db/` are the source of truth once a table exists.

- `exercise` — id, name, primaryMuscles, secondaryMuscles, equipment, preference (FAVOURITE / NEUTRAL / AVOID)
- `workout_session` — id, date, startedAt, finishedAt, planSessionRef (nullable), notes
- `set_entry` — id, sessionId, exerciseId, positionInSession, reps, weightKg, rpe (nullable), isWarmup
- `daily_energy` — date (PK), kcalBurned, kcalBurnedSource, kcalEaten, proteinG, carbsG, fatG, updatedAt
- `body_weight` — id, measuredAt, weightKg, source (POLAR / MANUAL), unique on (measuredAt, source) so repeated syncs don't duplicate entries
- `profile_prefs` — goal (CUT / MAINTAIN / BULK / RECOMP), sessionsPerWeek, sessionLengthMin, weakMuscleGroups, availableEquipment, free-text notes
- `plan` — id, version, createdAt, isActive, validFrom, validTo, json (the validated plan), rationale

## Domain calculations

Local, deterministic and unit-tested, computed in `domain/`. Claude should not do the arithmetic.

- Weekly sets per muscle group (primary = 1, secondary = 0.5)
- Adherence: planned vs completed sessions
- Weight trend: 7-day moving average and kg/week slope
- Average daily intake, average burn, average deficit/surplus
- **Empirical TDEE** from the weight trend vs logged intake over 2–4 weeks (wearable calorie burn is imprecise; prefer this when enough data exists)
- Per-exercise progression info (best set, estimated 1RM, last session's load)

## Data sync

- **Health Connect** (only when the app is opened or brought to the foreground, plus a manual "Sync now"; no background sync): read nutrition (kcal, macros) and **total** calories burned per day (`aggregateGroupByPeriod`, last 30 days). Active calories alone are not used: they exclude resting metabolism, so they'd make every day look like a surplus. Body weight comes from Polar, not Health Connect. Request permissions with a clear UI. Health Connect is part of the OS on API 34+, but still check `HealthConnectClient.getSdkStatus()` and handle "unavailable" / "update required" gracefully. Read-only: the app never writes to Health Connect.
- **Polar AccessLink**: OAuth 2.0 authorization code flow via Custom Tabs, with a redirect back into the app (custom scheme, e.g. `aitrainer://polar-callback`; verify Polar accepts it, otherwise propose an alternative). Exchange the code for a token, register the user with AccessLink once, then pull daily activity / calories daily. **Verify endpoints and flows against the official AccessLink v3 documentation** rather than assuming them.
- **Body weight from Polar**: read it from AccessLink's physical information data (weight is part of the user's physical settings in Polar Flow). Before building this, check in the docs how weight history is exposed (a new entry per change vs only the current value) and report back; if only the current value is available, store a new `body_weight` row whenever the value changes.
- Keep **manual weight entry** in the app as a fallback (source MANUAL).
- Merge rules (Polar wins, Health Connect replaces its own data): see "Data invariants" in `CLAUDE.md`.

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

## Fakes and demo data

- `FakeClaudeClient` (returns a canned valid plan), `FakePolarClient` and a fake health data source, swappable via Koin modules so I can develop the UI on the emulator offline and without API costs.
- **Seed demo data** (debug-only button, `DemoDataSeeder`): deterministic ~6 weeks of realistic workouts, weight and calorie data to exercise the charts and plan generation.
