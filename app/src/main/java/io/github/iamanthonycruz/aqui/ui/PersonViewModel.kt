package io.github.iamanthonycruz.aqui.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import io.github.iamanthonycruz.aqui.ActionRules
import io.github.iamanthonycruz.aqui.Config
import io.github.iamanthonycruz.aqui.Contact
import io.github.iamanthonycruz.aqui.Locations
import io.github.iamanthonycruz.aqui.Message
import io.github.iamanthonycruz.aqui.People
import io.github.iamanthonycruz.aqui.Perms
import io.github.iamanthonycruz.aqui.PingLog
import io.github.iamanthonycruz.aqui.SmsSender
import io.github.iamanthonycruz.aqui.Spot
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The person as typed. Saved only on Save. */
data class PersonUiState(
    val isNew: Boolean,
    val name: String = "",
    val phone: String = "",
    val minutes: Int = Config.DEFAULT_MINUTES,
    val template: String = "",
    /** Gets one text with your last spot when the battery gets low. */
    val lowBattery: Boolean = false,
    /** False until an existing person has been read, so the fields don't start empty. */
    val loaded: Boolean = isNew,
)

class PersonViewModel(app: Application, saved: SavedStateHandle) : AndroidViewModel(app) {

    private val app get() = getApplication<Application>()
    private val id: Long = saved["id"] ?: NEW_PERSON
    private val dao = PingLog.contacts(app)

    private val _state = MutableStateFlow(PersonUiState(isNew = id == NEW_PERSON))
    val state: StateFlow<PersonUiState> = _state.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    /** Fires once after Save or Delete, so the screen can close. */
    private val _done = Channel<Unit>(Channel.CONFLATED)
    val done: Flow<Unit> = _done.receiveAsFlow()

    init {
        if (id != NEW_PERSON) viewModelScope.launch {
            val c = dao.byId(id)
            if (c == null) { _done.trySend(Unit); return@launch } // Deleted meanwhile.
            _state.value = PersonUiState(
                isNew = false, name = c.name, phone = c.phone, minutes = c.minutes,
                template = c.template, lowBattery = c.lowBattery, loaded = true,
            )
        }
    }

    fun onName(v: String) = _state.update { it.copy(name = v) }
    fun onPhone(v: String) = _state.update { it.copy(phone = v) }

    /** From the contact picker: take the number, and the name only if none is typed yet. */
    fun onPicked(name: String, phone: String) = _state.update {
        it.copy(phone = phone, name = it.name.ifBlank { name })
    }
    fun onLowBattery(v: Boolean) = _state.update { it.copy(lowBattery = v) }
    fun onTemplate(v: String) = _state.update { it.copy(template = v) }
    fun stepMinutes(up: Boolean) = _state.update { it.copy(minutes = People.stepMinutes(it.minutes, up)) }

    fun save() {
        val s = _state.value
        ActionRules.saveBlocker(s.phone.trim(), s.minutes)?.let { say(it.message); return }
        val contact = Contact(
            id = if (s.isNew) 0 else id,
            name = s.name.trim(),
            phone = s.phone.trim(),
            minutes = s.minutes,
            template = s.template,
            lowBattery = s.lowBattery,
        )
        viewModelScope.launch {
            if (s.isNew) {
                dao.insert(contact)
            } else {
                val old = dao.byId(id)
                // A changed number or timer is a fresh start for them, even at a spot they were
                // just texted from. Editing the name or message keeps their history.
                val reset = old == null || People.resetsRule(old, contact)
                dao.update(contact.copy(resetAtMillis = if (reset) System.currentTimeMillis() else old!!.resetAtMillis))
            }
            _done.trySend(Unit)
        }
    }

    fun delete() {
        if (_state.value.isNew) { _done.trySend(Unit); return }
        viewModelScope.launch {
            dao.delete(id)
            _done.trySend(Unit)
        }
    }

    private val sendingTest = AtomicBoolean(false)

    /** Sends what this person would get if you'd stayed here, with the fields as typed now. */
    fun sendTest() {
        val s = _state.value
        val phone = s.phone.trim()
        ActionRules.testTextBlocker(phone, Perms.state(app))?.let { say(it.message); return }
        // Getting a fix can take a few seconds. Ignore extra taps so we don't send two texts.
        if (!sendingTest.compareAndSet(false, true)) return
        say("Sending test text…")
        Locations.current(app) { loc ->
            Thread { // The address lookup blocks.
                try {
                    val real = loc?.let { Spot(it.latitude, it.longitude) }
                    val spot = real
                    val text = Message.build(app, s.template, s.minutes, spot)
                    SmsSender.sendTest(app, phone, text) { r ->
                        val size = "${text.length} characters, ${r.parts} part" + if (r.parts == 1) "" else "s"
                        say(
                            when {
                                !r.answered -> "Test text: no answer from the phone after a minute ($size)"
                                r.failCode != null -> "Test text failed, phone code ${r.failCode} ($size)"
                                loc == null -> "Test text sent, no location so no address ($size)"
                                else -> "Test text sent ($size)"
                            }
                        )
                        sendingTest.set(false)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("Aqui", "Test text failed", e)
                    say("Failed: ${e.message}")
                    sendingTest.set(false)
                }
            }.start()
        }
    }

    private fun say(msg: String) {
        _messages.trySend(msg)
    }
}
