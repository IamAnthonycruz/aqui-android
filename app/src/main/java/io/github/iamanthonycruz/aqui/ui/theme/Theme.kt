package io.github.iamanthonycruz.aqui.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import io.github.iamanthonycruz.aqui.R

/** Dusk: the one look for every screen. Gradient from [DuskTop] to [DuskBottom]. */
val DuskTop = Color(0xFF1C2A4A)
val DuskBottom = Color(0xFF3B2F5C)
/** Text and icons. */
val Mist = Color(0xFFF2F0FA)
/** On, texted, done. */
val Covered = Color(0xFF3DDC97)
val OnCovered = Color(0xFF0B3B27)
/** Soft accent. */
val Lavender = Color(0xFFC9B8FF)
/** Failed, needed. */
val Alert = Color(0xFFFFB4AB)

private val Dusk = darkColorScheme(
    primary = Covered,
    onPrimary = OnCovered,
    primaryContainer = Color(0xFF1F5A44),
    onPrimaryContainer = Color(0xFFB8F5D9),
    secondary = Mist,
    onSecondary = DuskTop,
    secondaryContainer = Color(0xFF34406A),
    onSecondaryContainer = Mist,
    tertiary = Lavender,
    onTertiary = Color(0xFF2A1D5C),
    tertiaryContainer = Color(0xFF453A7A),
    onTertiaryContainer = Lavender,
    background = DuskTop,
    onBackground = Mist,
    surface = DuskTop,
    onSurface = Mist,
    surfaceVariant = Color(0xFF2E3960),
    onSurfaceVariant = Color(0xFFBEBAD4),
    surfaceContainerLowest = Color(0xFF18243F),
    surfaceContainerLow = Color(0xFF222E50),
    surfaceContainer = Color(0xFF283456),
    surfaceContainerHigh = Color(0xFF2E3A5E),
    surfaceContainerHighest = Color(0xFF354166),
    inverseSurface = Mist,
    inverseOnSurface = DuskTop,
    outline = Color(0xFF7D79A0),
    outlineVariant = Color(0xFF4A4870),
    error = Alert,
    onError = Color(0xFF690005),
)

/**
 * Outfit, the app's one typeface: geometric, a little playful. A single
 * variable font file (res/font/outfit.ttf, SIL Open Font License, see docs/fonts) gives every weight.
 */
@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int) = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { w ->
        Font(res, w, variationSettings = FontVariation.Settings(FontVariation.weight(w.weight)))
    }
)

val AppFont = variable(R.font.outfit)

/** Material's sizes, all in [AppFont]. Text with only a fontSize set inherits this too. */
private val AppType = Typography().run {
    fun TextStyle.app() = copy(fontFamily = AppFont)
    Typography(
        displayLarge = displayLarge.app(), displayMedium = displayMedium.app(), displaySmall = displaySmall.app(),
        headlineLarge = headlineLarge.app(), headlineMedium = headlineMedium.app(), headlineSmall = headlineSmall.app(),
        titleLarge = titleLarge.app(), titleMedium = titleMedium.app(), titleSmall = titleSmall.app(),
        bodyLarge = bodyLarge.app(), bodyMedium = bodyMedium.app(), bodySmall = bodySmall.app(),
        labelLarge = labelLarge.app(), labelMedium = labelMedium.app(), labelSmall = labelSmall.app(),
    )
}

/** Always Dusk, in light and dark system mode, so every screen matches the Start screen. */
@Composable
fun AquiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Dusk, typography = AppType, content = content)
}
