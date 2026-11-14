# Android logging smoke test

Run on an isolated Android API 26+ emulator or a dedicated test phone. The automated test creates one synthetic workout; do not run it against a personal workout database.

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebugAndroidTest
./gradlew :app:connectedDebugAndroidTest
```

The instrumentation test uses the normal Compose screen to enter a set, copy it, edit the copy, and save. It checks that Room has exactly one matching session, the associated draft is cleared, and History displays both sets after closing and relaunching the Activity. This runs within the instrumentation process and is not a process-death test.

## Separate cold-launch check

On a dedicated fixture, record a unique synthetic exercise (for example `Cold Start Test`), add `95 × 8`, copy the set, change only the duplicate to `95 × 6`, and save. Open History and confirm it has one matching entry with both sets. At the terminal, with that test device the only attached target and the [isolated fixture package](testing.md#isolated-fixture) installed:

```sh
FIXTURE=com.simplejim.tracker.migrationfixture
adb shell am force-stop "$FIXTURE"
# A blank pidof result confirms the package process is stopped.
adb shell pidof "$FIXTURE"
adb shell am start -n "$FIXTURE/com.simplejim.tracker.MainActivity"
```

From the cold launch, open History and confirm one matching entry still shows both `95.0 lb × 8` and `95.0 lb × 6`. This checks persisted history, not unsaved draft recovery. Do not uninstall an existing personal copy to bypass a signing mismatch.

## Test the in-place history migration before upgrading personal data

1. On a fresh dedicated test emulator or test phone, install fixture v1 (from v1 app source) and save a unique synthetic workout. Wait for the v1 write, cold-reopen it, and confirm that History still shows the workout; the v1 success message by itself is not the storage check.
2. Install fixture v2 (from v2 app source) **over** fixture v1, with the same fixture package and signing certificate. Do not uninstall or clear package data. Confirm the synthetic v1 workout still appears in History.
3. Start another named exercise, enter a partial set (for example, weight `102.` with reps blank), and wait for “Saving draft…” to finish. Force-stop and relaunch; confirm the exact text returns. Complete and save exactly one workout, then force-stop and relaunch again; the saved workout must be present and Track must be blank.
4. Enter a new incomplete draft, choose **Start Fresh**, confirm **Clear draft**, wait for it to finish, and cold relaunch. Check that the reset draft did not return and both saved sessions remain.

An abrupt process kill before a pending write finishes can still lose the uncommitted keystroke. Run the Room migration and UI tests on this disposable fixture only. The GitHub/Gradle build and APK signature checks alone do not prove the on-device migration.

## Confirm replacement without losing input

With one saved fixture workout, enter a draft with notes or a partial set. Choose **Repeat Last Workout** and press **Keep draft**; confirm the original input remains. Choose **Start Fresh** and cancel first, then confirm once and check that history is unchanged. On an empty editor, neither action should ask for confirmation.
