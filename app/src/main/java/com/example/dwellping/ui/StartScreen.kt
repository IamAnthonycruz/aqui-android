package com.example.dwellping.ui

import android.text.format.DateFormat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dwellping.Blocker
import com.example.dwellping.Contact
import com.example.dwellping.FirstFix
import com.example.dwellping.People
import com.example.dwellping.PermState
import com.example.dwellping.PersonStatus
import com.example.dwellping.Spot
import com.example.dwellping.Stay
import com.example.dwellping.ui.theme.Alert
import com.example.dwellping.ui.theme.Covered
import com.example.dwellping.ui.theme.DwellPingTheme
import com.example.dwellping.ui.theme.Mist
import com.example.dwellping.ui.theme.OnCovered
import java.util.Date
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val HOLD_TO_STOP_MILLIS = 1000

/** The first screen: the big button and who gets texted. Settings are behind the gear. */
@Composable
fun StartRoute(
    onOpenPerson: (Long) -> Unit,
    onAddPerson: () -> Unit,
    onOpenSettings: () -> Unit,
    vm: StartViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(vm) { vm.messages.collect { snackbar.showSnackbar(it) } }
    LifecycleResumeEffect(vm) {
        vm.refresh()
        onPauseOrDispose { }
    }
    // DwellService writes the stay about once a minute. While this screen is open, re-read it and
    // move the clock on, so the minutes count up even when the stay itself hasn't changed.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.pingsOn) {
        while (state.pingsOn) {
            now = System.currentTimeMillis()
            vm.refresh()
            // Quick while waiting for the first fix, so "0 min here" shows as soon as it lands.
            delay(if (state.stay == null) FirstFix.POLL_MILLIS else 10_000)
        }
    }

    StartScreen(
        state = state,
        now = now,
        snackbar = snackbar,
        onStart = {
            // A blocked Start goes where the fix is, instead of only saying what's wrong.
            if (state.loaded) when (state.blocker) {
                Blocker.NO_PEOPLE -> onAddPerson()
                null -> vm.start()
                else -> onOpenSettings()
            }
        },
        onStop = vm::stop,
        onOpenPerson = onOpenPerson,
        onAddPerson = onAddPerson,
        onOpenSettings = onOpenSettings,
    )

}

@Composable
fun StartScreen(
    state: StartUiState,
    now: Long,
    snackbar: SnackbarHostState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onOpenPerson: (Long) -> Unit,
    onAddPerson: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val blocker = state.blocker.takeIf { state.loaded && !state.pingsOn }
    val minutesHere = state.stay?.minutesAt(now)?.coerceAtLeast(0)
    // Who the ring is counting toward: the next person not yet handled at this stay.
    val handled = state.people.filter { it.status != PersonStatus.Waiting }.map { it.contact.id }.toSet()
    val next = People.next(state.people.map { it.contact }, handled)
    val elapsed = state.stay?.let { (now - it.sinceMillis).coerceAtLeast(0) }
    val progress = if (elapsed == null || state.people.isEmpty()) 0f else People.ringProgress(elapsed, next)
    Box(
        Modifier
            .fillMaxSize()
            .duskBackground()
            .safeDrawingPadding()
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth()) {
                IconButton(onClick = onOpenSettings, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Mist.copy(alpha = 0.8f))
                }
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                BigButton(
                    on = state.pingsOn,
                    needsSetup = blocker != null,
                    minutesHere = minutesHere,
                    progress = progress,
                    onStart = onStart,
                    onStop = onStop,
                )
                Spacer(Modifier.height(24.dp))
                if (state.pingsOn) {
                    val caption = when {
                        elapsed == null -> FirstFix.caption(state.startedAt, now)
                        state.people.isEmpty() -> null
                        next == null -> "Everyone's been texted from here"
                        else -> {
                            val left = next.minutes - (elapsed / 60_000L).toInt()
                            if (left <= 0) "Texting ${next.label}…" else "${next.label} gets a text in ${People.duration(left)}"
                        }
                    }
                    if (caption != null) {
                        Text(caption, color = Mist, fontSize = 17.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                    }
                }
                val hint = when {
                    state.pingsOn -> null // "Hold to stop" is inside the ring.
                    blocker == Blocker.NO_PEOPLE -> "Add someone to text to start"
                    blocker == Blocker.NO_PERMISSIONS -> "Allow location and SMS to start"
                    else -> null
                }
                if (blocker == Blocker.NO_PEOPLE) {
                    // First run: say what the app does before asking for anything.
                    Text(
                        "Aqui! texts the people you pick when you stay in one place for a while.",
                        color = Mist.copy(alpha = 0.7f),
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                }
                if (hint != null) {
                    Text(
                        hint,
                        color = if (state.pingsOn) Mist.copy(alpha = 0.55f) else Mist,
                        fontSize = if (state.pingsOn) 15.sp else 18.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            PeoplePanel(
                rows = state.people,
                pingsOn = state.pingsOn,
                onOpenPerson = onOpenPerson,
                onAdd = onAddPerson,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            )
        }

        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun PeoplePanel(
    rows: List<PersonRow>,
    pingsOn: Boolean,
    onOpenPerson: (Long) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Capped and scrolling, so a long list can't push the big button off the screen.
    Box(modifier.heightIn(max = 340.dp).clip(RoundedCornerShape(24.dp))) {
    DuskPanel(Modifier.verticalScroll(rememberScrollState()), padding = false) {
        rows.forEach { row ->
            PersonRow(
                contact = row.contact,
                // Statuses describe the current stay, so they only mean something while running.
                status = if (pingsOn) row.status else PersonStatus.Waiting,
                onClick = { onOpenPerson(row.contact.id) },
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onAdd)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Mist.copy(alpha = 0.10f)),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = Mist)
            }
            Spacer(Modifier.width(14.dp))
            Text("Add person", color = Mist, fontSize = 16.sp)
        }
    }
    }
}

@Composable
private fun PersonRow(contact: Contact, status: PersonStatus, onClick: () -> Unit) {
    val context = LocalContext.current
    fun time(millis: Long) = DateFormat.getTimeFormat(context).format(Date(millis))
    val texted = status is PersonStatus.Texted
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(40.dp).clip(CircleShape)
                .background(if (texted) Covered else Mist.copy(alpha = 0.16f)),
        ) {
            Text(
                contact.name.trim().ifBlank { "#" }.take(1).uppercase(),
                color = if (texted) OnCovered else Mist,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(contact.label, color = Mist, fontSize = 16.sp)
            val sub = listOfNotNull(contact.phone.takeIf { contact.name.isNotBlank() })
            if (sub.isNotEmpty()) {
                Text(
                    sub.joinToString(" · "),
                    color = Mist.copy(alpha = 0.55f),
                    fontSize = 13.sp,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        // Before their text: their own timer. After: what happened, until the next stop.
        val (text, color) = when (status) {
            PersonStatus.Waiting -> "after ${contact.minutes} min" to Mist.copy(alpha = 0.7f)
            is PersonStatus.Texted -> "Texted ${time(status.atMillis)}" to Covered
            is PersonStatus.Failed -> "Failed ${time(status.atMillis)}" to Alert
            is PersonStatus.NotSent -> "Not sent: ${status.reason}" to Mist.copy(alpha = 0.7f)
        }
        // Capped so a long status ("Not sent: texted here recently") wraps instead of squeezing the name.
        Text(
            text,
            color = color,
            fontSize = 14.sp,
            fontWeight = if (status is PersonStatus.Waiting) FontWeight.Normal else FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(max = 130.dp),
        )
    }
}

/**
 * Off: a ring outline with a play icon; tap to start.
 * On: a ring that fills toward the next person's text ([progress]); hold for a second to stop, so a
 * tap in a pocket can't switch off the check-in without you noticing. A second ring fills while you hold.
 */
@Composable
private fun BigButton(
    on: Boolean,
    needsSetup: Boolean,
    /** Minutes at the current stay, or null when off or before the first fix. */
    minutesHere: Int?,
    /** How full the ring is, 0..1. */
    progress: Float,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val hold = remember { Animatable(0f) }
    val shown by animateFloatAsState(if (on) progress else 0f, tween(800), label = "ring")

    val label = if (needsSetup) "Set up" else "Press to start"

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(250.dp)
            .clip(CircleShape)
            .semantics {
                contentDescription = when {
                    on && minutesHere != null -> "On, ${People.duration(minutesHere)} here. Hold to stop"
                    on -> "On. Hold to stop"
                    else -> label
                }
                if (on) onLongClick { onStop(); true } else onClick { onStart(); true }
            }
            .pointerInput(on) {
                detectTapGestures(
                    onTap = { if (!on) onStart() },
                    onPress = {
                        if (!on) return@detectTapGestures
                        val fill = scope.launch {
                            hold.animateTo(1f, tween(HOLD_TO_STOP_MILLIS, easing = LinearEasing))
                            hold.snapTo(0f)
                            onStop()
                        }
                        tryAwaitRelease()
                        // Let go early: cancel, and the ring drains back.
                        if (fill.isActive) {
                            fill.cancel()
                            scope.launch { hold.animateTo(0f, tween(200)) }
                        }
                    },
                )
            },
    ) {
        val track = Mist
        val fillColor = Covered
        Canvas(Modifier.fillMaxSize()) {
            val ring = 14.dp.toPx()
            val inset = ring / 2
            val arcSize = Size(size.width - ring, size.height - ring)
            drawCircle(track.copy(alpha = 0.05f), radius = size.minDimension / 2 - ring)
            drawArc(
                color = track.copy(alpha = if (on) 0.12f else 0.35f),
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize,
                style = Stroke(width = if (on) ring else 3.dp.toPx()),
            )
            if (shown > 0f) {
                drawArc(
                    color = fillColor,
                    startAngle = -90f, sweepAngle = 360f * shown, useCenter = false,
                    topLeft = Offset(inset, inset), size = arcSize,
                    style = Stroke(width = ring, cap = StrokeCap.Round),
                )
            }
            if (hold.value > 0f) {
                // Inside the main ring, so the progress stays readable while you hold.
                val holdInset = ring * 2
                drawArc(
                    color = track.copy(alpha = 0.9f),
                    startAngle = -90f, sweepAngle = 360f * hold.value, useCenter = false,
                    topLeft = Offset(holdInset, holdInset),
                    size = Size(size.width - 2 * holdInset, size.height - 2 * holdInset),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }
        when {
            on && minutesHere != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val (count, unit) = People.durationParts(minutesHere)
                Text(count, color = Mist, fontSize = 76.sp, fontWeight = FontWeight.SemiBold)
                Text("$unit here", color = Mist.copy(alpha = 0.7f), fontSize = 18.sp, fontWeight = FontWeight.Medium)
                HoldToStop()
            }
            on -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.MyLocation, contentDescription = null, tint = Mist.copy(alpha = 0.8f), modifier = Modifier.size(48.dp))
                HoldToStop()
            }
            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    if (needsSetup) Icons.Rounded.Tune else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Mist,
                    modifier = Modifier.size(64.dp),
                )
                Text(label, color = Mist, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}


/** The small hint at the bottom of the ring while it's on. */
@Composable
private fun HoldToStop() {
    Spacer(Modifier.height(14.dp))
    Text("Hold to stop", color = Mist.copy(alpha = 0.5f), fontSize = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp)
}

private val previewPeople = listOf(
    PersonRow(Contact(id = 1, name = "Mom", phone = "+1 404-555-0132", minutes = 30), PersonStatus.Waiting),
    PersonRow(Contact(id = 2, name = "Alex", phone = "+1 678-555-0199", minutes = 10), PersonStatus.Texted(5 * 60_000L)),
)

@Preview(name = "Off", widthDp = 393, heightDp = 851)
@Composable
private fun StartOffPreview() {
    DwellPingTheme {
        StartScreen(
            state = StartUiState(people = previewPeople, perms = PermState(true, true, true), loaded = true),
            now = 0L, snackbar = remember { SnackbarHostState() },
            onStart = {}, onStop = {}, onOpenPerson = {}, onAddPerson = {}, onOpenSettings = {},
        )
    }
}

@Preview(name = "On", widthDp = 393, heightDp = 851)
@Composable
private fun StartOnPreview() {
    DwellPingTheme {
        StartScreen(
            state = StartUiState(
                people = previewPeople, perms = PermState(true, true, true), pingsOn = true,
                stay = Stay(Spot(33.8, -84.7), sinceMillis = 0L), loaded = true,
            ),
            now = 12 * 60_000L, snackbar = remember { SnackbarHostState() },
            onStart = {}, onStop = {}, onOpenPerson = {}, onAddPerson = {}, onOpenSettings = {},
        )
    }
}
