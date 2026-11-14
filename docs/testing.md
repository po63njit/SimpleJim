# Test coverage and evidence

## What can be verified

| Check | Where it runs | What it covers | Status for this source snapshot |
| --- | --- | --- | --- |
| `:app:testDebugUnitTest` | JVM | Four set-copy/validation cases, two draft encoding/decoding cases, and four meaningful-input cases for replacement confirmation. | 10 passed. |
| `:app:assembleDebug` | Build host | App compile, resource linking, dexing, and APK package. | Built successfully. Not a phone run. |
| `:app:assembleDebugAndroidTest` | Build host | Compiles and packages three Room migration/transaction cases and one Compose workout smoke case. | Built successfully; instrumentation not executed on this build host. |
| `:app:connectedDebugAndroidTest` | Dedicated device/emulator | Actual Room schema creation/upgrade, rollback, stale writes/reset, and UI copy/edit/save/Activity relaunch. | Not run here. A test device/emulator is needed. |
| Synthetic package, v1→v2 | Android phone | In-place migration and hands-on storage checks after restart, without opening the normal app’s database. | A tester reported the separate fixture passed. This is not a result for personal data or a device run of the 1.1.1 changes below. |
| Earlier normal APK | Android phone | Basic launch/logging. | A tester confirmed it worked. Do not infer broad compatibility, new UI checks, or migration on personal history. |

The optional v2 test APK includes one Compose smoke test that adds a synthetic workout, verifies one matching Room session and cleared draft, and checks History after a new Activity launches. Activity relaunch is not an operating system process restart. Use the separate force-stop procedure to establish that distinction.

The 1.1 debug build introduced draft recovery and retained the Track/History redesign. The 1.1.1 debug build adds the chalk-and-tape **LIFT LOG** styling and asks before replacing a draft with input; the earlier 1.1 build has neither change. The 1.1.1 app and test APKs built and its JVM tests passed. This does not establish an on-phone result for the styling or confirmation dialog.

## Isolated fixture

Use two debug builds with a dedicated package, such as `com.simplejim.tracker.migrationfixture`, an unmistakable launcher label, higher version code on v2, and the same signing identity across the pair. Keep the regular package `com.simplejim.tracker` and its installed history out of the experiment. The fixture is a test configuration derived from the old and new sources, not a second production application.

First install fixture v1, save and cold-reopen a synthetic workout, then install fixture v2 **in place**. Verify it retains that history, restores an incomplete draft after cold launch, commits a workout once and clears its draft, then handles Reset without changing either saved session. The [Android smoke protocol](android-smoke.md) gives the on-device checklist. Do not uninstall the v1 fixture between versions; doing so destroys the very state the upgrade is meant to exercise.

Record the app source or APK hash, fixture version pair, device/Android level, whether the process was force-stopped, checks performed, and any failures. Use synthetic input and avoid uploading personal device logs or records.
