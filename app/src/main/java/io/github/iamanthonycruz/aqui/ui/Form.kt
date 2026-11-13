package io.github.iamanthonycruz.aqui.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.iamanthonycruz.aqui.ui.theme.Covered
import io.github.iamanthonycruz.aqui.ui.theme.Mist
import io.github.iamanthonycruz.aqui.ui.theme.OnCovered
import io.github.iamanthonycruz.aqui.ui.theme.AppFont

/*
 * The app's own form pieces, in place of Material's stock text fields and buttons.
 * One look everywhere: soft filled shapes on the Dusk gradient, no outlines, no floating labels.
 */

private val FieldShape = RoundedCornerShape(16.dp)
/** Text fields don't inherit the theme font, so it is set here. */
private val FieldText = TextStyle(color = Mist, fontSize = 17.sp, lineHeight = 24.sp, fontFamily = AppFont)

/** A small link at the right end of a field's label row, e.g. "Pick from contacts". */
data class FieldAction(val text: String, val onClick: () -> Unit)

/** Small heading above a field or group, with an optional [action] link on the right. */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier, action: FieldAction? = null, icon: ImageVector? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Mist.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text,
            color = Mist.copy(alpha = 0.7f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.3.sp,
            modifier = Modifier.weight(1f),
        )
        if (action != null) {
            Text(
                action.text,
                color = Covered,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clip(CircleShape).clickable(onClick = action.onClick).padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

/** Quiet line under a field. */
@Composable
fun FieldHint(text: String, color: Color = Mist.copy(alpha = 0.5f)) {
    Text(text, color = color, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
}

/** Label, filled rounded box, optional hint. */
@Composable
fun DuskField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    /** What the keyboard's action key does, e.g. run the search. */
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    action: FieldAction? = null,
    /** Shown at the start of the box, e.g. a phone for the number. */
    icon: ImageVector? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Column(modifier.fillMaxWidth()) {
        FieldLabel(label, action = action)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            textStyle = FieldText,
            cursorBrush = SolidColor(Covered),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interaction,
            decorationBox = { inner -> FieldBox(focused, enabled, showPlaceholder = value.isEmpty(), placeholder, icon, inner) },
        )
        if (hint != null) FieldHint(hint)
    }
}

/** Same as the other [DuskField], for a field that needs the cursor position (the message chips). */
@Composable
fun DuskField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    icon: ImageVector? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Column(modifier.fillMaxWidth()) {
        FieldLabel(label)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            minLines = minLines,
            visualTransformation = visualTransformation,
            textStyle = FieldText,
            cursorBrush = SolidColor(Covered),
            interactionSource = interaction,
            decorationBox = { inner -> FieldBox(focused, enabled = true, showPlaceholder = value.text.isEmpty(), placeholder, icon, inner) },
        )
    }
}

@Composable
private fun FieldBox(
    focused: Boolean,
    enabled: Boolean,
    showPlaceholder: Boolean,
    placeholder: String,
    icon: ImageVector?,
    inner: @Composable () -> Unit,
) {
    Row(
        // Top-aligned, so the icon sits on the first line of a multi-line message.
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .clip(FieldShape)
            .background(Mist.copy(alpha = if (focused) 0.12f else 0.08f))
            .border(1.dp, if (focused) Mist.copy(alpha = 0.35f) else Color.Transparent, FieldShape)
            .padding(horizontal = 16.dp, vertical = 15.dp),
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (focused) Covered else Mist.copy(alpha = 0.55f),
                modifier = Modifier.padding(top = 1.dp, end = 12.dp).size(22.dp),
            )
        }
        Box(Modifier.weight(1f)) {
            if (showPlaceholder) Text(placeholder, style = FieldText.copy(color = Mist.copy(alpha = 0.3f)))
            inner()
        }
    }
}

/** The main action: full-width green pill. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    PillButton(text, onClick, Covered, OnCovered, modifier, icon)
}

/** A lesser action: full-width see-through pill. */
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    PillButton(text, onClick, Mist.copy(alpha = 0.10f), Mist, modifier, icon)
}

@Composable
private fun PillButton(text: String, onClick: () -> Unit, bg: Color, fg: Color, modifier: Modifier, icon: ImageVector?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, color = fg, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Small round pill, e.g. the {place} chips. */
@Composable
fun Chip(text: String, icon: ImageVector? = null, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(Mist.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(start = if (icon != null) 10.dp else 14.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Covered, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = Mist, fontSize = 14.sp)
    }
}

/** "[−]  15 min  [+]" in one filled box, matching the text fields. */
@Composable
fun MinutesStepper(minutes: Int, canLower: Boolean, canRaise: Boolean, onStep: (up: Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(FieldShape)
            .background(Mist.copy(alpha = 0.08f))
            .padding(8.dp),
    ) {
        StepButton("−", "5 minutes less", canLower) { onStep(false) }
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f),
        ) {
            Text("$minutes", color = Mist, fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(6.dp))
            Text("min", color = Mist.copy(alpha = 0.6f), fontSize = 17.sp, modifier = Modifier.padding(bottom = 6.dp))
        }
        StepButton("+", "5 minutes more", canRaise) { onStep(true) }
    }
}

@Composable
private fun StepButton(symbol: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(52.dp)
            .alpha(if (enabled) 1f else 0.3f)
            .clip(CircleShape)
            .background(Mist.copy(alpha = 0.12f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        Text(symbol, color = Mist, fontSize = 26.sp, fontWeight = FontWeight.Medium)
    }
}

/** What the text will look like on their phone: a message bubble. */
@Composable
fun MessageBubble(text: String) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Text(
            text,
            color = OnCovered,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            modifier = Modifier
                .padding(start = 40.dp)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 6.dp))
                .background(Covered)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}
