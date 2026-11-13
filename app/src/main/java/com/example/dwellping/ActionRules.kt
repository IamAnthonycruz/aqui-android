package com.example.dwellping

/** Why an action can't run yet. */
enum class Blocker(val message: String) {
    NO_PEOPLE("Add someone to text first"),
    NO_NUMBER("Enter their number first"),
    NO_PERMISSIONS("Allow location and SMS in Settings first"),
    TOO_SHORT("The timer must be at least ${Config.MIN_MINUTES} min"),
}

/** Checks run before each action. Null means go ahead. */
object ActionRules {
    /** Dwell pings run in the background, so Start needs every permission. */
    fun startBlocker(people: Int, perms: PermState): Blocker? = when {
        people == 0 -> Blocker.NO_PEOPLE
        !perms.hasAll -> Blocker.NO_PERMISSIONS
        else -> null
    }

    /** The test text goes out right now from the app, so it doesn't need all-the-time location. */
    fun testTextBlocker(phone: String, perms: PermState): Blocker? = when {
        phone.isBlank() -> Blocker.NO_NUMBER
        !perms.hasForeground -> Blocker.NO_PERMISSIONS
        else -> null
    }

    fun saveBlocker(phone: String, minutes: Int): Blocker? = when {
        phone.isBlank() -> Blocker.NO_NUMBER
        minutes < Config.MIN_MINUTES -> Blocker.TOO_SHORT
        else -> null
    }

}
