package com.example.dwellping.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import com.example.dwellping.MessageTemplate
import com.example.dwellping.TokenEdit
import com.example.dwellping.ui.theme.Covered

/**
 * Shows {place}, {minutes} and {map} in the message box as green word pills ("address",
 * "15 min", "map link"). The saved text keeps the braces. The cursor can't stop inside a pill;
 * [TokenEdit.fix] makes deletes take a whole pill.
 */
class TokenPills(minutes: Int) : VisualTransformation {

    private val labels = mapOf(
        MessageTemplate.PLACE to "address",
        MessageTemplate.MINUTES to "$minutes min",
        MessageTemplate.MAP to "map link",
    )
    private val pill = SpanStyle(color = Covered, background = Covered.copy(alpha = 0.18f), fontWeight = FontWeight.Medium)

    override fun filter(text: AnnotatedString): TransformedText {
        val src = text.text
        val out = AnnotatedString.Builder()
        val toShown = IntArray(src.length + 1)
        val toSaved = ArrayList<Int>(src.length + 1)
        var i = 0
        fun plain(until: Int) {
            while (i < until) {
                toShown[i] = out.length
                toSaved += i
                out.append(src[i])
                i++
            }
        }
        for (r in TokenEdit.tokens(src)) {
            plain(r.first)
            val label = " " + labels.getValue(src.substring(r)) + " "
            val shownStart = out.length
            out.withStyle(pill) { append(label) }
            // Inside a pill counts as its start (from the left edge) or its end.
            toShown[r.first] = shownStart
            for (k in r.first + 1..r.last) toShown[k] = shownStart + label.length
            toSaved += r.first
            repeat(label.length - 1) { toSaved += r.last + 1 }
            i = r.last + 1
        }
        plain(src.length)
        toShown[src.length] = out.length
        toSaved += src.length

        return TransformedText(out.toAnnotatedString(), object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = toShown[offset.coerceIn(0, src.length)]
            override fun transformedToOriginal(offset: Int) = toSaved[offset.coerceIn(0, toSaved.size - 1)]
        })
    }
}
