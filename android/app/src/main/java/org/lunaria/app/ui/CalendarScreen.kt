package org.lunaria.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChildFriendly
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import org.lunaria.app.LocalCycle
import uniffi.lunaria_core.CalDate
import uniffi.lunaria_core.CalendarDay
import uniffi.lunaria_core.DayKind
import uniffi.lunaria_core.MonthView

@Composable
fun CalendarScreen() {
    val cycle = LocalCycle.current
    var ym by rememberSaveable { mutableStateOf(YearMonth.now()) }
    var selectedDay by remember { mutableStateOf<CalDate?>(null) }
    var tool by remember { mutableStateOf<Tool?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ScreenTitle("Calendar")

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.CalendarMonth, null, tint = Palette.text)
            Spacer(Modifier.width(14.dp))
            Text(
                ym.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Palette.text,
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { ym = ym.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month", tint = Palette.accent)
            }
            IconButton(onClick = { ym = ym.plusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month", tint = Palette.accent)
            }
        }

        AnimatedContent(
            targetState = ym,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                slideInHorizontally { it * dir } togetherWith slideOutHorizontally { -it * dir }
            },
            label = "month",
            modifier = Modifier.pointerInput(Unit) {
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        if (total < -120) ym = ym.plusMonths(1) else if (total > 120) ym = ym.minusMonths(1)
                    },
                ) { _, dx -> total += dx }
            },
        ) { month ->
            cycle.month(month.year, month.monthValue)?.let { view ->
                MonthGrid(view, cycle.firstWeekday) { selectedDay = it }
            }
        }

        Legend()

        if (cycle.periods.isEmpty()) {
            Text(
                "Tap a day to log when your last period started — predictions appear instantly.",
                color = Palette.secondaryText, fontSize = 15.sp,
            )
        }

        GeneralTools { tool = it }
        Spacer(Modifier.height(8.dp))
    }

    selectedDay?.let { DayDetailSheet(it) { selectedDay = null } }
    tool?.let { ToolSheet(it) { tool = null } }
}

@Composable
fun ScreenTitle(text: String) {
    Text(text, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Palette.text, modifier = Modifier.padding(top = 16.dp))
}

@Composable
private fun MonthGrid(view: MonthView, firstWeekday: Int, onSelect: (CalDate) -> Unit) {
    // DayOfWeek: 1 = Monday … 7 = Sunday; firstWeekday: 0 = Sunday.
    val weekdays = (0 until 7).map { DayOfWeek.of(((firstWeekday + it + 6) % 7) + 1) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            weekdays.forEach {
                Text(
                    it.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    Modifier.weight(1f), color = Palette.secondaryText, fontSize = 16.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        view.days.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                week.forEach { day ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        DayCell(day) { onSelect(day.date) }
                    }
                }
            }
        }
    }
}

private fun DrawScope.dashedCircle(color: Color, width: Dp, dash: Float) {
    val stroke = width.toPx()
    drawCircle(
        color, radius = size.minDimension / 2 - stroke / 2,
        style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))),
    )
}

@Composable
private fun DayCell(day: CalendarDay, onClick: () -> Unit) {
    val kind = if (day.inMonth) day.kind else DayKind.UNKNOWN
    val textColor = when {
        !day.inMonth -> Palette.secondaryText.copy(alpha = 0.4f)
        kind == DayKind.PERIOD -> Color.White
        else -> Palette.text
    }
    Box(
        Modifier
            .widthIn(max = 56.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(CircleShape)
            .then(
                when (kind) {
                    DayKind.PERIOD -> Modifier.background(Palette.period)
                    DayKind.PREDICTED_PERIOD -> Modifier.background(Palette.period.copy(alpha = 0.18f))
                        .drawBehind { dashedCircle(Palette.period, 1.5.dp, 10f) }
                    DayKind.OVULATION -> Modifier.background(Palette.ovulation)
                        .border(1.5.dp, Palette.period.copy(alpha = 0.6f), CircleShape)
                    DayKind.FERTILE -> Modifier.background(Palette.fertile)
                    DayKind.SAFE -> Modifier.drawBehind { dashedCircle(Palette.safeStroke, 1.2.dp, 8f) }
                    DayKind.UNKNOWN -> Modifier
                },
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = "${day.date.format("d MMMM")}, ${kind.label}" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "${day.date.day}",
            fontSize = 18.sp,
            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
        )
        if (day.isToday) {
            Box(
                Modifier.offset(y = 14.dp).size(5.dp)
                    .background(if (kind == DayKind.PERIOD) Color.White else Palette.accent, CircleShape),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend() {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(DayKind.PERIOD, DayKind.OVULATION, DayKind.FERTILE, DayKind.SAFE, DayKind.PREDICTED_PERIOD).forEach { kind ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(14.dp).clip(CircleShape).then(
                        when (kind) {
                            DayKind.PERIOD -> Modifier.background(Palette.period)
                            DayKind.OVULATION -> Modifier.background(Palette.ovulation).border(1.dp, Palette.period.copy(alpha = 0.6f), CircleShape)
                            DayKind.FERTILE -> Modifier.background(Palette.fertile)
                            DayKind.SAFE -> Modifier.drawBehind { dashedCircle(Palette.safeStroke, 1.dp, 4f) }
                            else -> Modifier.background(Palette.period.copy(alpha = 0.18f)).drawBehind { dashedCircle(Palette.period, 1.dp, 4f) }
                        },
                    ),
                )
                Spacer(Modifier.width(6.dp))
                Text(kind.label, color = Palette.secondaryText, fontSize = 15.sp)
            }
        }
    }
}

enum class Tool(val title: String, val subtitle: String, val icon: ImageVector) {
    DueDate("Due Date Calculator", "Calculates estimated due date", Icons.Outlined.EditCalendar),
    PregnancyTest("Pregnancy Test Calculator", "Calculates optimal date for pregnancy test.", Icons.Outlined.Science),
    PregnancyWeek("Pregnancy Calculator", "Estimates pregnancy week.", Icons.Outlined.ChildFriendly),
}

@Composable
private fun GeneralTools(onSelect: (Tool) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("General Tools:", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = Palette.text)
        Tool.entries.forEach { t ->
            Row(
                Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(40.dp)).clickable { onSelect(t) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(80.dp).background(Palette.iconBubble, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(t.icon, null, tint = Palette.accent, modifier = Modifier.size(34.dp))
                }
                Spacer(Modifier.width(20.dp))
                Column {
                    Text(t.title, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = Palette.text)
                    Text(t.subtitle, fontSize = 16.sp, color = Palette.secondaryText)
                }
            }
        }
    }
}
