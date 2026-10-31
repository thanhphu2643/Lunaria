package org.lunaria.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.lunaria.app.LocalCycle
import org.lunaria.app.compareTo
import org.lunaria.app.toCal
import uniffi.lunaria_core.CalDate
import uniffi.lunaria_core.DayLog
import uniffi.lunaria_core.Flow
import uniffi.lunaria_core.Mood
import uniffi.lunaria_core.Symptom

/** Sheet shown when tapping a day in the calendar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailSheet(date: CalDate, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp).imePadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(date.format("EEEE, d MMMM"), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Palette.text)
            PeriodActions(date)
            DayLogEditor(date)
        }
    }
}

@Composable
fun PeriodActions(date: CalDate) {
    val cycle = LocalCycle.current
    val isPeriod = cycle.isPeriodDay(date)
    val isFuture = date > cycle.today
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!isPeriod) {
            PillButton("Period started", Icons.Filled.WaterDrop, filled = true, enabled = !isFuture) {
                cycle.perform { cycle.store.startPeriod(date) }
            }
        } else {
            PillButton("Period ended this day", Icons.Filled.Check, filled = true) {
                cycle.perform { cycle.store.endPeriod(date) }
            }
        }
        PillButton(
            if (isPeriod) "Remove this day" else "Add just this day",
            if (isPeriod) Icons.Filled.Remove else Icons.Filled.Add,
            filled = false,
            enabled = !isFuture || isPeriod,
        ) { cycle.perform { cycle.store.togglePeriodDay(date) } }
        if (isFuture) Text("You can log periods up to today.", fontSize = 13.sp, color = Palette.secondaryText)
    }
}

@Composable
fun PillButton(
    title: String,
    icon: ImageVector,
    filled: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    Button(
        onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onClick() },
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (filled) Palette.accent else Palette.ovulation,
            contentColor = if (filled) Color.White else Palette.accent,
            disabledContainerColor = (if (filled) Palette.accent else Palette.ovulation).copy(alpha = 0.4f),
            disabledContentColor = (if (filled) Color.White else Palette.accent).copy(alpha = 0.6f),
        ),
        elevation = null,
    ) {
        Icon(icon, null, Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** Flow / mood / symptoms / note editor, saved on every change. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DayLogEditor(date: CalDate) {
    val cycle = LocalCycle.current
    val log = cycle.log(date)

    fun update(change: (DayLog) -> DayLog) {
        val next = change(cycle.log(date))
        cycle.perform { cycle.store.saveLog(next) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Section("Flow") {
            Chips(Flow.entries, listOfNotNull(log.flow), Flow::title) { f ->
                update { it.copy(flow = if (it.flow == f) null else f) }
            }
        }
        Section("Mood") {
            Chips(Mood.entries, listOfNotNull(log.mood), Mood::title) { m ->
                update { it.copy(mood = if (it.mood == m) null else m) }
            }
        }
        Section("Symptoms") {
            Chips(Symptom.entries, log.symptoms, Symptom::title) { s ->
                update { if (s in it.symptoms) it.copy(symptoms = it.symptoms - s) else it.copy(symptoms = it.symptoms + s) }
            }
        }
        Section("Notes") {
            OutlinedTextField(
                value = log.note,
                onValueChange = { v -> update { it.copy(note = v) } },
                placeholder = { Text("How are you feeling?") },
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Palette.background.copy(alpha = 0.6f),
                    focusedContainerColor = Palette.background.copy(alpha = 0.6f),
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Palette.accent,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Palette.text)
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> Chips(items: List<T>, selected: List<T>, title: (T) -> String, onTap: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            val on = item in selected
            Text(
                title(item),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (on) Color.White else Palette.text,
                modifier = Modifier
                    .background(if (on) Palette.accent else Palette.background.copy(alpha = 0.7f), CircleShape)
                    .clip(CircleShape)
                    .clickableNoRipple { onTap(item) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            )
        }
    }
}

/** "Log" tab: a day's log with a date switcher. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyLogScreen() {
    var day by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var picking by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).imePadding(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ScreenTitle("Daily Log")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Day", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Palette.text)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { picking = true }) {
                Icon(Icons.Outlined.CalendarMonth, null)
                Spacer(Modifier.width(6.dp))
                Text(day.toCal().format("EEE, d MMM yyyy"), fontSize = 16.sp)
            }
        }
        PeriodActions(day.toCal())
        DayLogEditor(day.toCal())
        Spacer(Modifier.height(8.dp))
    }

    if (picking) {
        DatePickerSheet(initial = day, onDismiss = { picking = false }) { day = it; picking = false }
    }
}

/** Material date picker limited to today and earlier. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerSheet(initial: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val todayMillis = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayMillis
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) { DatePicker(state) }
}

// ---- Labels ----

val Flow.title: String
    get() = when (this) {
        Flow.SPOTTING -> "Spotting"
        Flow.LIGHT -> "Light"
        Flow.MEDIUM -> "Medium"
        Flow.HEAVY -> "Heavy"
    }

val Mood.title: String
    get() = when (this) {
        Mood.HAPPY -> "😊 Happy"
        Mood.CALM -> "😌 Calm"
        Mood.SENSITIVE -> "🥺 Sensitive"
        Mood.SAD -> "😢 Sad"
        Mood.IRRITATED -> "😤 Irritated"
        Mood.ANXIOUS -> "😰 Anxious"
    }

val Symptom.title: String
    get() = when (this) {
        Symptom.CRAMPS -> "Cramps"
        Symptom.HEADACHE -> "Headache"
        Symptom.BLOATING -> "Bloating"
        Symptom.ACNE -> "Acne"
        Symptom.TENDER_BREASTS -> "Tender breasts"
        Symptom.FATIGUE -> "Fatigue"
        Symptom.BACK_PAIN -> "Back pain"
        Symptom.NAUSEA -> "Nausea"
        Symptom.CRAVINGS -> "Cravings"
        Symptom.INSOMNIA -> "Insomnia"
    }

fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = this.then(
    Modifier.clickable(interactionSource = null, indication = null, onClick = onClick),
)
