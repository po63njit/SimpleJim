# SimpleJim

SimpleJim is a bare-bones Android gym tracker aimed at fast workout logging on a phone.

## What is in this first version

- Kotlin + Jetpack Compose Android app
- Simple workout logger with exercises, sets, weight, reps, and optional notes
- Local offline persistence with Room (`SQLite`)
- Session history screen with exercise breakdown and rough training volume
- Quick-add preset lifts and one-tap repeat of your last workout
- Lightweight visual style that works well on a Google Pixel-sized device

## Project structure

- `app/` contains the Android application module
- `app/src/main/java/com/simplejim/tracker/` contains the activity, view model, UI, and persistence code
- `app/src/main/res/` contains the manifest resources and app theme

## Opening the project

1. Open this workspace root in Android Studio.
2. Use JDK 17 for the project.
3. Let Android Studio sync the Gradle files and run the `app` configuration on your Pixel.

## Notes

- A Gradle wrapper is checked in, so use `./gradlew` for consistent local builds.
- Weight is currently labeled in pounds to match a typical US gym setup.
