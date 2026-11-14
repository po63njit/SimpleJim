# Build, install and update SimpleJim

## Toolchain

| Component | Requirement |
| --- | --- |
| Android | Android 8.0+ (API 26 minimum), compile and target SDK 34 |
| Java | JDK 17; select it as Android Studio’s Gradle JDK |
| Gradle | 8.7 via the **root** checked-in wrapper |
| Android Gradle Plugin | 8.4.2 |
| Kotlin / Compose compiler | Kotlin 1.9.24 / Compose compiler 1.5.14 |
| Android SDK Build-Tools | 34.0.0 |
| Compose BOM / Room | Compose BOM 2024.06.00 / Room 2.6.1 |

Version pins live in the root and app Gradle files and the root wrapper properties. The first build normally needs network access to fetch the Gradle distribution and dependencies from configured official repositories. This is a developer build-time requirement, not an app runtime requirement.

## In Android Studio

1. Open the repository root, not the legacy nested directory.
2. Select JDK 17 for Gradle. Install Android SDK Platform 34 and Build-Tools 34.0.0 through SDK Manager if they are missing.
3. Sync the Gradle project. Select the `app` run configuration and a test emulator or Android device.
4. Use **Run** for a test installation when it will not overwrite real data with an incompatible build. For any update against existing history, follow the signing and migration guidance below.

## Command line

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
# Optional: builds the instrumentation APK; it does not run its tests.
./gradlew :app:assembleDebugAndroidTest
# Runs instrumentation on a connected disposable emulator or test phone.
./gradlew :app:connectedDebugAndroidTest
```

On Windows, use `gradlew.bat`. The app APK is `app/build/outputs/apk/debug/app-debug.apk`. The instrumentation APK is a separate package for tests, not an app to install for normal use.

## Safe in-place update

The normal app is labeled **SimpleJim**, package `com.simplejim.tracker`. Version 1.1 uses app version code 2 and migrates the local database from version 1 to 2 when opened. App version codes and the Room schema version are separate concepts.

- Verify a new APK comes from the expected build and is signed with the **same key** as the installed app. A locally built debug APK normally uses that development environment's debug key; it is not necessarily the same as another build's. If Android reports an incompatible signature or the installer does not offer an in-place **Update**, stop. **Do not uninstall or clear app storage.** Ask for the matching build or use a disposable test device.
- Rehearse v1 → v2 on synthetic history first. See the [migration protocol](android-smoke.md#test-the-in-place-history-migration-before-upgrading-personal-data) and [data invariants](architecture-and-data.md#database-migration-1--2).
- Install the verified update **over** SimpleJim. Open **History** first and confirm existing sessions are visible. An interrupted edit may not have reached disk; do not force-stop while **Saving draft…** is shown if those recent changes matter.
- Do not roll back by installing a v1 app after the database has migrated to v2. There is no downgrade migration. There is no in-app export/restore workflow yet, and a system backup is not a promised recovery path.

This repository does not bundle a personal signing key, a store release, or fixture APKs. Normal and fixture APKs have different package IDs. See [testing](testing.md#isolated-fixture) before opening test packages.
