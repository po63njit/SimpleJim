# Manual outdoor run and walk (local follow-on; not released)

The Cardio tab adds **Outdoor run** and **Outdoor walk** as distinct activity
types. Record the known final elapsed duration and measured total distance;
select miles or kilometres explicitly. The app stores those values as entered
and computes the whole-activity average pace in the entered unit. This is
manual, offline logging: no GPS, route, elevation, sensors, networking, extra
permissions, calorie estimates, or split/segment records.

Synthetic examples: a 28:12 run over 4.20 km averages 6:43/km; a 1:02:30 walk
over 3.75 mi averages 16:40/mi. Add one record for each complete activity;
do not duplicate a mid-activity checkpoint as another workout.

## Data and versioning

Database version remains **3**. Its existing text activity kind and typed
duration/distance columns already represent the new rows; the new type names
are `OUTDOOR_RUN` and `OUTDOOR_WALK`. No machine stats are stored on either.
Treadmill and stair rows keep their original kind, fields, and rendering.
This change has no database migration, app build/version bump, or new
dependency. Do not substitute it for the machine-cardio migration fixture.
**Do not roll back to an older APK.** Downgrades are unsupported. The
machine-only v3 app does not display outdoor rows in History, even if the rows
are still stored.

## Test gate before release

First verify the existing isolated machine-cardio fixture separately. For the
follow-on, use a new disposable fixture package or dedicated emulator with
synthetic data only:

1. Install the machine-only v3 build. Save one strength workout, one treadmill
   session, and one stair-machine session. Note the entry counts, original
   values, and units.
2. Install the outdoor build over that test app as an **Update**, using the
   same fixture application ID and signing key. Do not uninstall first. Verify
   the original strength and machine entries still display once and unchanged.
3. Save an outdoor run in kilometres and an outdoor walk in miles. Confirm
   their distinct labels and average paces, then force-stop the test app and
   reopen it. Verify both outdoor entries and all three original entries still
   display once with their original values.
4. Try saving with missing or zero distance, and confirm the form rejects it
   without adding a history entry.

Do not install test builds over your regular SimpleJim app or roll back the
fixture to inspect the saved data.
