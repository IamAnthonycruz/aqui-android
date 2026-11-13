package com.example.dwellping

/** Which of the app's permissions are granted right now. */
data class PermState(
    /** Precise location. Geofencing needs it; approximate alone is not enough. */
    val location: Boolean,
    val backgroundLocation: Boolean,
    val sms: Boolean,
    /** The user picked "Approximate" in the location prompt. */
    val approximateOnly: Boolean = false,
    /** Battery set to "Unrestricted", so Android doesn't pause tracking. Recommended, not required. */
    val battery: Boolean = true,
) {
    /** Enough to send a text from the app right now. */
    val hasForeground get() = location && sms

    /** Enough for dwell pings to run with the app closed. */
    val hasAll get() = hasForeground && backgroundLocation
}

/** The permission request the checklist's button should launch next. */
enum class PermStep { FOREGROUND, BACKGROUND, BATTERY, OPEN_SETTINGS, NONE }

object PermissionChecklist {
    /**
     * [blockedAt] is the step Android has stopped showing a prompt for (the user said no too
     * often). Asking again would do nothing, so the user has to go to Settings instead.
     */
    fun nextStep(s: PermState, blockedAt: PermStep? = null): PermStep {
        val step = when {
            !s.hasForeground -> PermStep.FOREGROUND
            !s.backgroundLocation -> PermStep.BACKGROUND
            !s.battery -> PermStep.BATTERY
            else -> PermStep.NONE
        }
        return if (step == blockedAt) PermStep.OPEN_SETTINGS else step
    }

    /**
     * Call with the result of a request for [requested]. [canAskAgain] is false when Android
     * would not show the prompt again for a permission that is still missing.
     * Returns the step to pass as `blockedAt` from now on, or null if nothing is blocked.
     */
    fun blockedAfter(requested: PermStep, after: PermState, canAskAgain: Boolean): PermStep? =
        if (!canAskAgain && nextStep(after) == requested) requested else null
}
