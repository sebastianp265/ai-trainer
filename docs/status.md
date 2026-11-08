# Project status

Last updated: 2026-11-08 · Branch `master` · Phase 3 of 7 (Health Connect integration), in progress.

**Can I run it? Yes.** The debug build compiles with no errors. The app launches into a working workout log and settings, plus a Progress tab that lists the data read from Health Connect. Polar and the AI parts don't exist yet.

## How to run

Devices and commands: [`CLAUDE.md`, Build and verify](../CLAUDE.md#build-and-verify). After the split into Gradle modules, sync Gradle first; if the old `app` run configuration is still listed in Android Studio, delete it.

## What to expect, screen by screen

The app opens on **Log**. The bottom bar has four tabs.

| Tab | State | What you can do |
|---|---|---|
| **Log** | ✅ Works | Start a workout, add exercises (searchable picker), log sets with +/- steppers, "Same as last time", mark warm-up sets, delete sets, finish or discard. Recent workouts are listed below. |
| **Plan** | ⏳ Placeholder | Shows "Your AI-generated training plan will appear here (phase 5)". Nothing to do yet. |
| **Progress** | 🧪 Data check (phase 3) | Health Connect card until it's connected (connect, or update Health Connect). Once connected, a plain list of the last 30 days read from Health Connect: calories eaten, protein, carbs, fat, and the source apps. No charts or stats yet (designed in phase 6). |
| **Settings** | ✅ Works | Claude API key (stored encrypted) and model. The training profile (goal, volume, weak muscle groups, equipment, notes) and exercise preferences were removed for now and will come back later. |

### Health Connect

- Tap **Connect** on the Progress tab and grant the permissions. The app reads calories eaten and macros for the last 30 days. Calories burned are not read from Health Connect (they will come from Polar). It never writes to Health Connect.
- Nothing is stored: the list is read from Health Connect each time you open the Progress tab or come back to it.
- Under each day, a small line shows which apps the numbers come from ("From: …", as package names, e.g. `com.fitatu.tracker`). "–" means no app is listed as a source.
- To check a number you don't recognize: Android **Settings → Health Connect → Data and access** (on some phones under *Security & privacy → Privacy → Health Connect*), pick **Nutrition**, open the day, and look at each entry and the app that wrote it. You can delete entries there too.
- Once connected, the Health Connect card disappears from the Progress tab. It comes back if you revoke the permissions in system settings. The app has no "how your data is used" page: Health Connect requires an entry point for it, so the app declares one that closes immediately when opened from Health Connect's settings.
- **Please verify on your phone** that the numbers match Fitatu / Health Connect, and that a meal removed in Fitatu disappears after reopening the tab.
- On a fresh emulator Health Connect has no data, so the list says there's no data until you add entries in the Health Connect app.

### Prototype database

- This version changes the schema (exercises, muscle groups and equipment are database tables), so installing it over the previous one **deletes your logged workouts** once ([`prototype-database.md`](prototype-database.md)).
- Exercise names come from the database, so the picker should show Polish names on a Polish phone.

## What's not there yet

| Feature | Phase |
|---|---|
| Polar connection (OAuth, daily activity, body weight) | 4 |
| Claude client; the saved API key and model aren't used by anything yet | 5 |
| Debug screen (debug builds only) to switch between real and fake Health Connect / Polar / Claude | 4–5 |
| Body weight, calorie stats, charts, plan generation, Plan screen, "Adjust today" | 6 (design) |
| Weekly review, DB export/import, polished error/empty states, real DB migrations | 7 |

## Known gaps

- After the module split, the instrumented tests (app flows and `KeystoreSecretStoreITest`) compile but haven't been run yet.
