//! The "General Tools" calculators. Pure functions, exported to Swift/Kotlin.

use chrono::Duration;

use crate::date::{days_between, CalDate};
use crate::CoreError;

const PREGNANCY_DAYS: i64 = 280;
const STANDARD_CYCLE: i64 = 28;
const LUTEAL: i64 = 14;

#[derive(uniffi::Record, Debug, Clone, PartialEq)]
pub struct DueDateResult {
    pub due_date: CalDate,
    pub conception_date: CalDate,
}

#[derive(uniffi::Record, Debug, Clone, PartialEq)]
pub struct PregnancyTestResult {
    pub estimated_ovulation: CalDate,
    /// Earliest a sensitive early test may detect hCG (~10 days past ovulation).
    pub earliest_test: CalDate,
    /// First day of the missed period: most reliable result.
    pub recommended_test: CalDate,
}

#[derive(uniffi::Record, Debug, Clone, PartialEq)]
pub struct PregnancyProgress {
    pub weeks: u32,
    pub days: u32,
    /// 1, 2 or 3.
    pub trimester: u8,
    pub due_date: CalDate,
    pub days_remaining: i32,
    /// 0.0 … 1.0
    pub progress: f64,
}

fn cycle_offset(cycle_length: u32) -> i64 {
    cycle_length as i64 - STANDARD_CYCLE
}

/// Naegele's rule adjusted for cycle length: LMP + 280 days + (cycle − 28).
#[uniffi::export]
pub fn due_date(last_period_start: CalDate, cycle_length: u32) -> Result<DueDateResult, CoreError> {
    let lmp = last_period_start.to_naive()?;
    let off = cycle_offset(cycle_length);
    Ok(DueDateResult {
        due_date: (lmp + Duration::days(PREGNANCY_DAYS + off)).into(),
        conception_date: (lmp + Duration::days(LUTEAL + off)).into(),
    })
}

#[uniffi::export]
pub fn pregnancy_test_date(last_period_start: CalDate, cycle_length: u32) -> Result<PregnancyTestResult, CoreError> {
    let lmp = last_period_start.to_naive()?;
    let ovulation = lmp + Duration::days(cycle_length as i64 - LUTEAL);
    Ok(PregnancyTestResult {
        estimated_ovulation: ovulation.into(),
        earliest_test: (ovulation + Duration::days(10)).into(),
        recommended_test: (lmp + Duration::days(cycle_length as i64)).into(),
    })
}

#[uniffi::export]
pub fn pregnancy_progress(
    last_period_start: CalDate,
    cycle_length: u32,
    today: CalDate,
) -> Result<PregnancyProgress, CoreError> {
    let lmp = last_period_start.to_naive()?;
    let today = today.to_naive()?;
    let off = cycle_offset(cycle_length);
    let age = (days_between(lmp, today) - off).clamp(0, PREGNANCY_DAYS + 14);
    let due = lmp + Duration::days(PREGNANCY_DAYS + off);
    let weeks = (age / 7) as u32;
    Ok(PregnancyProgress {
        weeks,
        days: (age % 7) as u32,
        trimester: match weeks {
            0..=12 => 1,
            13..=26 => 2,
            _ => 3,
        },
        due_date: due.into(),
        days_remaining: days_between(today, due) as i32,
        progress: (age as f64 / PREGNANCY_DAYS as f64).min(1.0),
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn naegele() {
        let r = due_date(CalDate::new(2024, 1, 1), 28).unwrap();
        assert_eq!(r.due_date, CalDate::new(2024, 10, 7));
        let r = due_date(CalDate::new(2024, 1, 1), 32).unwrap();
        assert_eq!(r.due_date, CalDate::new(2024, 10, 11));
    }

    #[test]
    fn test_dates() {
        let r = pregnancy_test_date(CalDate::new(2024, 1, 1), 28).unwrap();
        assert_eq!(r.estimated_ovulation, CalDate::new(2024, 1, 15));
        assert_eq!(r.earliest_test, CalDate::new(2024, 1, 25));
        assert_eq!(r.recommended_test, CalDate::new(2024, 1, 29));
    }

    #[test]
    fn progress() {
        let r = pregnancy_progress(CalDate::new(2024, 1, 1), 28, CalDate::new(2024, 3, 1)).unwrap();
        assert_eq!((r.weeks, r.days, r.trimester), (8, 4, 1));
    }
}
