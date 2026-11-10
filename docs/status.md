# Project status

Last updated: 2026-11-10 · Branch `phase-5-7` · Phase 5 of 7 (training, plans and weekly sets, without the coach) is done.
Phases 1–4 are done and merged. The design (phase 3) is in [`requirements.md`](requirements.md), [`design/`](design/README.md) and [`plan.md`](plan.md).
Choices made without asking are in [`decisions.md`](decisions.md).

**Can I run it? Yes.** The debug build compiles with no errors. The app starts on the new Training screen and has working
plans, workouts with RPE, a summary after each workout, exercise history, weekly sets per muscle group with targets, the
Progress tab with Health Connect data and the Settings screen. The AI coach does not exist yet (phase 6); the app behaves as
with the coach switched off.

## How to run

Devices and commands: [`CLAUDE.md`, Build and verify](../CLAUDE.md#build-and-verify). After a pull, sync Gradle first.
The emulator needs API 34+ with a *Google Play* image.

## What to expect, screen by screen

The app opens on **Training**. The bottom bar has three tabs: Training · Plan · Progress. Every tab has a gear icon in the top
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
| **Settings** | ⏳ Partly | Coach switch (off at first). With it on you can enter the Claude API key and model; they are stored but not used yet. Health Connect row. The rows "Data backup" and "Debug" come with phases 7 and 6. |
| **Health Connect** | ✅ Works | Unchanged from phase 4. |

### Things to check by hand on the phone

- Write a plan with a warm-up set and a date, start it from Training, change a weight, remove a set, finish. Check the summary
  ("Compared to plan") and the finished workout.
- The "Last time" line with several groups (it should break only at " · " and show "+N" when it does not fit in two lines).
- A second workout with a heavier set shows a record in the summary.
- Weekly sets: after a workout the Plan tab and the Progress card move; bench press counts 1 for chest and 0.5 for arms and shoulders.
- Polish texts of all new screens (they were written from the mockups and have not been read on a phone).
- The workout screen after closing and reopening the app in the middle of a workout.

### Database

Installing a new build keeps your data: schema 7 → 8 (six muscle groups, "Other" equipment), 8 → 9 (plans, workout ↔ plan link),
9 → 10 (weekly targets). If the app on your phone has a schema older than 6, uninstall it once.

## What's not there yet

| Feature | Phase |
|---|---|
| Coach: chat, tools, proposals for plans and targets, memory, API key and model screens | 6 |
| Debug screen (debug builds only) | 6 |
| Weekly review, export and import, loading states, polished empty and error states | 7 |

## Known gaps

- The Polish texts of the new screens were written from the mockups and have not been read on a phone.
- Only the strings of Health Connect workout types listed in `ExternalWorkoutText.kt` have their own names; other types show "Other activity".
- A first sync made without the "past data" permission is not extended to a year when the permission is granted later.
- Sync errors are not shown to the user.
- The IDE shows unresolved references in `jvmTest` files; Gradle compiles them (see `decisions.md`).
