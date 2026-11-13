package io.github.iamanthonycruz.aqui

/** The first-launch walkthrough: one page per permission, in the order Android needs them. */
enum class SetupPage { LOCATION, BACKGROUND, TEXTS, BATTERY }

object Onboarding {
    /** Nothing left to ask on [page]. All-the-time location can't be asked without location, so it's skipped too. */
    fun done(page: SetupPage, s: PermState): Boolean = when (page) {
        SetupPage.LOCATION -> s.location
        SetupPage.BACKGROUND -> s.backgroundLocation || !s.location
        SetupPage.TEXTS -> s.sms
        SetupPage.BATTERY -> s.battery
    }

    /**
     * Whether to open the walkthrough at launch. Its last page (not an emergency service, with a
     * checkbox) shows until ticked once, so people who set up before it existed see just that page.
     */
    fun showAtLaunch(onboarded: Boolean, notEmergencyOk: Boolean, s: PermState): Boolean =
        !notEmergencyOk || (!onboarded && next(s) != null)

    /** The page to show, starting at [from] (a page index), or null when the walkthrough is over. */
    fun next(s: PermState, from: Int = 0): SetupPage? =
        SetupPage.entries.drop(from).firstOrNull { !done(it, s) }
}
