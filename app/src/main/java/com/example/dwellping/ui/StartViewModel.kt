package com.example.dwellping.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dwellping.ActionRules
import com.example.dwellping.Blocker
import com.example.dwellping.Contact
import com.example.dwellping.DwellService
import com.example.dwellping.People
import com.example.dwellping.PermState
import com.example.dwellping.Perms
import com.example.dwellping.PersonStatus
import com.example.dwellping.PingLog
import com.example.dwellping.Prefs
import com.example.dwellping.Stay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PersonRow(val contact: Contact, val status: PersonStatus)

data class StartUiState(
    val people: List<PersonRow> = emptyList(),
    val pingsOn: Boolean = false,
    val perms: PermState = PermState(location = false, backgroundLocation = false, sms = false),
    /** When Start was tapped in this screen's lifetime, or null if unknown (e.g. the app was reopened). */
    val startedAt: Long? = null,
    /** The stay DwellService is counting. Null when off or before its first fix. */
    val stay: Stay? = null,
    /** False until the people list has been read once, so "Add someone" doesn't flash. */
    val loaded: Boolean = false,
) {
    /** Why Start can't run yet, or null. */
    val blocker: Blocker? get() = ActionRules.startBlocker(people.size, perms)
}

/** The flags the service and Settings change, re-read on resume and every few seconds. */
private data class Flags(
    val pingsOn: Boolean,
    val perms: PermState,
    val stay: Stay?,
    val startedAt: Long?,
)

@OptIn(ExperimentalCoroutinesApi::class)
class StartViewModel(app: Application) : AndroidViewModel(app) {

    private val app get() = getApplication<Application>()

    /** When Start was last tapped here. Kept in memory only: after a reopen it's unknown. */
    private var startedAt: Long? = null

    private val flags = MutableStateFlow(readFlags())

    /** Ping-log rows since the stay began. Room re-emits when the service logs a new one. */
    private val events = flags.map { it.stay?.sinceMillis }.distinctUntilChanged().flatMapLatest { since ->
        if (since == null) flowOf(emptyList()) else PingLog.dao(app).since(since)
    }

    val state: StateFlow<StartUiState> =
        combine(PingLog.contacts(app).all(), events, flags) { contacts, events, f ->
            StartUiState(
                people = contacts.map { PersonRow(it, People.status(it, events)) },
                pingsOn = f.pingsOn,
                perms = f.perms,
                startedAt = f.startedAt,
                stay = f.stay,
                loaded = true,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StartUiState())

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    private fun say(msg: String) {
        _messages.trySend(msg)
    }

    private fun readFlags(): Flags {
        val on = Prefs.isEnabled(app)
        return Flags(
            pingsOn = on,
            perms = Perms.state(app),
            stay = if (on) Prefs.stay(app) else null,
            startedAt = if (on) startedAt else null,
        )
    }

    /** Call on resume and while the screen is open: the service moves the stay on about once a minute. */
    fun refresh() {
        flags.value = readFlags()
    }

    /** Call only when [StartUiState.blocker] is null; the screen routes the other cases. */
    fun start() {
        Prefs.setEnabled(app, true)
        Prefs.setStay(app, null) // Count fresh from now.
        startedAt = System.currentTimeMillis()
        DwellService.start(app)
        refresh()
        say("Running. You can close the app.")
    }

    fun stop() {
        Prefs.setEnabled(app, false)
        DwellService.stop(app)
        refresh()
        say("Stopped. No more texts.")
    }
}
