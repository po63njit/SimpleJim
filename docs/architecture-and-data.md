# Architecture and data

## How a workout moves through the app

```text
Compose Track / History
       ↕ UI state and user actions
SimpleJimViewModel (editor state, validation, ordered persistence)
       ↕
WorkoutRepository (Room transactions)
       ↕
WorkoutDao → simple_jim.db (SQLite in the app sandbox)
```

| Source | Responsibility |
| --- | --- |
| `MainActivity.kt`, `ui/SimpleJimApp.kt` | Activity, Compose screens, entry controls and feedback. |
| `SimpleJimViewModel.kt` | Draft state, exercise IDs, validation, serial draft commands, restore and save. |
| `DraftSet.kt`, `WorkoutDraft.kt` | Raw typed draft values, validation for conversion to a completed set, versioned draft encoding. |
| `data/WorkoutDatabase.kt`, `WorkoutDao.kt`, `WorkoutRepository.kt` | Storage, queries, migration, draft revision checks and transaction boundaries. |
| `data/WorkoutModels.kt`, `WorkoutSessionEntity.kt` | Completed session domain model and mapping to stored history. |

All paths above are under `app/src/main/java/com/simplejim/tracker/`. The app is a single Android module using Kotlin, Compose, ViewModel, and Room.

## Data model

| Table | What is stored |
| --- | --- |
| `workout_sessions` | Completed session ID, performed time in epoch milliseconds, notes, and JSON containing the named exercises and numeric weights/reps. The numeric weight in the current UI is pounds. |
| `workout_draft` | One lazily created row with ID 1, monotonic revision and nullable versioned JSON. The JSON retains exercise IDs, exact draft text, notes and incomplete sets. A null payload means no saved draft. |

History and drafts are deliberately separate. The history structure contains completed strength logs, not cardio metrics, bodyweight, custom PR rankings, or charts. Changing any of those should not rewrite older raw observations just to fit a new metric.

## Database migration 1 → 2

The only migration step creates `workout_draft`. It neither drops nor alters `workout_sessions`, and there is no destructive migration fallback. Existing history becomes readable under the same session schema. A v1 build did not persist a draft: a transient pre-upgrade editor was never in the database and cannot be reconstructed.

The following invariants are part of the storage design:

1. No editor writes before initial restore resolves. A malformed or unsupported draft is not replaced on load; the app offers retry and an explicit reset option.
2. Draft updates are ordered in the ViewModel, and every write checks the expected SQLite row revision. A stale write cannot resurrect an already-cleared draft. A conflict or storage failure is surfaced rather than silently overriding a newer row.
3. Explicit Save inserts a session and clears the corresponding draft in one Room transaction. If either part fails, the transaction rolls back; existing history is not replaced. The UI confirms the workout only after commit.
4. Reset or Repeat requires confirmation when a draft contains a name, set input or notes. Reset clears only the draft and advances its revision. It does not delete or recalculate saved sessions.
5. Committed writes survive restart. The most recent uncommitted input can still be lost if the process is killed; autosave progress is not a completed-workout confirmation.

Before treating an upgrade as supported, [exercise a separate synthetic fixture](android-smoke.md). Never treat an uninstall or downgrade as a migration recovery step. There is no v2→v1 migration, and a v1 build cannot open the upgraded database.

## Data and privacy

The app requires no account and has no app backend, sync, telemetry, or declared `INTERNET` permission. Workout history and drafts live in the app’s private SQLite database. The app relies on Android’s application sandbox; it does not add independent database encryption or a workout export function.

The application manifest leaves Android system backup enabled. Depending on device settings and Android implementation, system backup or device transfer can include app data. This is distinct from SimpleJim running its own cloud service. Clearing app storage or uninstalling can remove local logs, and recovery through the platform is not guaranteed.

Fixtures use a different application package and store only the synthetic workout data created in them. Do not execute automated smoke tests against personal history.
