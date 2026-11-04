package com.hermesandroid.relay.data

/**
 * Reasoning block durations, kept for the life of the process.
 *
 * The number exists nowhere but in the watching: the gateway stores the text a
 * model thought, never how long it took. Desktop solved the same gap by holding
 * its measurements in a module-level map (see its `activity-timer.ts`), because
 * the transcript virtualizes and the component that watched a block finish is
 * usually gone by the time anyone scrolls back to it.
 *
 * Keyed by the block's stable identity (message plus segment index), so a block
 * that scrolls out and back reports the same total instead of restarting at 0.
 */
object ReasoningDurations {

    private val measured = mutableMapOf<String, Int>()

    /** Last duration measured for [key], or null when it was never watched. */
    fun get(key: String): Int? = measured[key]

    /** Remember the running total for [key] (called once a second while live). */
    fun record(key: String, seconds: Int) {
        if (seconds > 0) measured[key] = seconds
    }

    /** Test seam: forget every measurement. */
    fun clear() = measured.clear()
}
