# Requirements

What the app does, as decided in phase 3. The mockups in [`design/`](design/README.md) show the
screens; this file holds the rules the mockups can't show. Where they disagree, this file wins.
Hard constraints and secrets handling are in [`../CLAUDE.md`](../CLAUDE.md).

## 1. Principles

1. The only required data is workouts logged in the app. Everything from Health Connect is
   optional, per data type. A missing value hides its field or card; it is never shown as zero.
2. Nothing changes automatically. The coach proposes, the user accepts in the chat or replies.
3. The chat is where everything with the coach is agreed. Every coach change (plan, weekly
   target) arrives as a proposal card in the chat.
4. Everything the coach can change, the user can also change by hand.
5. The coach is optional. With the coach switched off, or without an API key, the rest of the app
   works in full.
6. No readiness score and no automatic progression.

## 2. Navigation

- Bottom bar: **Trening · Plan · Trener · Postęp**. Settings open from the gear icon in the top bar.
- With the coach switched off the bar has three tabs and no coach elements appear anywhere.
- During a workout the bottom bar is hidden; the bottom shows "Odrzuć" and "Zakończ".
- Until the coach exists (before phase 6) the app behaves as with the coach switched off.

## 3. Exercises

- An exercise has a name, one **primary** muscle group and zero or more **secondary** groups.
- Muscle groups: chest, back, legs, shoulders, arms, abs.
- The seed assigns groups to every seeded exercise. New seed data goes through a migration.
- The user can create an exercise: name, primary group, secondary groups.

## 4. Workouts

### Set

| Field | Rule |
|---|---|
| Weight | kg, typed on the numeric keyboard, decimals allowed. Empty for bodyweight exercises |
| Reps | integer, changed with − / + or typed |
| RPE | optional, 6 to 10 in steps of 0.5, picked from a row of chips under the set |
| Warm-up | flag, shown as "R". An exercise can have any number of warm-up sets |

Working sets are the sets without the warm-up flag. Statistics, records, volume and the "last
time" label use working sets only.

### Flow

1. Start from a plan (the next plan on the start screen, or any plan from the sheet) or start empty.
2. A plan fills the workout with its exercises and sets as editable values. The user changes
   values, adds and removes sets and exercises freely.
3. "Zakończ" saves the workout. The saved sets are what was done; there is no ticking off.
   Then the summary screen is shown.
4. "Odrzuć" asks for confirmation, then drops the workout. The plan stays.
5. A workout saved from a plan keeps a link to it. That plan is then done and leaves the
   upcoming list.
6. A finished workout can be edited and deleted. Deleting asks for confirmation.
7. An unfinished workout survives closing the app.

### "Last time" label

Shown above each exercise in a running workout: the working sets of the most recent finished
workout containing that exercise.

- Format per group: `<sets>×<reps>×<weight> kg RPE <rpe>`, for a single set `<reps>×<weight> kg RPE <rpe>`.
- Consecutive sets with the same weight and reps merge into one group. Their RPE is one value, or
  a range `8–9` when it differs. No RPE recorded: the RPE part is left out.
- Groups are separated by ` · `. A line break may fall only at a separator.
- At most two lines; the rest is shown as `+N` (N = hidden sets). Tapping the label opens the
  exercise history.
- No previous workout with the exercise: no label.
- Bodyweight sets: `3×10 · masa ciała`.

The same format is used in the exercise history and the workout summary.

### Records and estimates

- Estimated 1RM of a set: Epley, `weight × (1 + reps / 30)`, for working sets of 1 to 12 reps.
  The exercise's estimated 1RM on a day is the best set of that day.
- Records detected when a workout is saved: highest estimated 1RM, and most reps at a given weight.
- The summary compares each exercise with the plan: "zgodnie", or the difference in weight, reps
  or number of sets. Without a plan this section is absent.

## 5. Plans

- A plan is one workout to do: name, optional date, exercises, and for each set a target weight,
  reps, optional target RPE and the warm-up flag.
- A plan has an author: the coach or the user. Both kinds are edited the same way.
- Lists: upcoming plans by date, then plans without a date. Done plans are not listed.
- The week strip shows Monday to Sunday of the current week: finished workouts, planned
  workouts, workouts from other apps.
- "Zapisz" on a new plan needs a name and at least one exercise.

## 6. Weekly sets per muscle group

- A working set counts **1** for the exercise's primary group and **0.5** for each secondary group.
- The week runs Monday to Sunday.
- Each group has a target range of sets per week. The default is **10–20** for every group.
- A target has a source: default, coach (with date and reason) or user (with date).
- The user edits ranges on the targets screen and can restore the defaults. The coach changes a
  range only through an accepted proposal. Accepting a target change does not touch existing plans.
- "Pokazuj cele" switches the target bands off; the counts stay.
- Progress shows the weekly average over the selected range against the target.
- Plan shows the current week: sets done plus sets in this week's plans, against the target.

## 7. Health Connect

- Read only. The app never writes to Health Connect and never names a source app.
- Data types, each with its own permission, each optional:

| Type | Stored per day as |
|---|---|
| Nutrition | totals: kcal, protein, carbs, sugar, fat, saturated fat, fiber, sodium |
| Calories burned | total for the day |
| Weight | the last measurement of the day |
| Steps | sum |
| Sleep | duration, assigned to the day of waking up |
| Resting heart rate | daily average |
| HRV | daily average |
| Workouts from other apps | one row per session: type, start, duration, and distance, kcal, average heart rate when present |

- Data is copied into Room and stays there even when it disappears from Health Connect.
- First sync reads one year back, when the history permission is granted; otherwise 30 days.
- Every sync reads the last 14 days again and overwrites them, which picks up edits and deletions.
- Sync runs when the app comes to the foreground and from "Synchronizuj teraz". No background work.
- A type without permission is skipped; the others still sync. Workouts written by this app's
  own package are ignored.
- The Health Connect screen shows the state per type, the last sync time and the history state.
  When Health Connect is missing or outdated, it offers install or update instead of "Połącz".

## 8. Progress

- Range switch: 4 weeks, 3 months, year. It applies to every card.
- Cards: weight (daily points and 7-day average), calorie balance (eaten − burned, weekly bars),
  macros, steps, sleep, resting heart rate, HRV, strength (estimated 1RM per exercise), sets per
  muscle group, days.
- A card whose data is missing is hidden. With no Health Connect data at all, the workout cards
  remain and one card invites to connect.
- A metric opens its detail: chart, lowest, highest, change per week, list of values.
- A day opens the day detail: everything stored for that date.
- A strength row opens the exercise history.

## 9. Coach

- Claude API with the user's own key. The key and the model are set in Settings.
- The API is stateless. Every request carries: the profile, the coach's notes, the weekly
  targets, a short summary of recent data, and the conversation.
- The coach reads more through tools and changes the app only by creating proposals.

| Tool group | Tools |
|---|---|
| Read | workouts, one exercise's history, days from Health Connect, workouts from other apps, plans, weekly sets and targets |
| Propose | new plan, change of a plan, several plans at once, change of a weekly target |
| Memory | add, change and delete its own notes; fill in the profile |

- A proposal is pending, accepted or superseded. A newer proposal for the same plan or target
  supersedes the older one. Only "Akceptuj" applies a proposal. Replying is how it is declined
  or adjusted.
- A pending proposal is also shown as a card on Trening and Plan, and on the targets screen when
  it concerns a target.
- Under a coach message, "Sprawdzono: …" lists what the tools read for that answer.
- Memory screen: profile (goal, time, equipment, limitations, preferences), weekly targets,
  dated notes. The user can edit or delete each item.
- First conversation: greeting and suggestion chips. No key: a screen leading to the key entry.
- Errors: no network (banner, unsent message with retry), rejected key (leads to the key
  screen), no answer or overload (retry; the user's message is kept).
- The model screen shows this month's message and token counts, kept locally.
- Weekly review (phase 7): once a week, when the app is opened, the coach posts a review of the
  week and may propose the next week's plans.

## 10. Settings

- Coach switch, API key, model, memory screen; Health Connect; backup; debug.
- Backup: export everything except the API key to one file; import replaces all data after
  showing what the file contains and asking for confirmation.
- Debug (debug builds only): switch between real and fake Health Connect and Claude, the log of
  Claude requests and tool calls, the sync log, row counts. It never shows the API key.

## 11. Decided by the designer, not yet confirmed by the owner

Build these as written; they are the first candidates to change after the owner tries the app.

1. Custom exercises exist.
2. Finished workouts can be edited and deleted.
3. Records use the Epley formula and the two record kinds in section 4.
4. The filter "Siłownia / Z innych aplikacji" on the list of all workouts.
5. The monthly usage counter on the model screen.
6. The weekly review can propose several plans with "Akceptuj wszystkie".
7. Import replaces data instead of merging.
8. Default target 10–20 for every muscle group, including abs and arms.
9. First Health Connect sync reads one year.
10. A plan started and saved is done; a discarded workout leaves its plan untouched.
