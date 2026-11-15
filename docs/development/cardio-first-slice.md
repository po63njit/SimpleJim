# First cardio logging slice (local candidate; not released)

This branch connects the validated cardio forms and separate history to an
additive Room migration. Android app version and permissions are unchanged.
Incomplete cardio input remains in memory only; cardio draft recovery is not
part of this slice. Existing strength draft and history behavior are unchanged.

## Fit in the app

The separate Cardio tab offers Treadmill and Stair machine forms and cardio
history without mixing these records with strength history. Saving requires
explicit action and valid input. Failed saves retain input and can be retried.
An unchanged retry in the same process reuses the record ID.

| Form | Required | Optional | Derived display |
| --- | --- | --- | --- |
| Treadmill | elapsed duration, displayed distance, explicit mi/km | steady incline as displayed, %; allow decline | whole-entry average pace in the entered unit |
| Stair machine | elapsed duration, displayed floors | displayed level | whole-entry floors/min |

Example treadmill: 26:15, 2.5 mi, 3.5% yields 10:30/mi.
Example stairs: 15:30, 70.5 floors, level 7 yields 4.5 floors/min.
These are synthetic, not recommended workloads or training targets.

Record a treadmill session once, using the final elapsed time and total distance.
For example, if the timer showed 12:00 at a checkpoint and ended at 26:15 after
2.5 mi, record **26:15 and 2.5 mi once**, not a separate checkpoint workout.
If the incline changed, leave it blank: the finish grade is not the session
grade. Segments and within-session pace records are outside this slice.

Accept `mm:ss` (minutes may exceed 59) and `hh:mm:ss`; seconds and the
middle minutes in hour format may not exceed 59. Require positive duration,
distance and floors. Optional values remain absent rather than becoming zero.
Each decimal entry allows up to three fractional digits and a period or comma
as decimal separator, never grouping. A duration that contains only a number
is ambiguous and is rejected rather than silently interpreted.

## Persistence and rollout

A separate Room table, `cardio_entries`, is created by the additive v2→v3
migration; the prior v1→v2 migration remains registered. It makes no changes
to strength history or its singleton draft. Store
elapsed seconds, entered distance as integer thousandths plus the unit, floors
as integer thousandths, and optional incline-percent/level as signed or
nonnegative integer thousandths as appropriate. Derive canonical distance in
millimeters and display metrics from these records, without storing duplicate
derived metrics or inferring any missing values. Maintain a timestamp and
stable ID. Keep global unit settings and any broader cardio workout editing outside
this first slice. Preserve source units when rendering history even if a
preference later changes.

Before any normal cardio build is distributed, use a separate fixture
application ID and signing-matched debug APKs with increasing fixture version
codes. Install database-v2 fixture, save synthetic strength history, leave an
unfinished strength draft and confirm both survive a cold restart. Update in
place to the database-v3 fixture (do not uninstall). Confirm strength history
and the draft are preserved. Save a treadmill and a stair record and confirm
they retain raw fields and derived labels after force-stop/cold reopen. Verify
a double tap or unchanged retried save cannot duplicate entries. Run a separate
v1→latest fixture to exercise both migrations in sequence. Never use personal
workout history for this test. Test both distance units, zero
and malformed input, optional values, input rejection without writes, and
preservation of the original machine values on restore.

Excluded: energy estimates, session splits, GPS, networking, added
permissions, medical claims and PRs. A whole-entry average is not evidence
for the pace of a faster internal segment.
