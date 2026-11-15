# Strength history calculations

This is a calculation-layer prototype. It does not add a screen, persist a new metric, change the Room schema, add bodyweight, or update existing data. Completed workouts still supply the raw observations; drafts and repeated-but-unsaved workouts must not enter strength history.

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

## Integration plan

1. Read the completed Room rows and feed the strict adapter the saved strength JSON, rather than reusing the ordinary display decoder’s fallback values. Keep the calculation pure and operate only on completed strength data, not cardio or drafts.
2. Add a read-only exercise detail view. Show heaviest logged load in the original unit, a distinct marker for actual singles, and a separate estimated series with gaps for non-qualifying sessions, never zero or interpolated points. Label each estimate and attach its original saved exercise, load, unit, rep count and session date. Add chronological and empty-state UI tests before showing it to others.
3. Consider explicit identifiers, rep PRs, and mixed-unit entry separately. The roadmap still includes the original median-of-three formula estimate, a reps-in-reserve (RIR) input and visible formula spread; none is implemented by this provisional Epley-only slice. Chart number formatting and future bodyweight math should consume the same underlying raw records; they should not be mixed into this calculated milestone.
