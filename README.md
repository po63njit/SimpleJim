# SimpleJim

SimpleJim is a simple Android gym log. Enter a lift, weight and reps, copy a set when you repeat it, then save the workout to your history. No account is required.

## What works today

- **Track:** quick-add common lifts, name custom exercises, log sets in pounds, and optionally add workout notes.
- **Copy or repeat:** copy the immediately preceding set if it is complete, or bring the previous workout into a new editable draft.
- **Resume:** edits autosave locally as a draft, including unfinished sets. A draft does not count as a completed workout.
- **Finish:** tap **Save Workout** to add the session to **History**. **Start Fresh** clears only the current draft.
- **History:** review completed sessions and their exercise/set breakdown. There is no aggregate training-volume calculation yet.

Cardio, PR and estimated 1RM charts, bodyweight ratios, and benchmarks are planned, not implemented. See the [roadmap](docs/roadmap.md).

## Get started

Open this repository root in Android Studio with JDK 17 and Android SDK 34. The pinned Gradle wrapper is at the root. The app requires Android 8.0 (API 26) or newer.

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

The resulting debug APK is `app/build/outputs/apk/debug/app-debug.apk`. **If upgrading an existing installation, install over it with the same signing key; do not uninstall or clear data to bypass a signing mismatch.** See [build and update](docs/build-and-update.md) for the full install and migration checklist. This repository documents source and debug builds, not a Play Store release.

## Documentation

- [Documentation index](docs/README.md)
- [Log a workout and recover a draft](docs/workout-guide.md)
- [Build, install and upgrade](docs/build-and-update.md)
- [Architecture and data](docs/architecture-and-data.md)
- [Testing and migration fixtures](docs/testing.md)
- [Roadmap](docs/roadmap.md)

SimpleJim does not send workouts to a SimpleJim service. Workouts and drafts are stored in local Room/SQLite. Android's own backup or device transfer may apply depending on device settings; see [data and privacy](docs/architecture-and-data.md#data-and-privacy).
