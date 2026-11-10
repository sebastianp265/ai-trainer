# Design

The mockups of every screen, made in phase 3. They show what each screen contains and how screens
connect. They are not a pixel specification.

- Live canvas (rendered, clickable in Play mode): https://claude.ai/artifact/XMvr4pb1NaCcrkWRr78UQn.
  It is private to the project owner; an agent reads the files in `screens/` instead.
- `screens/*.dc.html`: one file per screen. `screens/canvas.json`: titles and layout on the canvas.
- `screens/` is a local copy, ignored by git. When it is missing, download the files of the
  canvas into it with the Artifact tool (list the artifact's files, then read every path under
  `project/`). If that isn't possible, stop and ask the owner for the files.

## How to read a screen file

The files don't render in a plain browser (they need the canvas runtime). Read them as source:

| What you need | Where it is in the file |
|---|---|
| Content of the screen, top to bottom | the markup inside `<x-dc>`, after `</helmet>` |
| Exact Polish copy | literal text in the markup; copy it into `values-pl/strings.xml` and translate for `values/strings.xml` |
| Where a button or row leads | `href="Other.dc.html"` on the element |
| State on the same screen (sheet, dialog, expandable box) | `<sc-if value="{{ flag }}">` plus the `onClick` handlers in the script at the bottom |
| Sizes, spacing, colors | the `<style>` block; treat as a hint only |

## What is binding and what is not

| Binding | Not binding |
|---|---|
| Which screens exist and what opens what | Exact sizes, spacing, corner radii |
| Which data and actions each screen has, and their order | Colors: the app uses Material 3 dynamic color |
| Copy (Polish) | Heights of the artboards |
| Rules written on the screens (captions, info boxes) | Sample numbers, names and dates |

When a mockup and [`../requirements.md`](../requirements.md) disagree, the requirements win. Build
screens with standard Material 3 components (bottom sheet, alert dialog, segmented button, switch,
navigation bar); the mockups imitate them in HTML.

## Screens

The phase is where the screen is built, see [`../plan.md`](../plan.md).

| File | Screen | Phase |
|---|---|---|
| `Main.dc.html` | Training: start. "Inny trening" opens the plan picker sheet | 5 |
| `StartSheet.dc.html` | Training: the same screen with the plan picker sheet open | 5 |
| `ActiveWorkout.dc.html` | Workout in progress, started from a plan. "Odrzuć" opens a confirmation dialog | 5 |
| `WorkoutSummary.dc.html` | Summary shown after "Zakończ" | 5 |
| `WorkoutDetail.dc.html` | A finished workout | 5 |
| `TrainingEmpty.dc.html` | Training: first launch, no workouts and no plans | 5 |
| `ExercisePicker.dc.html` | Add exercise | 5 |
| `ExerciseHistory.dc.html` | History of one exercise | 5 |
| `History.dc.html` | All workouts, including those from Health Connect | 4 |
| `ExternalWorkout.dc.html` | A workout from Health Connect, read-only | 4 |
| `MainNoCoach.dc.html` | Training: start, with the coach switched off (three tabs) | 5 |
| `Plan.dc.html` | Plan: list, week strip, sets this week against targets | 5 |
| `PlanDetail.dc.html` | Plan: details | 5 |
| `PlanEdit.dc.html` | Plan: manual edit | 5 |
| `PlanEmpty.dc.html` | Plan: no plans | 5 |
| `PlanNew.dc.html` | Plan: new, empty form | 5 |
| `Coach.dc.html` | Coach: chat with proposal cards | 6 |
| `CoachNoKey.dc.html` | Coach: no API key | 6 |
| `CoachMemory.dc.html` | What the coach knows about me: profile, targets, notes | 6 |
| `CoachProposal.dc.html` | Coach: preview of a proposed plan | 6 |
| `CoachEmpty.dc.html` | Coach: first conversation | 6 |
| `CoachWeekly.dc.html` | Coach: weekly review | 7 |
| `CoachError.dc.html` | Coach: errors (one screen shows all variants) | 6 |
| `CoachTargets.dc.html` | Coach: proposal to change a weekly set target | 6 |
| `Progress.dc.html` | Progress | 4, volume card in 5 |
| `ProgressNoHealth.dc.html` | Progress without Health Connect | 4 |
| `ProgressMetric.dc.html` | Progress: one metric in detail (weight; the others look the same) | 4 |
| `DayDetail.dc.html` | Progress: one day | 4 |
| `VolumeTargets.dc.html` | Weekly set targets per muscle group | 5 |
| `Settings.dc.html` | Settings | 4, coach rows in 6 |
| `HealthConnect.dc.html` | Settings: Health Connect | 4 |
| `HealthConnectOff.dc.html` | Settings: Health Connect not connected or unavailable | 4 |
| `ApiKey.dc.html` | Settings: API key | 6 |
| `ModelPicker.dc.html` | Settings: model | 6 |
| `DataBackup.dc.html` | Settings: export and import | 7 |
| `SettingsNoCoach.dc.html` | Settings with the coach switched off | 5 |
