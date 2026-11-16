# Log a workout

## Start and add sets

1. Open **Track**. Enter a custom exercise name or use **Quick add** for a preset lift.
2. Enter weight in **lb** and a positive whole number for reps. Weight can be zero, for example when recording unweighted reps. Weight is the value you enter; the app does not add bars, plates, or bodyweight.
3. Choose **Add blank set** for different values, or **Copy last set** to copy the immediately preceding set for that exercise, if it is complete. The copy is editable independently of the original.
4. Use **Add New Exercise** for another lift and **Workout notes (optional)** for session notes.

Example: enter `95` lb and `8` reps for a squat. Tap **Copy last set**, then change the second row to `6` reps. The first row stays at `95 lb × 8`.

**Repeat Last Workout** starts an editable draft from the latest completed workout’s exercises and sets. It replaces the current draft; the app asks first if the draft contains a name, notes, or any set input. Past sessions are unchanged, and previous notes are not copied. It is not a save.

## Finish explicitly

Dismiss the keyboard to reveal the **Save Workout** bar and tap it when finished. A workout needs at least one named exercise with a valid set. The current app saves the valid named exercises and sets and omits incomplete rows. If you meant to keep working on incomplete rows, leave the draft unsubmitted instead.

History opens after database confirmation. Session cards display the recorded exercises and sets without recalculating them. Open [Strength history](strength-history.md) for per-exercise heaviest sets, logged singles and separate provisional estimates. There is no aggregate training-volume calculation.

## Pause and resume

Track autosaves one draft locally as you edit. Incomplete values such as a weight of `102.` with the reps blank are retained after the write commits. If the app is interrupted, completed autosaves restore the next time it starts. There is no guarantee for uncommitted keystrokes if the app or device process dies before the write finishes.

- Wait until **Saving draft…** is gone if you need the latest inputs to survive a force-stop.
- If the app reports a draft storage issue, keep it open and use the available retry instead of assuming the new edits are already on disk.
- **Start Fresh** clears the current draft, not saved workouts. It asks before clearing any draft with input. On a restore error, **Start fresh without this draft** asks before discarding the unreadable draft only.
- A draft, a repeat, and a copied set do not enter History until you explicitly save the workout.
