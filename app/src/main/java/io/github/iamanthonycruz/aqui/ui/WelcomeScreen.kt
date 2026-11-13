package io.github.iamanthonycruz.aqui.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.iamanthonycruz.aqui.Onboarding
import io.github.iamanthonycruz.aqui.PermState
import io.github.iamanthonycruz.aqui.Perms
import io.github.iamanthonycruz.aqui.SetupPage
import io.github.iamanthonycruz.aqui.ui.theme.Alert
import io.github.iamanthonycruz.aqui.ui.theme.Covered
import io.github.iamanthonycruz.aqui.ui.theme.AquiTheme
import io.github.iamanthonycruz.aqui.ui.theme.Mist

/** What one walkthrough page says. */
private data class PageText(
    val icon: ImageVector,
    val title: String,
    val why: String,
    val button: String,
    /** What to expect on Android's own screen. */
    val hint: String,
    /** Shown after a "no": what won't work without it. */
    val without: String,
)

private fun textFor(page: SetupPage) = when (page) {
    SetupPage.LOCATION -> PageText(
        Icons.Rounded.LocationOn,
        "Know where you are",
        "Aqui! checks your spot about once a minute, so it can tell when you've stayed somewhere.",
        "Allow location",
        "Choose \"Precise\" and \"While using the app\".",
        "Without location, Aqui! can't tell where you are.",
    )
    SetupPage.BACKGROUND -> PageText(
        Icons.Rounded.Update,
        "Keep going with the app closed",
        "So your people still get their text while your phone is in your pocket.",
        "Allow all the time",
        "On the next screen, choose \"Allow all the time\", then go back.",
        "Without it, texts only go out while Aqui! is open.",
    )
    SetupPage.TEXTS -> PageText(
        Icons.Rounded.Sms,
        "Text from your number",
        "Aqui! sends texts from your own phone, only to the people you add.",
        "Allow texts",
        "Android may also ask about phone calls (it lets long texts go as one; Aqui! never calls) and notifications (shows when Aqui! is on).",
        "Without it, Aqui! can't text anyone.",
    )
    SetupPage.BATTERY -> PageText(
        Icons.Rounded.BatteryFull,
        "Don't get paused",
        "Some phones pause apps to save battery. That would quietly stop your check-ins.",
        "Allow unrestricted",
        "On the next screen, tap Allow.",
        "Some phones may pause Aqui! after a while.",
    )
}

/** What each page's button asks Android for. Battery is a dialog, not a permission. */
private fun permissionsFor(page: SetupPage): Array<String> = when (page) {
    SetupPage.LOCATION -> arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    SetupPage.BACKGROUND -> arrayOf(Perms.BACKGROUND_REQUEST)
    SetupPage.TEXTS -> listOfNotNull(
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.POST_NOTIFICATIONS.takeIf { Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU },
    ).toTypedArray()
    SetupPage.BATTERY -> emptyArray()
}

/** The permission on [page] that the walkthrough can't continue without. */
private fun mainPermission(page: SetupPage): String? = when (page) {
    SetupPage.LOCATION -> Manifest.permission.ACCESS_FINE_LOCATION
    SetupPage.BACKGROUND -> Perms.BACKGROUND_REQUEST
    SetupPage.TEXTS -> Manifest.permission.SEND_SMS
    SetupPage.BATTERY -> null
}

/** First launch: one page per permission, each saying why. Already-allowed pages are skipped. */
@Composable
fun WelcomeRoute(onDone: () -> Unit) {
    val context = LocalContext.current
    var perms by remember { mutableStateOf(Perms.state(context)) }
    /** Index of the page we're on or past. Skipping or allowing moves it on. */
    var from by rememberSaveable { mutableIntStateOf(0) }
    /** The current page got a "no". */
    var refused by rememberSaveable { mutableStateOf(false) }
    /** Android won't show the prompt again, so the button opens app settings instead. */
    var blocked by rememberSaveable { mutableStateOf(false) }

    // Back from app settings or Android's own screens: re-read, so a granted page moves on.
    LifecycleResumeEffect(Unit) {
        perms = Perms.state(context)
        onPauseOrDispose { }
    }

    val page = Onboarding.next(perms, from)
    /** The first page this walkthrough showed: it carries the not-an-emergency-service note. */
    val firstPage by rememberSaveable { mutableIntStateOf(page?.ordinal ?: 0) }
    LaunchedEffect(page) {
        if (page == null) onDone()
        else if (page.ordinal != from) { from = page.ordinal; refused = false; blocked = false }
    }
    if (page == null) return

    fun afterAnswer() {
        perms = Perms.state(context)
        if (Onboarding.done(page, perms)) {
            from = page.ordinal + 1
            refused = false
            blocked = false
        } else {
            refused = true
            val p = mainPermission(page)
            blocked = p != null && context.findActivity()?.shouldShowRequestPermissionRationale(p) == false
        }
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { afterAnswer() }
    val batteryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { afterAnswer() }

    @SuppressLint("BatteryLife") // A safety check-in that must keep running is the case this dialog is for.
    fun ask() {
        when {
            blocked -> {
                Toast.makeText(context, "Tap Permissions and turn on what's missing", Toast.LENGTH_LONG).show()
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.fromParts("package", context.packageName, null))
                )
            }
            page == SetupPage.BATTERY -> batteryLauncher.launch(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData(Uri.fromParts("package", context.packageName, null))
            )
            else -> {
                // Android's "all the time" screen is a settings page with no explanation of its own.
                if (page == SetupPage.BACKGROUND) {
                    Toast.makeText(context, "Choose \"Allow all the time\", then go back", Toast.LENGTH_LONG).show()
                }
                permLauncher.launch(permissionsFor(page))
            }
        }
    }

    WelcomeScreen(
        page = page,
        perms = perms,
        refused = refused,
        blocked = blocked,
        showNotice = page.ordinal == firstPage,
        onAllow = ::ask,
        onSkip = { from = page.ordinal + 1; refused = false; blocked = false },
    )
}

@Composable
fun WelcomeScreen(
    page: SetupPage,
    perms: PermState,
    refused: Boolean,
    blocked: Boolean,
    onAllow: () -> Unit,
    onSkip: () -> Unit,
    showNotice: Boolean = false,
) {
    val t = textFor(page)
    val without = if (page == SetupPage.LOCATION && perms.approximateOnly) {
        "Only approximate is on. Aqui! needs precise location to tell one spot from the next."
    } else t.without
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .duskBackground()
            .safeDrawingPadding()
            .padding(horizontal = 28.dp, vertical = 20.dp),
    ) {
        Spacer(Modifier.weight(1f))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(112.dp).clip(CircleShape).background(Mist.copy(alpha = 0.10f)),
        ) {
            Icon(t.icon, contentDescription = null, tint = Mist, modifier = Modifier.size(56.dp))
        }
        Spacer(Modifier.height(32.dp))
        Text(t.title, color = Mist, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        Text(
            t.why,
            color = Mist.copy(alpha = 0.75f), fontSize = 17.sp, lineHeight = 24.sp, textAlign = TextAlign.Center,
        )
        if (showNotice) {
            Spacer(Modifier.height(20.dp))
            Text(
                NOT_EMERGENCY,
                color = Mist.copy(alpha = 0.6f), fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.weight(1f))

        if (refused) {
            Text(
                without,
                color = Alert, fontSize = 15.sp, lineHeight = 21.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 14.dp),
            )
        }
        PrimaryButton(
            text = when {
                blocked -> "Open app settings"
                refused -> "Try again"
                else -> t.button
            },
            onClick = onAllow,
            icon = if (blocked) Icons.Rounded.Settings else t.icon,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            if (blocked) "Android won't ask again. Turn it on under Permissions." else t.hint,
            color = Mist.copy(alpha = 0.55f), fontSize = 13.sp, lineHeight = 18.sp, textAlign = TextAlign.Center,
        )
        TextButton(onClick = onSkip) {
            Text("Skip for now", color = Mist.copy(alpha = 0.6f), fontSize = 15.sp)
        }
        Spacer(Modifier.height(8.dp))
        StepDots(current = page.ordinal, total = SetupPage.entries.size)
    }
}

/** Shown once, on the first setup page: the app is a convenience, not a safety guarantee. */
const val NOT_EMERGENCY =
    "Aqui! helps people know where you are, but it's not an emergency service. " +
        "Texts can be late or fail. In an emergency, call 911."

@Composable
private fun StepDots(current: Int, total: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(total) { i ->
                Box(
                    Modifier.size(8.dp).clip(CircleShape)
                        .background(if (i == current) Covered else Mist.copy(alpha = if (i < current) 0.6f else 0.2f))
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Step ${current + 1} of $total", color = Mist.copy(alpha = 0.5f), fontSize = 13.sp)
    }
}

@Preview(widthDp = 393, heightDp = 851)
@Composable
private fun WelcomePreview() {
    AquiTheme {
        WelcomeScreen(
            page = SetupPage.LOCATION,
            perms = PermState(location = false, backgroundLocation = false, sms = false, battery = false),
            refused = false, blocked = false, onAllow = {}, onSkip = {}, showNotice = true,
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
