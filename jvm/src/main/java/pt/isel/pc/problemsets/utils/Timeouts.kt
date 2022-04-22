package pt.isel.pc.problemsets.utils

import java.util.concurrent.TimeUnit

object Timeouts {
    fun noWait(timeout: Long): Boolean {
        return timeout <= 0
    }

    /**
     * Returns a [long] representing the deadline for a timeout.
     *
     * @param duration the timeout duration.
     * @param timeUnit the duration unit.
     * @return the deadline for the timeout.
     */
    fun deadlineFor(duration: Long, timeUnit: TimeUnit): Long {
        return deadlineFor(timeUnit.toMillis(duration))
    }

    fun deadlineFor(timeout: Long): Long {
        return System.currentTimeMillis() + timeout
    }

    fun now(): Long {
        return System.currentTimeMillis()
    }

    /**
     * Returns the amount of milliseconds remaining for the timeout deadline.
     *
     * @param deadline the timeout deadline
     * @return the amount of milliseconds remaining for the timeout deadline.
     */
    fun remainingUntil(deadline: Long): Long {
        return deadline - System.currentTimeMillis()
    }

    /**
     * Checks if the timeout deadline was already reached
     *
     * @param remaining the remaining time for the timeout's deadline
     * @return `true` if the timeout was reached, `false` otherwise.
     */
    fun isTimeout(remaining: Long): Boolean {
        return remaining <= 0
    }
}