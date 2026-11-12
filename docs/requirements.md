# Requirements

What the app does, as decided in phase 3 and changed since.
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

- Bottom bar: **Trening · Trener · Postęp · Ustawienia**. The tabs have no top bar; nested screens have one with
  the back arrow and their title. "Co trener o mnie wie" opens from Settings.
- Trening shows, from the top: the week strip, a pending proposal, the next plan with "Zacznij",
  the next three plans after it with "Wszystkie plany" and "Napisz własny plan", the sets this week
  against the targets, and the recent workouts. "Wszystkie plany" opens the list of all plans.
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
- The week strip shows Monday to Sunday of the current week: finished workouts and planned
  workouts.
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
- Trening shows the current week: sets done plus sets in this week's plans, against the target.

## 7. Health Connect

- Read only. The app never writes to Health Connect and never names a source app.
- Data types, each with its own permission, each optional:

| Type | Shown per day as |
|---|---|
| Nutrition | totals: kcal, protein, carbs, sugar, fat, saturated fat, fiber, sodium |
| Calories burned | total for the day |
| Weight | the last measurement of the day |
| Steps | sum |
| Sleep | duration, assigned to the day of waking up |
| Resting heart rate | daily average |
| HRV | daily average |

- Nothing from Health Connect is stored in the app. Screens and the coach read it from Health
  Connect when they need it, so edits, deletions and removed permissions show at once. The history
  is as long as Health Connect keeps it.
- Data is read again whenever the app comes to the foreground and after a switch changes. No
  background work.
- Data older than 30 days needs Health Connect's history permission; with it there is no time
  limit.
- A type without permission is not read; the others still are.
- Workouts recorded by other apps are not read: only gym workouts logged in the app count.
- Settings has a section "Do czego trener ma dostęp" (with the coach off: "Dane z Health Connect")
  with one switch per type and a row "Dane sprzed 30 dni" with "Zezwól" while the history
  permission is missing. There is no separate Health Connect screen, no sync button and no
  "connect everything" button.
- Switching on asks Health Connect for the missing permission of that one type. Switching off makes the app and the coach stop reading that type.
  The app cannot revoke a single Health Connect permission, so the permission stays until the user
  removes it in Health Connect.
- When Health Connect is missing or outdated, the section offers install or update instead of the
  switches. The Progress card without data links to this section.

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
| Read | workouts, one workout with all its sets (the running one by default), one exercise's history, days from Health Connect, plans, weekly sets and targets |
| Propose | new plan, change of a plan, several plans at once (also a block of several weeks), change of a weekly target, change of the sets of the running or a finished workout, new exercise, daily calorie and protein target |
| Memory | add, change and delete its own notes; fill in the profile |

- A proposal is pending, accepted, superseded or undone. A newer proposal for the same plan or target
  supersedes the older one. Only "Akceptuj" applies a proposal. Replying is how it is declined
  or adjusted.
- "Cofnij" on an accepted proposal restores what was there before, as long as nothing used it since:
  no workout was started from its plans, its weekly or nutrition target was not changed again, and
  for a change of the running workout the workout is still running. A new exercise cannot be undone.
- Asking the coach from a plan, a workout, or an exercise's history
  opens the chat with that topic attached to the next message, so the coach knows what it is about.
- During a workout a button opens the coach about the running workout, with quick questions
  ("Za ciężko", "Coś mnie boli", "Mam mniej czasu"). An accepted change replaces the sets of the
  exercises it names.
- The accepted nutrition target is shown in Progress (calorie balance and macros cards).
- A pending proposal is also shown as a card on Trening, and on the targets screen when
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

- Coach switch, API key, model, memory screen; Health Connect; backup.
- Backup: export everything except the API key and Health Connect data (it stays in Health Connect) to one file; import replaces all data after
  showing what the file contains and asking for confirmation.
- No debug mode: the app contains only production code. Fakes of Health Connect and Claude live
  in the tests.

## 11. Decided by the designer, not yet confirmed by the owner

Build these as written; they are the first candidates to change after the owner tries the app.

1. Custom exercises exist.
2. Finished workouts can be edited and deleted.
3. Records use the Epley formula and the two record kinds in section 4.
4. The monthly usage counter on the model screen.
5. The weekly review can propose several plans with "Akceptuj wszystkie".
6. Import replaces data instead of merging.
7. Default target 10–20 for every muscle group, including abs and arms.
8. A plan started and saved is done; a discarded workout leaves its plan untouched.
