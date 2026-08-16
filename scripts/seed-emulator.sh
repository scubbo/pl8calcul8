#!/usr/bin/env bash
# Seeds the emulator's pl8calcul8 database with plausible workout history
# so the History screen has something to show during development.
#
# Replaces any existing workout/exercise data (lifts are kept).
# Requires: a running emulator (adb root must work) and the app installed
# and launched at least once (so the database exists).
set -euo pipefail

ADB="${ADB:-$HOME/Library/Android/sdk/platform-tools/adb}"
PKG=com.scubbo.pl8calcul8
DB="/data/data/$PKG/databases/pl8calcul8.db"

"$ADB" root >/dev/null
sleep 1
if ! "$ADB" shell "ls $DB" >/dev/null 2>&1; then
    echo "error: $DB not found - install the app and open a data screen (Settings is quickest) so Room creates the database" >&2
    exit 1
fi
"$ADB" shell am force-stop "$PKG"

python3 - <<'PYEOF' | "$ADB" shell "sqlite3 $DB" && echo "Seeded. Relaunch the app."
import random
import time

random.seed(8)  # deterministic output

DAY_MS = 86_400_000
now = int(time.time() * 1000)

# name -> (start weight, weekly increment, assigned reps@rpe x sets)
PROGRAM = {
    "Squat":          (225.0, 5.0, (5, 8.0, 3)),
    "Bench Press":    (185.0, 5.0, (5, 8.0, 3)),
    "Barbell Row":    (135.0, 5.0, (8, 7.0, 3)),
    "Deadlift":       (275.0, 10.0, (3, 8.0, 3)),
    "Overhead Press": (95.0,  2.5, (5, 8.0, 3)),
    "Bicep Curl":     (65.0,  2.5, (10, 7.0, 3)),
}
DAY_A = ["Squat", "Bench Press", "Barbell Row"]      # e.g. Mondays
DAY_B = ["Deadlift", "Overhead Press", "Bicep Curl"]  # e.g. Thursdays
WEEKS = 8
NOTES = [
    "felt strong", "grinder on last set", "slept badly", "paused reps",
    "belt on", "rushed rest times", "new gym", "shoulder niggle",
]

def round5(x):
    return round(x / 5.0) * 5.0

print("PRAGMA foreign_keys=ON;")
print("DELETE FROM Exercise;")
print("DELETE FROM Workout;")

workout_id = 0

def emit_workout(date_ms, lifts):
    global workout_id
    workout_id += 1
    print(f"INSERT INTO Workout (id, date) VALUES ({workout_id}, {date_ms});")
    for name in lifts:
        start, weekly_inc, (reps, rpe, sets) = PROGRAM[name]
        week = (date_ms - (now - WEEKS * 7 * DAY_MS)) / (7 * DAY_MS)
        weight = round5(start + weekly_inc * week)
        # RPE wobbles around the assignment
        actual_rpe = rpe + random.choice([-0.5, 0.0, 0.0, 0.5, 0.5, 1.0])
        actual_rpe = min(10.0, max(6.5, actual_rpe))
        note = f"'{random.choice(NOTES)}'" if random.random() < 0.25 else "NULL"
        print(
            "INSERT INTO Exercise (workoutId, liftId, assignedReps, assignedRpe,"
            f" sets, weightLb, rpe, notes) VALUES ({workout_id},"
            f" (SELECT id FROM Lift WHERE name='{name}'),"
            f" {reps}, {rpe}, {sets}, {weight}, {actual_rpe}, {note});"
        )

for week in range(WEEKS):
    base = now - (WEEKS - week) * 7 * DAY_MS
    jitter = random.randint(-4, 4) * 3_600_000
    emit_workout(base + jitter, DAY_A)
    if week != 5:  # skipped a B day one week; life happens
        emit_workout(base + 3 * DAY_MS + jitter, DAY_B)

print("SELECT 'workouts: ' || count(*) FROM Workout;")
print("SELECT 'exercises: ' || count(*) FROM Exercise;")
PYEOF
