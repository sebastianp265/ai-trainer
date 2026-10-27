# Project status

Last updated: 2026-10-26 · Branch `phase-3-health-progress` · Phase 3 of 6 (Health Connect + progress), work in progress and not committed yet.

**Can I run it? Yes.** The debug build compiles with no errors. The app launches into a working workout log, settings and progress screens. The AI parts (plan generation) and Polar don't exist yet.

## How to run

**Android Studio (easiest)**

1. Pick a device in the toolbar: an emulator (API 34+, *Google Play* image, so Health Connect is built in) or your phone via USB / wireless debugging.
2. Select the `app` run configuration and press Run (▶).

**Command line**

```
gradlew.bat installDebug      # build and install on the connected device
gradlew.bat test              # unit tests
gradlew.bat connectedDebugAndroidTest   # UI tests: emulator only, they uninstall the app and delete its data
```

Tip: to see the charts right away, open **Settings → Debug tools → Replace data with demo data**. This **replaces** all workouts, weight and calorie data with 6 weeks of generated data, so don't do it on your phone if you have real data.

## What to expect, screen by screen

The app opens on **Log**. The bottom bar has four tabs.

| Tab | State | What you can do |
|---|---|---|
| **Log** | ✅ Works | Start a workout, add exercises (searchable picker), log sets with +/- steppers, "Same as last time", mark warm-up sets, delete sets, finish or discard. Recent workouts are listed below. |
| **Plan** | ⏳ Placeholder | Shows "Your AI-generated training plan will appear here (phase 5)". Nothing to do yet. |
| **Progress** | ✅ Works (phase 3, new) | Health Connect card (connect / sync now / how data is used), body weight (latest, 7-day average, kg/week trend, chart, manual entry, delete), calorie balance (7-day averages, daily/weekly bar charts), sets per muscle group for the last 7 days. |
| **Settings** | ✅ Works | Goal, sessions per week, session length, weak muscle groups, equipment, notes, exercise preferences (favourite / neutral / avoid), Claude API key (stored encrypted) and model. In debug builds: **Debug tools**. |

### Health Connect

- Tap **Connect** on the Progress tab and grant the permissions. The app reads intake/macros and total calories burned for the last 30 days. It never writes to Health Connect.
- Sync runs only when you open the app (or come back to it) and when you tap **Sync now**. There's no background sync.
- Entries deleted in Health Connect (e.g. a meal removed in Fitatu) should disappear after the next sync. This fix is in the code but hasn't been checked on a device yet. **Please verify it manually.**
- On a fresh emulator Health Connect has no data, so the calorie section stays empty unless you use demo data or add entries in the Health Connect app.

## What's not there yet

| Feature | Phase |
|---|---|
| Polar connection (OAuth, daily activity, body weight sync) | 4 |
| Claude: plan generation, Plan screen, "Adjust today" | 5 |
| The saved Claude API key and model aren't used by anything yet | 5 |
| Switching between real and fake Claude/Polar clients in Debug tools | 4–5 |
| Weekly review, DB export/import, polished error/empty states | 6 |

Body weight currently comes only from manual entry or demo data (Polar sync is phase 4).

## Known gaps

- Phase 3 changes are **uncommitted** on the branch.
- This status was checked by building the project and reading the code, not by running the test suites or launching the app. Run the tests before merging.
