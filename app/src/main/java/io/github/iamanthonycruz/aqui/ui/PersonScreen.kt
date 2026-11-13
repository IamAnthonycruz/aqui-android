package io.github.iamanthonycruz.aqui.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.iamanthonycruz.aqui.Config
import io.github.iamanthonycruz.aqui.LowBattery
import io.github.iamanthonycruz.aqui.MessageTemplate
import io.github.iamanthonycruz.aqui.TokenEdit
import io.github.iamanthonycruz.aqui.ui.theme.Alert
import io.github.iamanthonycruz.aqui.ui.theme.Covered
import io.github.iamanthonycruz.aqui.ui.theme.OnCovered
import io.github.iamanthonycruz.aqui.ui.theme.AquiTheme
import io.github.iamanthonycruz.aqui.ui.theme.Mist

/** Add or edit one person. Changes are kept only when Save is tapped; Back discards them. */
@Composable
fun PersonRoute(onDone: () -> Unit, vm: PersonViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm) { vm.messages.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(vm) { vm.done.collect { onDone() } }

    PersonScreen(
        state = state,
        snackbar = snackbar,
        onBack = onDone,
        onName = vm::onName,
        onPhone = vm::onPhone,
        onPickContact = rememberContactPicker { vm.onPicked(it.name, it.phone) },
        onTemplate = vm::onTemplate,
        onLowBattery = vm::onLowBattery,
        onStepMinutes = vm::stepMinutes,
        onSendTest = vm::sendTest,
        onSave = vm::save,
        onDelete = vm::delete,
    )
}

@Composable
fun PersonScreen(
    state: PersonUiState,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onName: (String) -> Unit,
    onPhone: (String) -> Unit,
    onPickContact: () -> Unit,
    onTemplate: (String) -> Unit,
    onLowBattery: (Boolean) -> Unit,
    onStepMinutes: (up: Boolean) -> Unit,
    onSendTest: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    DuskScaffold(
        title = if (state.isNew) "Add person" else "Edit person",
        onBack = onBack,
        snackbar = snackbar,
        // Pinned, so Add / Save is on screen on every phone, however long the form gets.
        bottomBar = if (state.loaded) {
            { PrimaryButton(if (state.isNew) "Add person" else "Save", onSave, icon = Icons.Rounded.Check) }
        } else null,
    ) {
        if (!state.loaded) return@DuskScaffold

        // Adding: start from contacts, or just type below. Editing keeps the small link on Number.
        if (state.isNew) SecondaryButton("Choose from contacts", onPickContact, icon = Icons.Rounded.Contacts)

        DuskField(
            value = state.name,
            onValueChange = onName,
            label = "Name",
            icon = Icons.Rounded.Person,
            placeholder = "Mom (optional)",
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        )
        DuskField(
            value = state.phone,
            onValueChange = onPhone,
            label = "Number",
            icon = Icons.Rounded.Phone,
            placeholder = "+1 404 555 0132",
            hint = "Include the country code: +1 for the US",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            action = if (state.isNew) null else FieldAction("Pick from contacts", onPickContact),
        )

        Column {
            FieldLabel("Text after", icon = Icons.Rounded.Timer)
            MinutesStepper(
                minutes = state.minutes,
                canLower = state.minutes > Config.MIN_MINUTES,
                canRaise = state.minutes < Config.MAX_MINUTES,
                onStep = onStepMinutes,
            )
            FieldHint("One text once you've stayed this long in one spot.")
        }

        MessageSection(state.template, state.minutes, onTemplate)

        LowBatteryRow(state.lowBattery, onLowBattery)


        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton("Send test text", onSendTest, icon = Icons.AutoMirrored.Rounded.Send)
            if (!state.isNew) {
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = Alert, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Delete person", color = Alert, fontSize = 15.sp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${state.name.trim().ifBlank { state.phone }}?") },
            text = { Text("They won't get any more texts.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Delete", color = Alert) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = Mist) } },
        )
    }
}

@Composable
private fun LowBatteryRow(checked: Boolean, onChange: (Boolean) -> Unit) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange)
                .padding(vertical = 4.dp),
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = Covered,
                    checkmarkColor = OnCovered,
                    uncheckedColor = Mist.copy(alpha = 0.5f),
                ),
            )
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Rounded.BatteryAlert, contentDescription = null, tint = Mist, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Text them when my battery is low", color = Mist, fontSize = 17.sp)
        }
        FieldHint("One text at ${LowBattery.PERCENT}% with your last spot, once per charge.")
    }
}

@Composable
private fun MessageSection(template: String, minutes: Int, onTemplateChange: (String) -> Unit) {
    // Kept here, not in the ViewModel: the chips need the cursor position to insert at.
    var field by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(template, TextRange(template.length)))
    }
    fun update(v: TextFieldValue) {
        field = v
        onTemplateChange(v.text)
    }
    fun insert(token: String) {
        val text = field.text.replaceRange(field.selection.min, field.selection.max, token)
        update(TextFieldValue(text, TextRange(field.selection.min + token.length)))
    }

    Column {
        DuskField(
            value = field,
            onValueChange = { v ->
                // A delete that cuts into a pill takes the whole pill.
                val fixed = if (v.text == field.text) null else TokenEdit.fix(field.text, v.text, v.selection.end)
                if (fixed == null || fixed.text == v.text) update(v)
                else update(TextFieldValue(fixed.text, TextRange(fixed.cursor)))
            },
            label = "Message",
            icon = Icons.Rounded.ChatBubbleOutline,
            placeholder = "Leave empty for the default",
            minLines = 3,
            visualTransformation = remember(minutes) { TokenPills(minutes) },
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Chip("address", Icons.Rounded.Place) { insert(MessageTemplate.PLACE) }
            Chip("minutes", Icons.Rounded.Schedule) { insert(MessageTemplate.MINUTES) }
            Chip("map link", Icons.Rounded.Map) { insert(MessageTemplate.MAP) }
        }
        if (field.text.isNotEmpty()) {
            TextButton(onClick = { update(TextFieldValue("")) }) {
                Text("Use the default message", color = Mist.copy(alpha = 0.7f), fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(20.dp))
        FieldLabel("Message preview", icon = Icons.Rounded.Visibility)
        MessageBubble(
            MessageTemplate.render(
                template = field.text,
                place = "123 Main St, Boston",
                minutes = minutes,
                mapLink = MessageTemplate.mapLink(42.36010, -71.05890),
            )
        )
        Text(
            "Sample address. The real text uses where you are.",
            color = Mist.copy(alpha = 0.45f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp, end = 4.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

@Preview(widthDp = 393, heightDp = 1100)
@Composable
private fun PersonScreenPreview() {
    AquiTheme {
        PersonScreen(
            state = PersonUiState(isNew = false, name = "Mom", phone = "+14045550132", minutes = 30, loaded = true),
            snackbar = remember { SnackbarHostState() },
            onBack = {}, onName = {}, onPhone = {}, onPickContact = {}, onTemplate = {}, onLowBattery = {}, onStepMinutes = {},
            onSendTest = {}, onSave = {}, onDelete = {},
        )
    }
}
