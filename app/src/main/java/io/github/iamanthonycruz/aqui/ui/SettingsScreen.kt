package io.github.iamanthonycruz.aqui.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.iamanthonycruz.aqui.ui.theme.Covered
import io.github.iamanthonycruz.aqui.ui.theme.Mist
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.iamanthonycruz.aqui.PermState
import io.github.iamanthonycruz.aqui.PermStep
import io.github.iamanthonycruz.aqui.Perms
import io.github.iamanthonycruz.aqui.ui.theme.Alert
import io.github.iamanthonycruz.aqui.ui.theme.AquiTheme

/** Permissions, wired to the system permission dialogs. */
@Composable
fun SettingsRoute(onBack: () -> Unit, vm: SettingsViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    LifecycleResumeEffect(vm) {
        vm.refresh()
        onPauseOrDispose { }
    }

    // A Toast, not the snackbar: these hints must still show on top of the system permission screen.
    fun hint(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

    // Android stops showing a prompt once the user has said no too often. Detect that, so the
    // button can send them to Settings instead of silently doing nothing.
    fun canAskAgain(step: PermStep): Boolean {
        val activity = context.findActivity() ?: return true
        return Perms.missingFor(context, step).all { activity.shouldShowRequestPermissionRationale(it) }
    }

    // Once a button tap starts the chain, each step leads to the next, and the last one goes back
    // to Start. Opening Settings from the gear when all is set doesn't bounce you out.
    var batteryAsked by remember { mutableStateOf(false) }

    fun finishIfDone() {
        if (vm.state.value.perms.hasAll) {
            hint("All set. Tap Start.")
            onBack()
        }
    }

    val batteryPrompt = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        vm.refresh()
        // Recommended, not required: back to Start even if they said no.
        finishIfDone()
    }

    @SuppressLint("BatteryLife") // A safety check-in that must keep running is the case this dialog is for.
    fun requestBattery() {
        batteryAsked = true
        batteryPrompt.launch(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(Uri.fromParts("package", context.packageName, null))
        )
    }

    /** After a step's result: ask for what's next, or go back to Start when nothing is. */
    fun continueChain() {
        when (vm.state.value.permStep) {
            PermStep.BATTERY -> if (!batteryAsked) requestBattery() else finishIfDone()
            PermStep.NONE -> finishIfDone()
            else -> Unit // Something was refused: stay here, the button shows what's left.
        }
    }

    val backgroundPerm = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        vm.onPermissionResult(PermStep.BACKGROUND, canAskAgain(PermStep.BACKGROUND))
        continueChain()
    }

    fun requestBackground(hintText: String) {
        hint(hintText)
        backgroundPerm.launch(Perms.BACKGROUND_REQUEST)
    }

    // Two steps, as Android requires: location + SMS first, then "Allow all the time".
    val foregroundPerms = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        vm.onPermissionResult(PermStep.FOREGROUND, canAskAgain(PermStep.FOREGROUND))
        if (vm.state.value.permStep == PermStep.BACKGROUND) {
            requestBackground("Next screen: choose \"Allow all the time\"")
        } else {
            continueChain()
        }
    }

    SettingsScreen(
        state = state,
        snackbar = snackbar,
        onBack = onBack,
        onFixPermissions = {
            when (state.permStep) {
                PermStep.FOREGROUND -> foregroundPerms.launch(Perms.FOREGROUND_REQUEST)
                PermStep.BACKGROUND -> requestBackground("Choose \"Allow all the time\"")
                PermStep.BATTERY -> requestBattery()
                PermStep.OPEN_SETTINGS -> {
                    hint(
                        if (state.perms.approximateOnly) "Tap Permissions > Location, then turn on \"Use precise location\""
                        else "Tap Permissions and turn on what's missing"
                    )
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(Uri.fromParts("package", context.packageName, null))
                    )
                }
                PermStep.NONE -> Unit
            }
        },
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onFixPermissions: () -> Unit,
) {
    val perms = state.perms
    val missing = listOf(perms.location, perms.backgroundLocation, perms.sms, perms.battery).count { !it }
    DuskScaffold(title = "Settings", onBack = onBack, snackbar = snackbar) {
        StatusHeader(missing)

        Column {
            FieldLabel("Permissions")
            DuskPanel(padding = false) {
                PermRow(
                    icon = Icons.Rounded.LocationOn,
                    title = "Location",
                    why = if (perms.approximateOnly) "Only approximate is on. Turn on precise location."
                    else "Finds the spot you're at",
                    done = perms.location,
                )
                RowDivider()
                PermRow(
                    icon = Icons.Rounded.Update,
                    title = "Location all the time",
                    why = "Lets texts go out with the app closed",
                    done = perms.backgroundLocation,
                )
                RowDivider()
                PermRow(
                    icon = Icons.Rounded.Sms,
                    title = "Send texts",
                    why = "Texts your people from your own number",
                    done = perms.sms,
                )
                RowDivider()
                PermRow(
                    icon = Icons.Rounded.BatteryFull,
                    title = "Battery unrestricted",
                    why = "Stops Android from pausing Aqui! to save battery",
                    done = perms.battery,
                    badge = "Recommended",
                )
            }
        }

        when (state.permStep) {
            PermStep.FOREGROUND -> PrimaryButton("Allow location and texts", onFixPermissions, icon = Icons.Rounded.LocationOn)
            PermStep.BACKGROUND -> Column {
                PrimaryButton("Allow location all the time", onFixPermissions, icon = Icons.Rounded.Update)
                FieldHint("On the next screen, choose \"Allow all the time\".")
            }
            PermStep.BATTERY -> Column {
                PrimaryButton("Let Aqui! run unrestricted", onFixPermissions, icon = Icons.Rounded.BatteryFull)
                FieldHint("On the next screen, tap Allow. Without it, some phones pause texts after a while.")
            }
            PermStep.OPEN_SETTINGS -> Column {
                PrimaryButton("Open app settings", onFixPermissions, icon = Icons.Rounded.Settings)
                FieldHint("Android won't ask again. Turn on what's missing under Permissions.")
            }
            PermStep.NONE -> Unit
        }

    }
}

/** Big icon and one line: all good, or how many things are left. */
@Composable
private fun StatusHeader(missing: Int) {
    val ok = missing == 0
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background((if (ok) Covered else Alert).copy(alpha = 0.16f)),
        ) {
            Icon(
                if (ok) Icons.Rounded.VerifiedUser else Icons.Rounded.Shield,
                contentDescription = null,
                tint = if (ok) Covered else Alert,
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                if (ok) "You're all set" else if (missing == 1) "1 thing to allow" else "$missing things to allow",
                color = Mist, fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
            )
            Text(
                if (ok) "Aqui! can text your people with the app closed."
                else "Aqui! needs these to text your people while you're out.",
                color = Mist.copy(alpha = 0.6f), fontSize = 14.sp, lineHeight = 19.sp,
            )
        }
    }
}

@Composable
private fun PermRow(icon: ImageVector, title: String, why: String, done: Boolean, badge: String = "Needed") {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(40.dp).clip(CircleShape).background(Mist.copy(alpha = 0.10f)),
        ) {
            Icon(icon, contentDescription = null, tint = Mist, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Mist, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(why, color = Mist.copy(alpha = 0.55f), fontSize = 13.sp, lineHeight = 18.sp)
        }
        Spacer(Modifier.width(12.dp))
        if (done) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = "Allowed", tint = Covered, modifier = Modifier.size(24.dp))
        } else {
            Text(
                badge,
                color = Alert,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Alert.copy(alpha = 0.14f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun RowDivider() {
    Box(Modifier.padding(start = 70.dp).fillMaxWidth().height(1.dp).background(Mist.copy(alpha = 0.08f)))
}

@Preview(widthDp = 393, heightDp = 900)
@Composable
private fun SettingsScreenPreview() {
    AquiTheme {
        SettingsScreen(
            state = SettingsUiState(perms = PermState(location = true, backgroundLocation = false, sms = true)),
            snackbar = remember { SnackbarHostState() },
            onBack = {}, onFixPermissions = {},
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
