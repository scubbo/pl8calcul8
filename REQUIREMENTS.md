# Requirements & Decisions

Outcome of the initial requirements interview (2026-08-15). Where this document
conflicts with SPEC.md, this document wins.

## Core data model

* **Lift**: a movement (e.g. Bench Press). App ships with a seeded list of
  common lifts; user can add more. Each lift has a configurable weight
  increment (default 5lb).
* **Exercise definition**: assigned reps @ RPE × sets for a lift
  (e.g. Bench 4@7×3). Entered via structured pickers (lift / reps / RPE / sets),
  not text parsing.
* **Exercise (completed)**: one weight + one RPE recorded **per exercise**
  (not per set), plus optional free-text notes. Actual reps are assumed to
  match the assigned rep count; failures are noted in the notes field.
* **Workout**: a dated collection of exercises completed in one session.

## Units

* lb only. No kg support.

## RPE calculation

* Ratio table: the RTS/Tuchscherer RPE chart (percentage of e1RM by
  rep-count × RPE, half-integer RPEs supported). Jack verified the embedded
  table against his copy. Extended with a derived RPE 6 column using the
  reps-in-reserve diagonal (n@6 == (n+1)@7); 12@6 extrapolated to 57.4%.
* Assigned RPE (exercise definitions): whole numbers 6-10 only.
* Recorded RPE (results): half-steps 6.5-10.
* Advice input: the single recorded weight/RPE of the most recent completed
  exercise for that lift.
* Advice algorithm:
  1. Look up most recent exercise for the lift → (weight, reps, RPE).
  2. Compute implied 1RM via the ratio table.
  3. Compute target weight for the new (reps, RPE) from that 1RM.
  4. Add the lift's increment (always applied; the ratio math self-corrects
     for unusually hard/easy previous sessions).
  5. Round to the **nearest** 5lb; exact midpoints round **down**
     (212.5 → 210, 213 → 215). Note: supersedes SPEC.md's round-down rule.
* First time a lift is performed (no history): prompt the user to enter a
  starting weight manually.

## Session UX

* Active session companion: start a session → enter exercise definitions via
  pickers → see advised weights → record actual weight/RPE/notes per exercise
  as the session progresses.
* Picker order: Sets → Reps → RPE.
* Numeric inputs (sets/reps/RPE/weight) are vertical spinners, not
  dropdowns or free text. Weight spinner moves in 2.5lb steps, defaulting
  to the advised weight (or a 45lb bar when there's no advice).
* The lift dropdown includes a "New Lift…" entry to create a lift
  mid-session (default 5lb increment).

## History

* Two modes:
  * Single lift: graph of calculated 1RM over time, graph of total weight
    lifted over time, and a scrollable table of past exercises.
  * Compare lifts: select any number of lifts (filter chips) and graph
    either estimated 1RM or total weight lifted for all of them on one
    chart with a legend.
* "Total weight lifted" means tonnage: weight × reps × sets per exercise.
* Dev convenience: `scripts/seed-emulator.sh` fills the emulator DB with
  8 weeks of plausible history.

## Settings

* Lift list showing each lift's increment; tap to edit the increment
  (spinner, 0-25lb in 2.5 steps). Add-lift button.
* No lift rename/delete in v1 (delete is blocked by exercise history;
  rename raises data-identity questions).
* Backup/Restore buttons live here, disabled until the server exists.

## Backup / server

* Manual "Backup now" and "Restore" buttons in the app.
* Auth: static shared secret / API key configured in the app.
* Self-hosted; endpoints for upload and restore only (web history view is a
  stretch goal, not P0).

## Backlog (explicitly not v1)

* Rest timer
* CSV export
* Dark mode
* Web interface for viewing history
* Custom app icon (currently the default Android icon)
* In-progress session survives process death (currently a killed app loses
  an unfinished workout; ViewModel state only survives rotation)

## Constraints

* Personal use only; no Play Store publication.
* Jack is new to Android development: prioritise a local emulator workflow
  and explain Android concepts from first principles as we go.
