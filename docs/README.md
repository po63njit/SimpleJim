# SimpleJim documentation

| Start here if you want to… | Read |
| --- | --- |
| Log a lift, repeat a set, or resume unfinished work | [Workout guide](workout-guide.md) |
| Build, install, or update without uninstalling | [Build and update](build-and-update.md) |
| Understand Room, draft ordering, migration, or privacy | [Architecture and data](architecture-and-data.md) |
| Run checks or decide what remains unverified | [Testing and evidence](testing.md) |
| Rehearse an in-place upgrade with synthetic data | [Android smoke and migration protocol](android-smoke.md) |
| Read per-exercise strength history and understand estimates | [Strength history calculations](strength-history.md) |
| See what is intentionally not shipped | [Roadmap](roadmap.md) |

## Keep this documentation accurate

- Update relevant docs in the same change as a visible feature, button name, unit, dependency, permissions change, schema migration, or build procedure. Start by checking the root README and update this index when adding a guide.
- Separate implemented behavior, a planned feature, a built APK, and an on-device result. A compiled instrumentation test is not a passed test. Identify the source/build exercised and report the specific checks that passed, failed, or were skipped.
- Treat work and history as separate concepts. A draft was not “saved to History” until a session is committed. Describe important invalid input, failure, restart and recovery behavior, not just the happy path.
- For every database change, document the old and new schema, data that must be preserved, the migration path, downgrade risks, and a synthetic fixture that can prove the upgrade before touching real history.
- Document calculation units, formulas and edge cases before shipping new metrics. Link to the authoritative explanation instead of copying it across pages.
- Use synthetic examples. Never put device logs, personal workout records, private links, credentials, debug signing material, or local machine paths in the repository.
- Keep progress notes and source release labels honest. A change pushed to source is not automatically present in an earlier APK that uses the same app version label.
