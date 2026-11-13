package io.github.iamanthonycruz.aqui.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.iamanthonycruz.aqui.Onboarding
import io.github.iamanthonycruz.aqui.Perms
import io.github.iamanthonycruz.aqui.Prefs

/**
 * Welcome: first launch only. One page per permission, saying why, then Start.
 * Then three screens:
 *  - Start: the big button and the people list.
 *  - Person: add or edit one person (number, timer, message). [NEW_PERSON] = add.
 *  - Settings: permissions. Behind the gear on Start.
 */
private const val WELCOME = "welcome"
private const val START = "start"
private const val SETTINGS = "settings"
private const val PERSON = "person/{id}"
const val NEW_PERSON = -1L

private fun person(id: Long) = "person/$id"

@Composable
fun AquiApp() {
    val nav = rememberNavController()
    val context = LocalContext.current
    val first = remember {
        if (!Prefs.isOnboarded(context) && Onboarding.next(Perms.state(context)) != null) WELCOME else START
    }
    NavHost(navController = nav, startDestination = first) {
        composable(WELCOME) {
            WelcomeRoute(onDone = {
                Prefs.setOnboarded(context)
                nav.navigate(START) { popUpTo(WELCOME) { inclusive = true } }
            })
        }
        composable(START) {
            StartRoute(
                onOpenPerson = { nav.navigate(person(it)) },
                onAddPerson = { nav.navigate(person(NEW_PERSON)) },
                onOpenSettings = { nav.navigate(SETTINGS) },
            )
        }
        composable(PERSON, arguments = listOf(navArgument("id") { type = NavType.LongType })) {
            PersonRoute(onDone = { nav.popBackStack() })
        }
        composable(SETTINGS) {
            SettingsRoute(onBack = { nav.popBackStack() })
        }
    }
}
