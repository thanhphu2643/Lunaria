package org.lunaria.app.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.temporal.ChronoUnit
import org.lunaria.app.LocalCycle
import org.lunaria.app.toLocal
import uniffi.lunaria_core.CycleSettings
import uniffi.lunaria_core.coreVersion

@Composable
fun ProfileScreen() {
    val cycle = LocalCycle.current
    val context = LocalContext.current
    val settings = cycle.settings
    val stats = cycle.stats
    var confirmErase by remember { mutableStateOf(false) }

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) cycle.perform {
            val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("Could not read the file")
            cycle.store.importJson(json)
        }
    }

    fun update(change: (CycleSettings) -> CycleSettings) {
        cycle.perform { cycle.store.updateSettings(change(cycle.settings)) }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ScreenTitle("Profile")

        Group("Cycle", "With “Learn from my history” on, averages of your last 6 cycles are used once you have logged data.") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Learn from my history", color = Palette.text, modifier = Modifier.weight(1f))
                Switch(
                    checked = settings.autoAdjust,
                    onCheckedChange = { v -> update { it.copy(autoAdjust = v) } },
                    colors = SwitchDefaults.colors(checkedTrackColor = Palette.accent),
                )
            }
            Stepper("Cycle length", settings.cycleLength.toInt(), 15..60,
                enabled = !(settings.autoAdjust && (stats?.loggedCycles ?: 0u) > 0u)) { v -> update { it.copy(cycleLength = v.toUInt()) } }
            Stepper("Period length", settings.periodLength.toInt(), 1..14,
                enabled = !(settings.autoAdjust && cycle.periods.isNotEmpty())) { v -> update { it.copy(periodLength = v.toUInt()) } }
            Stepper("Luteal phase", settings.lutealLength.toInt(), 7..20) { v -> update { it.copy(lutealLength = v.toUInt()) } }
        }

        if (stats != null && stats.loggedCycles > 0u) {
            Group("History") {
                Line("Cycles logged", "${stats.loggedCycles}")
                stats.shortestCycle?.let { Line("Shortest cycle", "$it days") }
                stats.longestCycle?.let { Line("Longest cycle", "$it days") }
            }
        }

        Group("Periods") {
            val periods = cycle.periods.reversed()
            if (periods.isEmpty()) Text("No periods logged yet", color = Palette.secondaryText)
            periods.forEach { p ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(p.start.format("d MMMM yyyy"), color = Palette.text, modifier = Modifier.weight(1f))
                    val days = ChronoUnit.DAYS.between(p.start.toLocal(), p.end.toLocal()) + 1
                    Text("$days days", color = Palette.secondaryText)
                    IconButton(onClick = { cycle.perform { cycle.store.deletePeriod(p.start) } }) {
                        Icon(Icons.Outlined.Delete, "Delete period", tint = Palette.secondaryText)
                    }
                }
            }
        }

        Group(
            "Your data",
            "Lunaria has no account, no server, no analytics and no internet permission. Your data never leaves this device unless you export it.\n\nLunaria core v${coreVersion()} · Open source (MIT)",
        ) {
            Action("Export backup", Icons.Outlined.Upload) {
                runCatching { cycle.store.exportJson() }.onSuccess { json ->
                    val send = Intent(Intent.ACTION_SEND).setType("application/json").putExtra(Intent.EXTRA_TEXT, json)
                    context.startActivity(Intent.createChooser(send, "Lunaria backup"))
                }
            }
            Action("Import backup", Icons.Outlined.Download) { importer.launch(arrayOf("application/json", "text/plain")) }
            Action("Erase all data", Icons.Outlined.Delete, color = Color(0xFFD32F2F)) { confirmErase = true }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (confirmErase) {
        AlertDialog(
            onDismissRequest = { confirmErase = false },
            title = { Text("Erase all data?") },
            text = { Text("This permanently removes every period and log from this device.") },
            confirmButton = {
                TextButton(onClick = { confirmErase = false; cycle.perform { cycle.store.eraseAll() } }) {
                    Text("Erase", color = Color(0xFFD32F2F))
                }
            },
            dismissButton = { TextButton(onClick = { confirmErase = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Group(title: String, footer: String? = null, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title.uppercase(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Palette.secondaryText)
        Column(
            Modifier.fillMaxWidth().background(Palette.background.copy(alpha = 0.5f), RoundedCornerShape(20.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) { content() }
        footer?.let { Text(it, fontSize = 13.sp, color = Palette.secondaryText) }
    }
}

@Composable
private fun Stepper(label: String, value: Int, range: IntRange, enabled: Boolean = true, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label: $value days", color = Palette.text.copy(alpha = if (enabled) 1f else 0.4f), modifier = Modifier.weight(1f))
        FilledTonalIconButton(onClick = { onChange(value - 1) }, enabled = enabled && value > range.first) {
            Icon(Icons.Filled.Remove, "Decrease $label")
        }
        FilledTonalIconButton(onClick = { onChange(value + 1) }, enabled = enabled && value < range.last) {
            Icon(Icons.Filled.Add, "Increase $label")
        }
    }
}

@Composable
private fun Line(title: String, value: String) {
    Row {
        Text(title, color = Palette.text, modifier = Modifier.weight(1f))
        Text(value, color = Palette.secondaryText)
    }
}

@Composable
private fun Action(title: String, icon: ImageVector, color: Color = Palette.accent, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, null, tint = color)
        Spacer(Modifier.padding(start = 10.dp))
        Text(title, color = color, fontSize = 16.sp, modifier = Modifier.weight(1f))
    }
}
