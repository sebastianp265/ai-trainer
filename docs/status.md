# Project status

Last updated: 2026-11-10 · Branch `phase-4` · Phase 4 of 7 (Health Connect data and Progress) is done.
Phase 3 (design) is done: [`requirements.md`](requirements.md), [`design/`](design/README.md), [`plan.md`](plan.md).
Choices made without asking are in [`decisions.md`](decisions.md).

**Can I run it? Yes.** The debug build compiles with no errors. The app launches into a working
workout log, a Progress tab with cards and charts from Health Connect data, a Settings screen
behind the gear icon, and the Health Connect screen. The Plan tab is still a placeholder and the AI
parts don't exist yet.

## How to run

Devices and commands: [`CLAUDE.md`, Build and verify](../CLAUDE.md#build-and-verify). After the
split into Gradle modules, sync Gradle first; if the old `app` run configuration is still listed in
Android Studio, delete it.

## What to expect, screen by screen

The app opens on **Training**. The bottom bar has three tabs: Training · Plan · Progress. Every tab
has a gear icon in the top bar that opens **Settings**; Settings and the screens opened from it have
a back arrow and no bottom bar.

| Screen | State | What you can do |
|---|---|---|
| **Training** | ✅ Works (old design) | Start a workout, add exercises (searchable picker), log sets with +/- steppers, "Same as last time", mark warm-up sets, delete sets, finish or discard. Recent workouts are listed below; "All" opens the list of all workouts. The new Training screen with plans comes in phase 5. |
| **Plan** | ⏳ Placeholder | Shows a placeholder text. Nothing to do yet. |
| **Progress** | ✅ Works | Range switch (4 weeks, 3 months, year) and cards: weight (daily points and 7-day average), calorie balance (weekly bars), macros, steps, sleep, resting heart rate, HRV, strength (estimated 1RM), workouts, days. A card is hidden when its data is missing. "Details" opens the metric (chart, lowest, highest, change per week, list of values); a day opens everything stored for that date. Without Health Connect an invitation card appears and the strength and workouts cards remain. |
| **All workouts** | ✅ Works | Gym workouts and workouts from other apps by month, filter All / Gym / From other apps. A workout from another app opens read-only. Gym workouts are not tappable yet (phase 5). |
| **Settings** | ✅ Works | Claude API key (stored encrypted) and model, and the Health Connect row ("X of 8 data types · synced …"). |
| **Health Connect** | ✅ Works | State per data type, history permission, last sync time, "Sync now", a button to change permissions in Health Connect. When not connected: "Connect"; when missing or outdated: install or update. |

### Health Connect

- Data is copied into the app (Room) and stays there even when it disappears from Health Connect.
  Types: nutrition, calories burned, weight, steps, sleep, resting heart rate, HRV and workouts from
  other apps. Each type has its own permission and is optional. The app never writes to Health Connect.
- Sync runs when the app comes to the foreground and from "Sync now". The first sync reads one year
  back when the "past data" permission is granted, otherwise 30 days. Every later sync reads the last
  14 days again and overwrites them, so edits and deletions in the source apps show up.
- Progress averages leave out today for calorie balance, macros and steps (the day is not over).
- **Please verify on your phone:** connect and allow some types but not all; check that Progress shows
  only the cards for allowed types, that weight, sleep, steps and calories match Health Connect, and
  that a meal removed in Health Connect disappears within the last 14 days after reopening the app.
- **Please verify:** a workout recorded by another app shows in All workouts with the right type,
  duration and, when Health Connect has them, distance, calories and average heart rate.
- On a fresh emulator Health Connect has no data; add entries in the Health Connect app to see cards.
- Health Connect requires an entry point for "how your data is used"; the app declares one that
  closes immediately when opened from Health Connect's settings.

### Database

- Installing a new build keeps your logged workouts: every schema change comes with a migration.
  Phase 4 moved the schema from version 6 to 7 (tables `health_day` and `health_workout`). If the app
  on your phone has an older schema than 6, it crashes on start; uninstall it once and install again.
- Exercise names come from the database, so the picker should show Polish names on a Polish phone.

## What's not there yet

The tasks behind each row are in [`plan.md`](plan.md).

| Feature | Phase |
|---|---|
| RPE per set, the new set row, the "last time" label, discard confirmation | 5 |
| Summary after a workout with records; exercise history; edit and delete a finished workout | 5 |
| Planned workouts written by hand; start a workout from a plan; the new Training screen | 5 |
| Weekly sets per muscle group with targets, in Progress and Plan | 5 |
| Coach: chat, tools, proposals for plans and targets, memory, API key and model screens | 6 |
| Debug screen (debug builds only) | 6 |
| Weekly review, export and import, loading states, polished empty and error states | 7 |

## Known gaps

- The Polish texts of the new screens were written from the mockups and have not been read on a phone.
- Only the strings of Health Connect workout types listed in `ExternalWorkoutText.kt` have their own
  names; other types show "Other activity".
- A first sync made without the "past data" permission is not extended to a year when the permission
  is granted later.
- Sync errors (for example a permission revoked during a sync) are not shown to the user.
