package org.lunaria.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import org.lunaria.app.LocalCycle
import org.lunaria.app.toCal
import org.lunaria.app.toLocal
import uniffi.lunaria_core.dueDate
import uniffi.lunaria_core.pregnancyProgress
import uniffi.lunaria_core.pregnancyTestDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolSheet(tool: Tool, onDismiss: () -> Unit) {
    val cycle = LocalCycle.current
    var lmp by remember { mutableStateOf(cycle.periods.lastOrNull()?.start?.toLocal() ?: LocalDate.now()) }
    var cycleLength by remember { mutableIntStateOf(cycle.stats?.averageCycleLength?.toInt() ?: 28) }
    var picking by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(
            Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(tool.title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Palette.text)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("First day of last period", color = Palette.text, modifier = Modifier.weight(1f))
                TextButton(onClick = { picking = true }) {
                    Icon(Icons.Outlined.CalendarMonth, null)
                    Spacer(Modifier.width(6.dp))
                    Text(lmp.toCal().format("d MMM yyyy"))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Cycle length: $cycleLength days", color = Palette.text, modifier = Modifier.weight(1f))
                FilledTonalIconButton(onClick = { if (cycleLength > 20) cycleLength-- }) { Icon(Icons.Filled.Remove, "Shorter") }
                FilledTonalIconButton(onClick = { if (cycleLength < 45) cycleLength++ }) { Icon(Icons.Filled.Add, "Longer") }
            }
            HorizontalDivider(color = Palette.text.copy(alpha = 0.08f))

            val start = lmp.toCal()
            val len = cycleLength.toUInt()
            when (tool) {
                Tool.DueDate -> runCatching { dueDate(start, len) }.getOrNull()?.let { r ->
                    ResultRow("Estimated due date", r.dueDate.format("d MMMM yyyy"), highlight = true)
                    ResultRow("Likely conception", r.conceptionDate.format())
                }
                Tool.PregnancyTest -> runCatching { pregnancyTestDate(start, len) }.getOrNull()?.let { r ->
                    ResultRow("Best day to test", r.recommendedTest.format("EEE, d MMMM"), highlight = true)
                    ResultRow("Earliest (sensitive test)", r.earliestTest.format())
                    ResultRow("Estimated ovulation", r.estimatedOvulation.format())
                }
                Tool.PregnancyWeek -> runCatching { pregnancyProgress(start, len, cycle.today) }.getOrNull()?.let { r ->
                    ResultRow("You are", "${r.weeks} weeks ${r.days} days", highlight = true)
                    ResultRow("Trimester", "${r.trimester}")
                    ResultRow("Due date", r.dueDate.format("d MMMM yyyy"))
                    LinearProgressIndicator(
                        progress = { r.progress.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = Palette.accent, trackColor = Palette.ovulation, strokeCap = StrokeCap.Round,
                        gapSize = 0.dp, drawStopIndicator = {},
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Estimates only, based on the calendar method. Always confirm with a healthcare professional.",
                fontSize = 13.sp, color = Palette.secondaryText,
            )
        }
    }

    if (picking) DatePickerSheet(lmp, onDismiss = { picking = false }) { lmp = it; picking = false }
}

@Composable
private fun ResultRow(title: String, value: String, highlight: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Palette.secondaryText, modifier = Modifier.weight(1f))
        Text(
            value,
            color = if (highlight) Palette.accent else Palette.text,
            fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = if (highlight) 18.sp else 16.sp,
        )
    }
}
