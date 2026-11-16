package com.simplejim.tracker.strength

/**
 * The main-thread ViewModel owns the gate. A queued workout save invalidates older reads,
 * and no fresh read can publish until the save finishes (successfully or otherwise).
 */
internal class StrengthSnapshotGate {
    private var generation = 0L
    private var saving = false

    fun beginWorkoutSave() {
        saving = true
        invalidate()
    }

    fun finishWorkoutSave() {
        saving = false
        invalidate()
    }

    fun beginLoad(): Long? = if (saving) null else ++generation

    fun invalidate() {
        generation++
    }

    fun isCurrent(ticket: Long): Boolean = !saving && generation == ticket
}
