package org.lunaria.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.AndroidViewModel
import java.io.File
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale
import uniffi.lunaria_core.CalDate
import uniffi.lunaria_core.CycleSettings
import uniffi.lunaria_core.CycleStats
import uniffi.lunaria_core.CycleStatus
import uniffi.lunaria_core.CycleStore
import uniffi.lunaria_core.DayLog
import uniffi.lunaria_core.MonthView
import uniffi.lunaria_core.PeriodEntry

/**
 * Thin Kotlin facade over the Rust `CycleStore`. All cycle logic lives in Rust;
 * this class only adds Compose state so the UI recomposes after mutations.
 */
class CycleState(val store: CycleStore) {
    /** Bumped after every mutation; every query reads it so Compose tracks the dependency. */
    var revision by mutableIntStateOf(0)
        private set
    var error by mutableStateOf<String?>(null)

    val today: CalDate get() { revision; return LocalDate.now().toCal() }

    /** 0 = Sunday … 6 = Saturday, from the user's locale. */
    val firstWeekday: Int get() = WeekFields.of(Locale.getDefault()).firstDayOfWeek.value % 7

    fun perform(action: () -> Unit) {
        try {
            action()
            revision++
        } catch (e: Exception) {
            error = e.message ?: e.toString()
        }
    }

    fun month(year: Int, month: Int): MonthView? = runCatching {
        revision
        store.month(year, month.toUInt(), today, firstWeekday.toUByte())
    }.getOrNull()

    val status: CycleStatus? get() { revision; return runCatching { store.status(today) }.getOrNull() }
    val stats: CycleStats? get() { revision; return runCatching { store.stats() }.getOrNull() }
    val settings: CycleSettings get() { revision; return store.settings() }
    val periods: List<PeriodEntry> get() { revision; return store.periods() }

    fun log(date: CalDate): DayLog {
        revision
        return store.logFor(date) ?: DayLog(date = date, flow = null, mood = null, symptoms = emptyList(), note = "", intimacy = null)
    }

    fun isPeriodDay(date: CalDate): Boolean = periods.any { it.start <= date && date <= it.end }

    companion object {
        fun preview(): CycleState {
            val s = CycleState(CycleStore.inMemory())
            val today = LocalDate.now()
            s.store.startPeriod(today.minusDays(40).toCal())
            s.store.startPeriod(today.minusDays(12).toCal())
            return s
        }
    }
}

class CycleViewModel(app: Application) : AndroidViewModel(app) {
    val state: CycleState

    init {
        val file = File(app.filesDir, "lunaria/data.json")
        var openError: String? = null
        val store = try {
            CycleStore.open(file.path)
        } catch (e: Exception) {
            openError = "Could not open saved data: ${e.message}"
            CycleStore.inMemory()
        }
        state = CycleState(store).also { it.error = openError }
    }

    override fun onCleared() {
        state.store.close()
    }
}

val LocalCycle = staticCompositionLocalOf<CycleState> { error("CycleState not provided") }

// ---- Date bridging ----

fun CalDate.toLocal(): LocalDate = LocalDate.of(year, month.toInt(), day.toInt())
fun LocalDate.toCal(): CalDate = CalDate(year, monthValue.toUInt(), dayOfMonth.toUInt())

operator fun CalDate.compareTo(other: CalDate): Int = compareValuesBy(this, other, { it.year }, { it.month }, { it.day })
