# Strength history calculations

This slice adds a read-only view at **History → Strength history → exercise**. It queries the completed strength rows, not the current draft. It does not persist new metrics, change the Room schema, add bodyweight, or update existing data. A workout repeated into Track does not appear until it is saved.

## Three separate measures

| Measure | Meaning |
| --- | --- |
| Heaviest logged load | The maximum load from any valid saved set, regardless of rep count. Its source set retains the entered numerical load, entered unit, reps, exercise name and session. |
| Heaviest logged single | The maximum load from a saved one-rep set. An explicitly entered single is not a tested max unless that effort was separately verified; the app does not know whether it was maximal. It is not shown as an estimate. |
| Estimated 1RM history | One estimated point per session when there is a qualifying set; the maximum qualifying estimate in that session. The point retains its source set. A session without an estimate has no estimated point, not zero. |

The provisional prototype uses only the Epley-style formula `estimated load = entered load × (1 + reps / 30)`. The app restricts it to positive loads and 2–10 reps as a product rule. For example, `80 kg × 8` estimates about `101.3 kg`; a separately logged `90 kg × 1` remains a `90 kg` single. A bigger estimate does not mean the estimated weight was ever lifted. The app does not capture exertion, so a set that stops short of a maximum effort is not a validated 1RM test. Use estimates as approximate trends, not training prescriptions or comparisons across distinct exercise variants. See [a published comparison of prediction equations](https://revistas.rcaap.pt/motricidade/article/download/2948/4846/15810) for the Epley equation context.

## Units, variants, and invalid records

- Compare and calculate using kilograms as the normalized mass; [NIST defines 1 avoirdupois lb as exactly 0.45359237 kg](https://www.nist.gov/system/files/documents/2024/01/14/NIST%20HB%2044%202024%20Appendix%20C%20General%20Tables%20of%20Units%20of%20Measurement.pdf). Keep the original numerical load and unit for display and calculation provenance. Do not rewrite saved values or round inputs/results until display.
- Existing history stores weights in pounds and does not have a unit field. The strict legacy adapter explicitly tags them as lb. Future entry of kilograms requires an explicit new unit in source data; this prototype does not add kg input or a database field.
- Use case-insensitive, whitespace-normalized exercise names as prototype keys. “Bench Press” and “ bench  press ” group together; “Paused Bench Press” and “Machine Bench Press” remain separate. Different equipment or variants logged under an identical name cannot be distinguished from old records. Use distinct names for them for now; eventual stable exercise IDs may improve this without renaming history automatically.
- Accept valid zero external load as a logged set, but never manufacture an estimate from it. Reject negative or nonfinite load, nonpositive reps, blank name, invalid session/date, and estimates that cannot be represented as a finite positive value. The legacy adapter also skips malformed JSON and missing or nonnumeric weight/reps instead of defaulting missing weight to zero. It does not rewrite or delete those records.
- Sort by the original stored session time (epoch milliseconds), oldest first, with session ID as the deterministic tie-breaker. Retain separate sessions on the same day. Equal strength values retain the first observation in that stable sequence.

## Using the read-only view

Choose an exercise to see the heaviest saved set and single, followed by a session trend with each estimate and its original source. The list starts newest first and can switch to oldest first; its underlying calculation remains chronological. The view uses headings, ordinary buttons, and a text trend rather than a chart, so the figures do not rely on color or gestures. A session without an estimate says **no qualifying set**; it does not display a fabricated zero. The source line includes the entered load, unit and rep count plus the original session date and local time formatted in the device’s locale and time zone. A chronological session number also distinguishes entries with the same displayed time.

Unreadable or incomplete sets are omitted from this view without changing the database. If all saved strength data is missing or unreadable, the view explains why nothing is shown. A storage error has a retry, which only reloads completed rows. If a workout save is in progress, the view waits for it before showing a new snapshot and retains the selected exercise when it can. Return to workout history via the Back button, or select Track to leave Strength history.

The calculator and text formatting are covered by JVM tests. The synthetic repository test and two Compose UI tests compile, but this branch has not run them on an Android device or emulator. A normal APK or update was not built for distribution.

## Follow-on integration plan

1. Verify the current text-based view on the smallest supported phone with large text, screen reader and Android Back. Exercise loading, empty or corrupted rows, and chronological scroll using synthetic data.
2. If/when adding a chart, keep the accessible text trend. Show distinct markers for actual singles and a separate estimate series with gaps for non-qualifying sessions, never zero or interpolated points. Label each estimate and attach its original saved exercise, load, unit, rep count and session date. Add chronological and empty-state UI tests before showing it to others.
3. Consider explicit identifiers, rep PRs, and mixed-unit entry separately. The roadmap still includes the original median-of-three formula estimate, a reps-in-reserve (RIR) input and visible formula spread; none is implemented by this provisional Epley-only slice. Chart number formatting and future bodyweight math should consume the same underlying raw records; they should not be mixed into this calculated milestone.
