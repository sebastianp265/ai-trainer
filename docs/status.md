# Project status

Last updated: 2026-11-10 · Branch `master` · Phase 4 of 7 (Health Connect data and Progress), not started.
Phase 3 (design) is done: [`requirements.md`](requirements.md), [`design/`](design/README.md), [`plan.md`](plan.md).

**Can I run it? Yes.** The debug build compiles with no errors. The app launches into a working
workout log and settings, plus a Progress tab that lists the data read from Health Connect. Calories burned
and body weight aren't read yet, and the AI parts don't exist yet.

## How to run

Devices and commands: [`CLAUDE.md`, Build and verify](../CLAUDE.md#build-and-verify). After the
split into Gradle modules, sync Gradle first; if the old `app` run configuration is still listed in
Android Studio, delete it.

## What to expect, screen by screen

The app opens on **Log**. The bottom bar has four tabs.

| Tab          | State                   | What you can do                                                                                                                                                                                                                                                  |
|--------------|-------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Log**      | ✅ Works                | Start a workout, add exercises (searchable picker), log sets with +/- steppers, "Same as last time", mark warm-up sets, delete sets, finish or discard. Recent workouts are listed below.                                                                        |
| **Plan**     | ⏳ Placeholder          | Shows "Your AI-generated training plan will appear here (phase 5)". Nothing to do yet.                                                                                                                                                                           |
| **Progress** | 🧪 Data check (phase 3) | Health Connect card until it's connected (connect, or update Health Connect). Once connected, a plain list of the last 30 days read from Health Connect: calories eaten, protein, carbs, fat, and the source apps. No charts or stats yet (designed in phase 3). |
| **Settings** | ✅ Works                | Claude API key (stored encrypted) and model. The training profile (goal, volume, weak muscle groups, equipment, notes) and exercise preferences were removed for now and will come back later.                                                                   |

### Health Connect

- Tap **Connect** on the Progress tab and grant the permissions. The app reads calories eaten and
  macros for the last 30 days. Calories burned and body weight
  aren't read yet. It never writes to Health Connect.
- Nothing is stored: the list is read from Health Connect each time you open the Progress tab or
  come back to it.
- Under each day, a small line shows which apps the numbers come from ("From: …", as package names).
  "–" means no app is listed as a source.
- To check a number you don't recognize: Android **Settings → Health Connect → Data and access** (on
  some phones under *Security & privacy → Privacy → Health Connect*), pick **Nutrition**, open the
  day, and look at each entry and the app that wrote it. You can delete entries there too.
- Once connected, the Health Connect card disappears from the Progress tab. It comes back if you
  revoke the permissions in system settings. The app has no "how your data is used" page: Health
  Connect requires an entry point for it, so the app declares one that closes immediately when
  opened from Health Connect's settings.
- **Please verify on your phone** that the numbers match Health Connect and the app you log meals in,
  and that a meal removed there disappears after reopening the tab.
- On a fresh emulator Health Connect has no data, so the list says there's no data until you add
  entries in the Health Connect app.

### Database

- Installing a new build keeps your logged workouts: every schema change comes with a migration.
  This starts with the current schema (version 6). If the app on your phone has an older one, it
  crashes on start; uninstall it once and install again.
- Exercise names come from the database, so the picker should show Polish names on a Polish phone.

## What's not there yet

The tasks behind each row are in [`plan.md`](plan.md).

| Feature                                                                                              | Phase |
|------------------------------------------------------------------------------------------------------|-------|
| Settings behind the gear icon; tabs Trening · Plan · Postęp                                          | 4     |
| Stored Health Connect data: nutrition, kcal burned, weight, steps, sleep, resting HR, HRV, workouts  | 4     |
| Health Connect screen in Settings: state per data type, sync now                                     | 4     |
| Progress charts, metric detail, day detail; list of all workouts including those from other apps     | 4     |
| RPE per set, the new set row, the "last time" label, discard confirmation                            | 5     |
| Summary after a workout with records; exercise history; edit and delete a finished workout           | 5     |
| Planned workouts written by hand; start a workout from a plan                                        | 5     |
| Weekly sets per muscle group with targets, in Progress and Plan                                      | 5     |
| Coach: chat, tools, proposals for plans and targets, memory, API key and model screens               | 6     |
| Debug screen (debug builds only)                                                                     | 6     |
| Weekly review, export and import, loading states, polished empty and error states                    | 7     |

## Known gaps

- After the module split, the instrumented tests (app flows and `KeystoreSecretStoreITest`) compile
  but haven't been run yet.
