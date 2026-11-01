package org.lunaria.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.lunaria.app.toLocal
import uniffi.lunaria_core.CalDate
import uniffi.lunaria_core.CyclePhase
import uniffi.lunaria_core.DayKind

object Palette {
    val accent = Color(0xFFE0607E)
    val period = Color(0xFFE8687C)
    val ovulation = Color(0xFFFBE3E8)
    val fertile = Color(0xFFE4EEFB)
    val safeStroke = Color(0xFFEDB3BF)
    val background = Color(0xFFFBE6EB)
    val text = Color(0xFF2B2B2E)
    val secondaryText = Color(0xFF6E6E73)
    val iconBubble = Color(0xFFF4C9D3)
}

@Composable
fun LunariaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Palette.accent,
            onPrimary = Color.White,
            primaryContainer = Palette.ovulation,
            onPrimaryContainer = Palette.text,
            secondaryContainer = Palette.ovulation,
            background = Color.White,
            surface = Color.White,
            surfaceContainerLow = Color.White,
            surfaceContainerHigh = Color.White,
            onSurface = Palette.text,
            onSurfaceVariant = Palette.secondaryText,
        ),
        content = content,
    )
}

fun Modifier.card(): Modifier = this
    .shadow(16.dp, RoundedCornerShape(28.dp), ambientColor = Palette.accent.copy(alpha = 0.15f), spotColor = Palette.accent.copy(alpha = 0.15f))
    .background(Color.White, RoundedCornerShape(28.dp))
    .padding(20.dp)

val DayKind.label: String
    get() = when (this) {
        DayKind.PERIOD -> L("Period")
        DayKind.PREDICTED_PERIOD -> L("Predicted")
        DayKind.OVULATION -> L("Ovulation")
        DayKind.FERTILE -> L("Fertile Days")
        DayKind.SAFE -> L("Safe Days")
        DayKind.UNKNOWN -> ""
    }

val CyclePhase.title: String
    get() = when (this) {
        CyclePhase.MENSTRUAL -> L("Menstrual phase")
        CyclePhase.FOLLICULAR -> L("Follicular phase")
        CyclePhase.FERTILE -> L("Fertile window")
        CyclePhase.LUTEAL -> L("Luteal phase")
    }

val CyclePhase.blurb: String
    get() = when (this) {
        CyclePhase.MENSTRUAL -> L("Rest, hydrate and keep warm. Energy is usually lower.")
        CyclePhase.FOLLICULAR -> L("Estrogen is rising — energy and mood often improve.")
        CyclePhase.FERTILE -> L("Highest chance of pregnancy in this window.")
        CyclePhase.LUTEAL -> L("Progesterone peaks. PMS symptoms may appear near the end.")
    }

fun CalDate.format(pattern: String = "d MMM"): String =
    toLocal().format(DateTimeFormatter.ofPattern(pattern, I18n.locale))
