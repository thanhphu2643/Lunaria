//! Cycle prediction: classifies every day as period / ovulation / fertile / safe.
//!
//! Model (calendar method, the same one most trackers use):
//! - A cycle runs from one period start to the next.
//! - Ovulation happens `luteal_length` days before the next period starts.
//! - The fertile window is the 5 days before ovulation, ovulation day and the day after.
//! - Future cycles are projected from the last logged start using the average cycle length.
//!
//! This is an estimate, not contraception or medical advice.

use chrono::{Datelike, Duration, NaiveDate, Weekday};
use serde::{Deserialize, Serialize};

use crate::date::{days_between, CalDate};
use crate::CoreError;

pub const FERTILE_DAYS_BEFORE_OVULATION: i64 = 5;
pub const FERTILE_DAYS_AFTER_OVULATION: i64 = 1;
const MIN_CYCLE: i64 = 15;
const MAX_CYCLE: i64 = 60;
const HISTORY_FOR_AVERAGE: usize = 6;

#[derive(uniffi::Record, Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub struct PeriodEntry {
    pub start: CalDate,
    /// Inclusive last day of bleeding.
    pub end: CalDate,
}

#[derive(uniffi::Record, Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub struct CycleSettings {
    pub cycle_length: u32,
    pub period_length: u32,
    pub luteal_length: u32,
    /// Recompute cycle/period lengths from logged history.
    pub auto_adjust: bool,
}

impl Default for CycleSettings {
    fn default() -> Self {
        Self { cycle_length: 28, period_length: 5, luteal_length: 14, auto_adjust: true }
    }
}

#[derive(uniffi::Enum, Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum DayKind {
    /// Logged period day.
    Period,
    /// Projected period day.
    PredictedPeriod,
    Ovulation,
    Fertile,
    Safe,
    /// No data to classify (before the first log).
    Unknown,
}

#[derive(uniffi::Enum, Debug, Clone, Copy, PartialEq, Eq)]
pub enum CyclePhase {
    Menstrual,
    Follicular,
    Fertile,
    Luteal,
}

#[derive(uniffi::Record, Debug, Clone, PartialEq)]
pub struct CalendarDay {
    pub date: CalDate,
    pub in_month: bool,
    pub is_today: bool,
    pub kind: DayKind,
}

#[derive(uniffi::Record, Debug, Clone, PartialEq)]
pub struct MonthView {
    pub year: i32,
    pub month: u32,
    /// Always a multiple of 7, starting on the configured first weekday.
    pub days: Vec<CalendarDay>,
}

#[derive(uniffi::Record, Debug, Clone, PartialEq)]
pub struct CycleStatus {
    /// 1-based day of the current cycle.
    pub cycle_day: u32,
    pub cycle_length: u32,
    pub phase: CyclePhase,
    pub last_period_start: CalDate,
    pub next_period_start: CalDate,
    pub days_until_next_period: i32,
    pub next_ovulation: CalDate,
    pub days_late: u32,
    pub fertile_start: CalDate,
    pub fertile_end: CalDate,
}

#[derive(uniffi::Record, Debug, Clone, PartialEq)]
pub struct CycleStats {
    pub average_cycle_length: u32,
    pub average_period_length: u32,
    pub shortest_cycle: Option<u32>,
    pub longest_cycle: Option<u32>,
    pub logged_cycles: u32,
}

/// Immutable snapshot of history + settings that answers prediction questions.
pub struct Predictor {
    /// Sorted by start, validated.
    periods: Vec<(NaiveDate, NaiveDate)>,
    cycle_len: i64,
    period_len: i64,
    luteal_len: i64,
}

impl Predictor {
    pub fn new(periods: &[PeriodEntry], settings: CycleSettings) -> Result<Self, CoreError> {
        let mut ps = periods
            .iter()
            .map(|p| Ok((p.start.to_naive()?, p.end.to_naive()?)))
            .collect::<Result<Vec<_>, CoreError>>()?;
        ps.sort();

        let effective = effective_settings(&ps, settings);
        Ok(Self {
            periods: ps,
            cycle_len: effective.cycle_length as i64,
            period_len: effective.period_length as i64,
            luteal_len: (effective.luteal_length as i64).min(effective.cycle_length as i64 - 1),
        })
    }

    pub fn cycle_length(&self) -> u32 {
        self.cycle_len as u32
    }

    pub fn period_length(&self) -> u32 {
        self.period_len as u32
    }

    /// Start of the first projected (future) period, never earlier than `today`.
    /// If the period is late, the prediction is moved to today.
    fn first_projection(&self, today: NaiveDate) -> Option<NaiveDate> {
        let last = self.periods.last()?.0;
        Some((last + Duration::days(self.cycle_len)).max(today.max(last + Duration::days(1))))
    }

    /// Cycle containing `date`: (start, next_start, start_is_logged).
    fn cycle_for(&self, date: NaiveDate, today: NaiveDate) -> Option<(NaiveDate, NaiveDate, bool)> {
        let first = self.periods.first()?.0;
        if date < first {
            return None;
        }
        // Within logged history.
        let idx = self.periods.partition_point(|p| p.0 <= date);
        if idx < self.periods.len() {
            return Some((self.periods[idx - 1].0, self.periods[idx].0, true));
        }
        let last = self.periods[idx - 1].0;
        let proj = self.first_projection(today)?;
        if date < proj {
            return Some((last, proj, true));
        }
        let k = days_between(proj, date) / self.cycle_len;
        let start = proj + Duration::days(k * self.cycle_len);
        Some((start, start + Duration::days(self.cycle_len), false))
    }

    pub fn classify(&self, date: NaiveDate, today: NaiveDate) -> DayKind {
        if self.periods.iter().any(|(s, e)| *s <= date && date <= *e) {
            return DayKind::Period;
        }
        let Some((start, next, logged)) = self.cycle_for(date, today) else {
            return DayKind::Unknown;
        };
        if !logged && date < start + Duration::days(self.period_len) {
            return DayKind::PredictedPeriod;
        }
        let ovulation = next - Duration::days(self.luteal_len);
        if date == ovulation {
            DayKind::Ovulation
        } else if date >= ovulation - Duration::days(FERTILE_DAYS_BEFORE_OVULATION)
            && date <= ovulation + Duration::days(FERTILE_DAYS_AFTER_OVULATION)
        {
            DayKind::Fertile
        } else {
            DayKind::Safe
        }
    }

    pub fn month(&self, year: i32, month: u32, today: NaiveDate, week_start: Weekday) -> Result<MonthView, CoreError> {
        let first = NaiveDate::from_ymd_opt(year, month, 1).ok_or(CoreError::InvalidDate { year, month, day: 1 })?;
        let lead = (first.weekday().num_days_from_monday() + 7 - week_start.num_days_from_monday()) % 7;
        let grid_start = first - Duration::days(lead as i64);
        let next_month = if month == 12 {
            NaiveDate::from_ymd_opt(year + 1, 1, 1)
        } else {
            NaiveDate::from_ymd_opt(year, month + 1, 1)
        }
        .ok_or(CoreError::InvalidDate { year, month, day: 1 })?;
        let used = days_between(grid_start, next_month);
        let cells = ((used + 6) / 7) * 7;

        let days = (0..cells)
            .map(|i| {
                let d = grid_start + Duration::days(i);
                CalendarDay {
                    date: d.into(),
                    in_month: d.month() == month && d.year() == year,
                    is_today: d == today,
                    kind: self.classify(d, today),
                }
            })
            .collect();
        Ok(MonthView { year, month, days })
    }

    pub fn status(&self, today: NaiveDate) -> Option<CycleStatus> {
        let (start, next, logged) = self.cycle_for(today, today)?;
        // Projections never start before today, so a projected cycle containing
        // today means the period is due today or late.
        let (anchor, next, days_late) = if logged {
            (start, next, 0)
        } else {
            let last = self.periods.last()?.0;
            let expected = last + Duration::days(self.cycle_len);
            (last, start, days_between(expected, today).max(0))
        };
        let ovulation = next - Duration::days(self.luteal_len);
        let fertile_start = ovulation - Duration::days(FERTILE_DAYS_BEFORE_OVULATION);
        let fertile_end = ovulation + Duration::days(FERTILE_DAYS_AFTER_OVULATION);
        let cycle_day = days_between(anchor, today) + 1;

        let phase = match if logged { self.classify(today, today) } else { DayKind::Safe } {
            DayKind::Period | DayKind::PredictedPeriod => CyclePhase::Menstrual,
            DayKind::Ovulation | DayKind::Fertile => CyclePhase::Fertile,
            _ if today < fertile_start => CyclePhase::Follicular,
            _ => CyclePhase::Luteal,
        };

        Some(CycleStatus {
            cycle_day: cycle_day as u32,
            cycle_length: self.cycle_len as u32,
            phase,
            last_period_start: anchor.into(),
            next_period_start: next.into(),
            days_until_next_period: days_between(today, next) as i32,
            next_ovulation: ovulation.into(),
            days_late: days_late as u32,
            fertile_start: fertile_start.into(),
            fertile_end: fertile_end.into(),
        })
    }

    pub fn stats(&self) -> CycleStats {
        let lengths = cycle_lengths(&self.periods);
        CycleStats {
            average_cycle_length: self.cycle_len as u32,
            average_period_length: self.period_len as u32,
            shortest_cycle: lengths.iter().min().map(|v| *v as u32),
            longest_cycle: lengths.iter().max().map(|v| *v as u32),
            logged_cycles: lengths.len() as u32,
        }
    }
}

fn cycle_lengths(periods: &[(NaiveDate, NaiveDate)]) -> Vec<i64> {
    periods
        .windows(2)
        .map(|w| days_between(w[0].0, w[1].0))
        .filter(|l| (MIN_CYCLE..=MAX_CYCLE).contains(l))
        .collect()
}

fn mean_of_recent(values: &[i64]) -> Option<i64> {
    let recent = &values[values.len().saturating_sub(HISTORY_FOR_AVERAGE)..];
    if recent.is_empty() {
        return None;
    }
    let sum: i64 = recent.iter().sum();
    Some((sum as f64 / recent.len() as f64).round() as i64)
}

fn effective_settings(periods: &[(NaiveDate, NaiveDate)], s: CycleSettings) -> CycleSettings {
    let mut out = s;
    out.cycle_length = out.cycle_length.clamp(MIN_CYCLE as u32, MAX_CYCLE as u32);
    out.period_length = out.period_length.clamp(1, 14);
    out.luteal_length = out.luteal_length.clamp(7, 20);
    if !s.auto_adjust {
        return out;
    }
    if let Some(c) = mean_of_recent(&cycle_lengths(periods)) {
        out.cycle_length = c as u32;
    }
    let durations: Vec<i64> = periods
        .iter()
        .map(|(s, e)| days_between(*s, *e) + 1)
        .filter(|d| (1..=14).contains(d))
        .collect();
    if let Some(p) = mean_of_recent(&durations) {
        out.period_length = p as u32;
    }
    out
}

pub fn weekday_from_index(i: u8) -> Weekday {
    // 0 = Sunday … 6 = Saturday (matches Calendar.firstWeekday - 1 on Apple platforms).
    match i % 7 {
        0 => Weekday::Sun,
        1 => Weekday::Mon,
        2 => Weekday::Tue,
        3 => Weekday::Wed,
        4 => Weekday::Thu,
        5 => Weekday::Fri,
        _ => Weekday::Sat,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn d(y: i32, m: u32, day: u32) -> NaiveDate {
        NaiveDate::from_ymd_opt(y, m, day).unwrap()
    }
    fn p(s: NaiveDate, e: NaiveDate) -> PeriodEntry {
        PeriodEntry { start: s.into(), end: e.into() }
    }

    #[test]
    fn classifies_like_the_design_month() {
        // Period Jul 10–15, 28-day cycle → next Aug 7, ovulation Jul 24, fertile Jul 19–25.
        let pr = Predictor::new(&[p(d(2023, 7, 10), d(2023, 7, 15))], CycleSettings::default()).unwrap();
        let today = d(2023, 7, 16);
        assert_eq!(pr.classify(d(2023, 7, 12), today), DayKind::Period);
        assert_eq!(pr.classify(d(2023, 7, 24), today), DayKind::Ovulation);
        assert_eq!(pr.classify(d(2023, 7, 19), today), DayKind::Fertile);
        assert_eq!(pr.classify(d(2023, 7, 25), today), DayKind::Fertile);
        assert_eq!(pr.classify(d(2023, 7, 18), today), DayKind::Safe);
        assert_eq!(pr.classify(d(2023, 8, 7), today), DayKind::PredictedPeriod);
        assert_eq!(pr.classify(d(2023, 7, 1), today), DayKind::Unknown);
    }

    #[test]
    fn month_grid_starts_on_sunday() {
        let pr = Predictor::new(&[], CycleSettings::default()).unwrap();
        let m = pr.month(2023, 7, d(2023, 7, 1), Weekday::Sun).unwrap();
        assert_eq!(m.days.len(), 42);
        assert_eq!(m.days[0].date, CalDate::new(2023, 6, 25));
        assert!(m.days[6].in_month);
    }

    #[test]
    fn averages_history() {
        let ps = [
            p(d(2024, 1, 1), d(2024, 1, 4)),
            p(d(2024, 1, 31), d(2024, 2, 3)),
            p(d(2024, 3, 1), d(2024, 3, 4)),
        ];
        let pr = Predictor::new(&ps, CycleSettings::default()).unwrap();
        assert_eq!(pr.cycle_length(), 30);
        assert_eq!(pr.period_length(), 4);
        assert_eq!(pr.stats().shortest_cycle, Some(30));
    }

    #[test]
    fn late_period_moves_prediction_to_today() {
        let pr = Predictor::new(&[p(d(2024, 1, 1), d(2024, 1, 5))], CycleSettings::default()).unwrap();
        let today = d(2024, 2, 3); // expected Jan 29 → 5 days late
        let st = pr.status(today).unwrap();
        assert_eq!(st.days_late, 5);
        assert_eq!(st.next_period_start, today.into());
        assert_eq!(pr.classify(today, today), DayKind::PredictedPeriod);
    }

    #[test]
    fn status_mid_cycle() {
        let pr = Predictor::new(&[p(d(2024, 1, 1), d(2024, 1, 5))], CycleSettings::default()).unwrap();
        let st = pr.status(d(2024, 1, 10)).unwrap();
        assert_eq!(st.cycle_day, 10);
        assert_eq!(st.days_until_next_period, 19);
        assert_eq!(st.next_ovulation, CalDate::new(2024, 1, 15));
        assert_eq!(st.phase, CyclePhase::Fertile);
    }
}
