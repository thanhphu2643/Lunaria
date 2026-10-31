package org.lunaria.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.lunaria.app.LocalCycle
import uniffi.lunaria_core.CycleStatus

@Composable
fun HomeScreen(selectTab: (Tab) -> Unit) {
    val cycle = LocalCycle.current
    val status = cycle.status

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(0f to Palette.background, 0.45f to Color.White))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Column(Modifier.padding(top = 16.dp)) {
            Text(
                LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())),
                fontSize = 16.sp, color = Palette.secondaryText,
            )
            Text("Hello 🌸", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Palette.text)
        }

        if (status != null) {
            CycleRing(status)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val today = cycle.today
                if (cycle.isPeriodDay(today)) {
                    PillButton("Period ended", Icons.Filled.Check, filled = true, modifier = Modifier.weight(1f)) {
                        cycle.perform { cycle.store.endPeriod(today) }
                    }
                } else {
                    PillButton("Period started", Icons.Filled.WaterDrop, filled = true, modifier = Modifier.weight(1f)) {
                        cycle.perform { cycle.store.startPeriod(today) }
                    }
                }
                PillButton("Log today", Icons.Outlined.EditNote, filled = false, modifier = Modifier.weight(1f)) {
                    selectTab(Tab.Log)
                }
            }
            PhaseCard(status)
            cycle.stats?.let { stats ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("${stats.averageCycleLength}", "days", "Avg. cycle", Modifier.weight(1f))
                    StatTile("${stats.averagePeriodLength}", "days", "Avg. period", Modifier.weight(1f))
                    StatTile("${stats.loggedCycles}", "", "Cycles logged", Modifier.weight(1f))
                }
            }
        } else {
            EmptyState { selectTab(Tab.Calendar) }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun CycleRing(status: CycleStatus) {
    val progress = (status.cycleDay.toFloat() / status.cycleLength.toFloat().coerceAtLeast(1f)).coerceAtMost(1f)
    val days = status.daysUntilNextPeriod
    val late = status.daysLate.toInt()
    val headline = when {
        late > 0 -> "$late day${if (late == 1) "" else "s"} late"
        days == 0 -> "Period expected today"
        else -> "$days day${if (days == 1) "" else "s"}"
    }
    Box(
        Modifier.fillMaxWidth().padding(vertical = 8.dp)
            .clearAndSetSemantics { contentDescription = "Period in $headline, cycle day ${status.cycleDay}" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(260.dp)) {
            val stroke = 22.dp.toPx()
            val inset = stroke / 2
            val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(Palette.ovulation, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            drawArc(
                Brush.sweepGradient(listOf(Palette.period.copy(alpha = 0.6f), Palette.accent)),
                -90f, 360f * progress, false, topLeft, arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(40.dp)) {
            if (late == 0 && days != 0) Text("Period in", fontSize = 17.sp, color = Palette.secondaryText)
            Text(headline, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Palette.accent, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(
                "Cycle day ${status.cycleDay}",
                fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Palette.text,
                modifier = Modifier.background(Palette.ovulation, CircleShape).padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun PhaseCard(status: CycleStatus) {
    Column(Modifier.fillMaxWidth().card(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(status.phase.title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Palette.text)
        Text(status.phase.blurb, fontSize = 15.sp, color = Palette.secondaryText)
        HorizontalDivider(color = Palette.text.copy(alpha = 0.08f))
        InfoLine(Palette.period, "Next period", status.nextPeriodStart.format())
        InfoLine(Palette.ovulation, "Ovulation", status.nextOvulation.format())
        InfoLine(Palette.fertile, "Fertile window", "${status.fertileStart.format()} – ${status.fertileEnd.format()}")
    }
}

@Composable
private fun InfoLine(color: Color, title: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).background(color, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(title, color = Palette.secondaryText, fontSize = 16.sp)
        Spacer(Modifier.weight(1f))
        Text(value, color = Palette.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatTile(value: String, unit: String, title: String, modifier: Modifier = Modifier) {
    Column(
        modifier.background(Palette.background.copy(alpha = 0.6f), RoundedCornerShape(20.dp)).padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Palette.accent)
            if (unit.isNotEmpty()) {
                Spacer(Modifier.width(3.dp))
                Text(unit, fontSize = 13.sp, color = Palette.secondaryText, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
        Text(title, fontSize = 13.sp, color = Palette.secondaryText)
    }
}

@Composable
private fun EmptyState(openCalendar: () -> Unit) {
    val cycle = LocalCycle.current
    Column(
        Modifier.fillMaxWidth().padding(top = 20.dp).card(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Icon(Icons.Filled.WaterDrop, null, tint = Palette.period, modifier = Modifier.size(64.dp))
        Text("Welcome to Lunaria", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Palette.text)
        Text(
            "Log the first day of your last period and Lunaria will predict your next period, ovulation and fertile window. Everything stays on this device.",
            textAlign = TextAlign.Center, color = Palette.secondaryText,
        )
        PillButton("My period started today", Icons.Filled.WaterDrop, filled = true) {
            cycle.perform { cycle.store.startPeriod(cycle.today) }
        }
        PillButton("Pick a past date", Icons.Outlined.CalendarMonth, filled = false, onClick = openCalendar)
    }
}
