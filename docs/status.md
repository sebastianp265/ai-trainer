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

**Look.** Every screen uses the app's green palette (light and dark, no wallpaper colors),
line icons, filled cards with small gray labels, grouped settings rows, colored tags (record, from plan, Health Connect)
and green/orange text for good and off-target values. Plans written by the coach show the coach's note on the plan details.

The app opens on **Training**. The bottom bar has four tabs: Training · Coach · Progress · Settings; with the coach switched off
in Settings the Coach tab and every coach element disappear. The tabs have no top bar; screens opened from them have one with the back arrow. While a workout runs, the workout screen covers everything (no bottom bar, no top bar); if you close
the app during a workout, it opens on the workout again.

| Screen | State | What you can do |
|---|---|---|
| **Training** | ✅ Works | From the top: the week strip (done, planned), a pending coach proposal, "Start workout" and recent workouts with "All". "Start workout" opens a sheet with every plan (the next one first; tap to start, the info icon opens the plan), "Empty workout" and "New plan". |
| **Workout in progress** | ✅ Works | Exercise cards with a "Last time" line (tap for the exercise history). Per set: tap the number to mark a warm-up, tap the weight or the reps to type, − / + for reps, tap RPE to pick 6 to 10, delete. "+ Set", "Same as last time", the microphone (say "80 na 8, RPE 8" and the set is added), remove an exercise, add an exercise. The coach button next to the title opens the coach about this workout. "Discard" asks for confirmation; "Finish" saves and opens the summary. |
| **Summary** | ✅ Works | Time, working sets, average RPE; new records; comparison with the plan ("on plan", "−1 set", "+2.5 kg", "+1 reps"); the previous workout of the same plan; "See all sets". |
| **Finished workout** | ✅ Works | All sets with RPE and, for a planned workout, the plan line and the difference. Edit (re-opens it as a running workout) and delete (with confirmation). |
| **Exercise picker** | ✅ Works | Search, muscle group chips, recently used (with the last time), all exercises, "New exercise" (name, main group, additional groups). |
| **Exercise history** | ✅ Works | Estimated 1RM, best set, change over 3 months, 1RM chart, average RPE chart, past sessions (tap to open the workout). Opens from the "Last time" line and from the strength card in Progress. |
| **Plan details / edit / new** | ✅ Works | Details with warm-ups and grouped sets; "Start workout"; edit (name, optional date, exercises, weight, reps, RPE, warm-up flag); delete with confirmation. |
| **Progress** | ✅ Works | As before, plus the card "Sets per muscle group · weekly average" with the target band, legend, an info box and a link to the targets. An accepted nutrition target shows under the calorie balance and the macros. |
| **Weekly set targets** | ✅ Works | Ranges per muscle group (default 10–20), source and date, "Show targets" switch, "Restore defaults". |
| **All workouts** | ✅ Works | Gym workouts by month; tap one to open it. |
| **Coach** | ✅ Works, not tried with a real key | Chat with the coach: starter suggestions in the first conversation, a "Checked: …" line under an answer, "The coach is checking/writing" while it works. Proposal cards (new plan, plan change, several plans or a block of weeks, target change, change of the sets of a running or finished workout, new exercise, daily calorie and protein target) with "Accept", "Preview" for plans and, for several plans, "Accept all". An accepted card has "Undo" while nothing used it since. Opened from a plan, workout, exercise or activity, the chat shows that topic above the input ("About plan: Push A") and sends it with the next message; opened from a running workout it also offers quick questions. Without a key the tab leads to the key screen; no network, a rejected key and no answer each show their own message with "Try again". |
| **Proposal preview** | ✅ Works | The proposed plan with the changes marked (added, removed, sets, weight); "Accept" or "Reply to the coach". A pending proposal also shows as a card on Training and the targets screen. |
| **Weekly review** | ✅ Works, not tried with a real key | When the app comes to the foreground after a finished Monday-to-Sunday week with data, the coach posts a card with the week's numbers and its review in the chat. |
| **What the coach knows about me** | ✅ Works | Profile (goal, time, equipment, limitations, preferences), weekly set targets, coach notes; everything can be edited or deleted. |
| **Settings** | ✅ Works | Coach switch (on at first). With it on: Claude API key, model, coach memory. Section "What your coach can access" (with the coach off: "Health Connect data") with a switch per Health Connect data type and "Data older than 30 days" with "Allow". "Data backup". |
| **API key** | ✅ Works, not tried with a real key | Paste a key, "Save and check" (one call that spends no tokens), remove the key. |
| **Coach model** | ✅ Works | Opus, Sonnet or Haiku; messages and tokens used this month, counted on the phone. |
| **Data backup** | ✅ Works, not tried between two devices | Export everything except the API key to one file. Import shows what the file contains, asks, replaces all data and restarts the app. |
| **Health Connect** | ✅ Works | Nothing is copied into the app: Progress, the day detail and the coach read Health Connect when they open and again whenever the app comes back to the foreground. No separate screen and no sync buttons; everything is in the Settings section. Switching on asks Health Connect for that one item; switching off hides that data from the app and the coach (the permission in Health Connect stays; "Change permissions in Health Connect" opens it). "Allow" next to "Data older than 30 days" asks for access to past data, with no time limit. Workouts recorded by other apps are not read. The Progress card without data opens Settings. |

### Things to check by hand on the phone

- Write a plan with a warm-up set and a date, start it from Training, change a weight, remove a set, finish. Check the summary
  ("Compared to plan") and the finished workout.
- The "Last time" line with several groups (it should break only at " · " and show "+N" when it does not fit in two lines).
- A second workout with a heavier set shows a record in the summary.
- Weekly sets: after a workout the Progress card moves; bench press counts 1 for chest and 0.5 for arms and shoulders.
- Polish texts of all new screens (they have not been read on a phone).
- The workout screen after closing and reopening the app in the middle of a workout.
- With your own key: talk to the coach, accept a plan and a target change, and check the plan on Training and the targets.
- Open the app after a full week of data and read the weekly review.
- Export a backup, import it (best on a second device), and check that the data is back. The API key is not in the file.
- With your own key: during a workout tap the coach button, ask "Za ciężko", accept the change and check the sets; then tap "Cofnij" on the card.
- Ask the coach about a finished workout with a typo (e.g. 800 kg) and accept the correction.
- Ask the coach for a calorie and protein target and check Progress; ask for a new exercise and find it in the exercise picker.
- Dictate a set with the microphone during a workout ("80 na 8, RPE 8"), in Polish and English.
- Look through every screen in light and dark mode.
- Switch the coach off and check that the Coach tab and the coach shortcuts are gone.
- Switch on a Health Connect type in Settings that has no permission yet: the system permission screen opens for that type only, and after allowing, the switch is on and its data appears in Progress (no automated test taps that system screen).

### Database

Installing a new build keeps your data: schema 7 → 8 (six muscle groups, "Other" equipment), 8 → 9 (plans, workout ↔ plan link),
9 → 10 (weekly targets), 10 → 11 (coach conversation, proposals, notes, profile, usage, request log), 11 → 12 (weekly review
stored with a coach message), 12 → 13 (the request log of the removed debug screen is dropped), 13 → 14 (the coach's note on a plan), 14 → 15 (undo data of proposals, the topic of a message, the nutrition target). If the app on your phone has a schema older than 6, uninstall it once.

## What's not there yet

Nothing planned is missing. See the known gaps below.

## Known gaps

- The Polish texts of the new screens have not been read on a phone.
- When Health Connect cannot be read (for example its read limit is reached), the screens show no Health Connect data instead of an error.
- Voice entry is tested only through its parser; the system speech screen is never tapped by the tests.
- Not checked on a phone with a real key: the real Claude calls, the weekly review text, and backup import with a file from another device (the app restarts after an import).
- The launcher icon and splash screen are still the Android defaults.
- The IDE shows unresolved references in `jvmTest` files; Gradle compiles them (see `decisions.md`).
- The end-to-end tests depend on the emulator. With 2 GB of RAM the emulator showed "System UI isn't responding" and tests failed because of that window; give it 4 GB.

## Final summary

All tasks of phases 5, 6 and 7 are ticked. `./gradlew.bat build` passed at the end of phase 7 (unit tests, architecture rules, lint, release build). On 2026-11-10 the full instrumented run passed on an emulator with Android 17: 58 of 58 end-to-end tests and 11 of 11 integration tests of the Android core.
After that the debug mode was removed (schema 13, 57 end-to-end tests left). The unit tests and the 11 integration tests pass, and all 57 end-to-end tests pass on two emulators with 4 GB of RAM: Android 15 (5 min 50 s) and Android 16 (6 min 22 s). An earlier run on the Android 17 emulator was cut off when its system crashed.
Then the tests were reworked so that each path is tested once: end-to-end tests get their data (API key, plans, workouts) through the app's services instead of tapping it in, and never tap the system Health Connect screen (50 end-to-end tests, 10 integration tests). All of them pass on the Android 16 emulator.
On 2026-11-11 a bug hunt fixed: deleting a plan or workout went back two screens (e.g. from All workouts to Training); "Back" tapped twice went back twice; the Coach tab opened from Training, Plan or the targets screen landed on the wrong tab's history; the topic of the chat stayed after leaving without sending; "Undo" could overwrite a plan or sets changed by hand; accepting a change of a plan deleted in the meantime crashed the app; a plan could be saved with empty reps; rows in lists were two separate elements for TalkBack; the "coach has a proposal" card on Training and Plan opened an empty plan preview for a workout change, new exercise or nutrition target (it now shows plan proposals only).
After these fixes and the new coach features (undo, topics, coach during a workout, new exercise, nutrition target, voice entry) all 53 end-to-end tests and the 10 integration tests pass on a freshly booted Android 16 emulator.
Check by hand: the list under "Things to check by hand on the phone".
