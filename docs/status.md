# Project status

Last updated: 2026-11-10 · Branch `phase-5-7` · Phases 5, 6 and 7 are done (training, plans and weekly sets; the coach; weekly review, backup and polish).
Phases 1–4 are done and merged. The design (phase 3) is in [`requirements.md`](requirements.md), [`design/`](design/README.md) and [`plan.md`](plan.md).
Choices made without asking are in [`decisions.md`](decisions.md).

**Can I run it? Yes.** The debug build compiles with no errors. The app starts on the new Training screen and has working
plans, workouts with RPE, a summary after each workout, exercise history, weekly sets per muscle group with targets, the
Progress tab with Health Connect data and the Settings screen. The Coach tab, weekly review and backup work. The coach needs your Claude API key (Settings); without it the app behaves as with the coach off.

## How to run

Devices and commands: [`CLAUDE.md`, Build and verify](../CLAUDE.md#build-and-verify). After a pull, sync Gradle first.
The emulator needs API 34+ with a *Google Play* image.

## What to expect, screen by screen

The app opens on **Training**. The bottom bar has four tabs: Training · Plan · Coach · Progress; with the coach switched off
in Settings the Coach tab and every coach element disappear. Every tab has a gear icon in the top
bar that opens **Settings**. While a workout runs, the workout screen covers everything (no bottom bar, no top bar); if you close
the app during a workout, it opens on the workout again.

| Screen | State | What you can do |
|---|---|---|
| **Training** | ✅ Works | Shows the next plan with "Start" and "Other workout" (a sheet with every plan and "Empty workout"), or "Start on your own" when you have no plans. Below: recent workouts (tap one to see it) and "All". |
| **Workout in progress** | ✅ Works | Exercise cards with a "Last time" line (tap for the exercise history). Per set: tap the number to mark a warm-up, tap the weight or the reps to type, − / + for reps, tap RPE to pick 6 to 10, delete. "+ Set", "Same as last time", remove an exercise, add an exercise. "Discard" asks for confirmation; "Finish" saves and opens the summary. |
| **Summary** | ✅ Works | Time, working sets, average RPE; new records; comparison with the plan ("on plan", "−1 set", "+2.5 kg", "+1 reps"); the previous workout of the same plan; "See all sets". |
| **Finished workout** | ✅ Works | All sets with RPE and, for a planned workout, the plan line and the difference. Edit (re-opens it as a running workout) and delete (with confirmation). |
| **Exercise picker** | ✅ Works | Search, muscle group chips, recently used (with the last time), all exercises, "New exercise" (name, main group, additional groups). |
| **Exercise history** | ✅ Works | Estimated 1RM, best set, change over 3 months, 1RM chart, average RPE chart, past sessions (tap to open the workout). Opens from the "Last time" line and from the strength card in Progress. |
| **Plan** | ✅ Works | This week (Monday to Sunday) with done, planned and other-app markers; sets this week against the targets (done + planned); upcoming plans by date, then plans without a date. "New plan". |
| **Plan details / edit / new** | ✅ Works | Details with warm-ups and grouped sets; "Start workout"; edit (name, optional date, exercises, weight, reps, RPE, warm-up flag); delete with confirmation. |
| **Progress** | ✅ Works | As before, plus the card "Sets per muscle group · weekly average" with the target band, legend, an info box and a link to the targets. |
| **Weekly set targets** | ✅ Works | Ranges per muscle group (default 10–20), source and date, "Show targets" switch, "Restore defaults". |
| **All workouts** | ✅ Works | Gym workouts (tap to open) and workouts from other apps. |
| **Coach** | ✅ Works, not tried with a real key | Chat with the coach: starter suggestions in the first conversation, a "Checked: …" line under an answer, "The coach is checking/writing" while it works. Proposal cards (new plan, plan change, several plans, target change) with "Accept", "Preview" and, for several plans, "Accept all". Without a key the tab leads to the key screen; no network, a rejected key and no answer each show their own message with "Try again". |
| **Proposal preview** | ✅ Works | The proposed plan with the changes marked (added, removed, sets, weight); "Accept" or "Reply to the coach". A pending proposal also shows as a card on Training, Plan and the targets screen. |
| **Weekly review** | ✅ Works, not tried with a real key | When the app comes to the foreground after a finished Monday-to-Sunday week with data, the coach posts a card with the week's numbers and its review in the chat. |
| **What the coach knows about me** | ✅ Works | Profile (goal, time, equipment, limitations, preferences), weekly set targets, coach notes; everything can be edited or deleted. |
| **Settings** | ✅ Works | Coach switch (on at first). With it on: Claude API key, model, coach memory. Health Connect row. "Data backup". |
| **API key** | ✅ Works, not tried with a real key | Paste a key, "Save and check" (one call that spends no tokens), remove the key. |
| **Coach model** | ✅ Works | Opus, Sonnet or Haiku; messages and tokens used this month, counted on the phone. |
| **Data backup** | ✅ Works, not tried between two devices | Export everything except the API key to one file. Import shows what the file contains, asks, replaces all data and restarts the app. |
| **Health Connect** | ✅ Works | Unchanged from phase 4. |

### Things to check by hand on the phone

- Write a plan with a warm-up set and a date, start it from Training, change a weight, remove a set, finish. Check the summary
  ("Compared to plan") and the finished workout.
- The "Last time" line with several groups (it should break only at " · " and show "+N" when it does not fit in two lines).
- A second workout with a heavier set shows a record in the summary.
- Weekly sets: after a workout the Plan tab and the Progress card move; bench press counts 1 for chest and 0.5 for arms and shoulders.
- Polish texts of all new screens (they were written from the mockups and have not been read on a phone).
- The workout screen after closing and reopening the app in the middle of a workout.
- With your own key: talk to the coach, accept a plan and a target change, and check the plan on the Plan tab and the targets.
- Open the app after a full week of data and read the weekly review.
- Export a backup, import it (best on a second device), and check that the data is back. The API key is not in the file.
- Switch the coach off and check that the Coach tab and the coach shortcuts are gone.
- Connect Health Connect from Progress: the system permission screen opens, and after allowing, the "Connect" card is gone (no automated test taps that system screen).

### Database

Installing a new build keeps your data: schema 7 → 8 (six muscle groups, "Other" equipment), 8 → 9 (plans, workout ↔ plan link),
9 → 10 (weekly targets), 10 → 11 (coach conversation, proposals, notes, profile, usage, request log), 11 → 12 (weekly review
stored with a coach message), 12 → 13 (the request log of the removed debug screen is dropped). If the app on your phone has a schema older than 6, uninstall it once.

## What's not there yet

Nothing planned is missing. See the known gaps below.

## Known gaps

- The Polish texts of the new screens were written from the mockups and have not been read on a phone.
- Only the strings of Health Connect workout types listed in `ExternalWorkoutText.kt` have their own names; other types show "Other activity".
- A first sync made without the "past data" permission is not extended to a year when the permission is granted later.
- Sync errors show one line on the Health Connect screen only.
- Not checked on a phone with a real key: the real Claude calls, the weekly review text, and backup import with a file from another device (the app restarts after an import).
- The final design audit of every screen against the mockups was done only for the empty and error states listed in `decisions.md`; small visual differences may remain.
- The IDE shows unresolved references in `jvmTest` files; Gradle compiles them (see `decisions.md`).
- The end-to-end tests depend on the emulator. With 2 GB of RAM the emulator showed "System UI isn't responding" and tests failed because of that window; give it 4 GB.

## Final summary

All tasks of phases 5, 6 and 7 are ticked. `./gradlew.bat build` passed at the end of phase 7 (unit tests, architecture rules, lint, release build). On 2026-11-10 the full instrumented run passed on an emulator with Android 17: 58 of 58 end-to-end tests and 11 of 11 integration tests of the Android core.
After that the debug mode was removed (schema 13, 57 end-to-end tests left). The unit tests and the 11 integration tests pass, and all 57 end-to-end tests pass on two emulators with 4 GB of RAM: Android 15 (5 min 50 s) and Android 16 (6 min 22 s). An earlier run on the Android 17 emulator was cut off when its system crashed.
Then the tests were reworked so that each path is tested once: end-to-end tests get their data (API key, plans, workouts) through the app's services instead of tapping it in, and never tap the system Health Connect screen (50 end-to-end tests, 10 integration tests). All of them pass on the Android 16 emulator.
Check by hand: the list under "Things to check by hand on the phone".
