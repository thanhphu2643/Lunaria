use chrono::{Datelike, Duration, NaiveDate};
use serde::{Deserialize, Serialize};

use crate::CoreError;

/// A calendar date without time zone. Plain record so it maps cleanly to Swift/Kotlin.
#[derive(uniffi::Record, Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash, Serialize, Deserialize)]
pub struct CalDate {
    pub year: i32,
    pub month: u32,
    pub day: u32,
}

impl CalDate {
    pub fn new(year: i32, month: u32, day: u32) -> Self {
        Self { year, month, day }
    }

    pub fn to_naive(self) -> Result<NaiveDate, CoreError> {
        NaiveDate::from_ymd_opt(self.year, self.month, self.day).ok_or(CoreError::InvalidDate {
            year: self.year,
            month: self.month,
            day: self.day,
        })
    }

    pub fn add_days(self, days: i64) -> Result<Self, CoreError> {
        Ok((self.to_naive()? + Duration::days(days)).into())
    }
}

impl From<NaiveDate> for CalDate {
    fn from(d: NaiveDate) -> Self {
        Self { year: d.year(), month: d.month(), day: d.day() }
    }
}

/// Days from `a` to `b` (positive when `b` is later).
pub fn days_between(a: NaiveDate, b: NaiveDate) -> i64 {
    (b - a).num_days()
}
