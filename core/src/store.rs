//! Local-only persistence. Data is a single JSON file the host app chooses
//! (e.g. the iOS Application Support directory). Nothing leaves the device.

use std::fs;
use std::path::PathBuf;
use std::sync::{Arc, Mutex};

use chrono::{Duration, NaiveDate};
use serde::{Deserialize, Serialize};

use crate::cycle::{weekday_from_index, CycleSettings, CycleStats, CycleStatus, MonthView, PeriodEntry, Predictor};
use crate::date::CalDate;
use crate::CoreError;

const SCHEMA_VERSION: u32 = 1;

#[derive(uniffi::Enum, Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum Flow {
    Spotting,
    Light,
    Medium,
    Heavy,
}

#[derive(uniffi::Enum, Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum Mood {
    Happy,
    Calm,
    Sensitive,
    Sad,
    Irritated,
    Anxious,
}

#[derive(uniffi::Enum, Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum Symptom {
    Cramps,
    Headache,
    Bloating,
    Acne,
    TenderBreasts,
    Fatigue,
    BackPain,
    Nausea,
    Cravings,
    Insomnia,
}

/// Sexual activity on a day, with an optional private note.
#[derive(uniffi::Record, Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct Intimacy {
    pub protected: bool,
    #[serde(default)]
    pub note: String,
}

#[derive(uniffi::Record, Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct DayLog {
    pub date: CalDate,
    pub flow: Option<Flow>,
    pub mood: Option<Mood>,
    pub symptoms: Vec<Symptom>,
    pub note: String,
    #[serde(default)]
    #[uniffi(default = None)]
    pub intimacy: Option<Intimacy>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
struct Data {
    version: u32,
    settings: CycleSettings,
    periods: Vec<PeriodEntry>,
    #[serde(default)]
    logs: Vec<DayLog>,
}

impl Default for Data {
    fn default() -> Self {
        Self { version: SCHEMA_VERSION, settings: CycleSettings::default(), periods: vec![], logs: vec![] }
    }
}

#[derive(uniffi::Object)]
pub struct CycleStore {
    path: Option<PathBuf>,
    data: Mutex<Data>,
}

impl CycleStore {
    fn with<T>(&self, f: impl FnOnce(&Data) -> T) -> T {
        f(&self.data.lock().unwrap())
    }

    fn mutate(&self, f: impl FnOnce(&mut Data) -> Result<(), CoreError>) -> Result<(), CoreError> {
        let mut data = self.data.lock().unwrap();
        let mut next = data.clone();
        f(&mut next)?;
        next.periods.sort_by_key(|p| p.start);
        next.logs.sort_by_key(|l| l.date);
        if let Some(path) = &self.path {
            let json = serde_json::to_vec_pretty(&next)?;
            let tmp = path.with_extension("tmp");
            fs::write(&tmp, json)?;
            fs::rename(&tmp, path)?;
        }
        *data = next;
        Ok(())
    }

    fn predictor(&self) -> Result<Predictor, CoreError> {
        self.with(|d| Predictor::new(&d.periods, d.settings))
    }
}

#[uniffi::export]
impl CycleStore {
    /// Opens (or creates) the store at `path`.
    #[uniffi::constructor]
    pub fn open(path: String) -> Result<Arc<Self>, CoreError> {
        let path = PathBuf::from(path);
        let data = match fs::read(&path) {
            Ok(bytes) => serde_json::from_slice(&bytes)?,
            Err(e) if e.kind() == std::io::ErrorKind::NotFound => Data::default(),
            Err(e) => return Err(e.into()),
        };
        if let Some(dir) = path.parent() {
            fs::create_dir_all(dir)?;
        }
        Ok(Arc::new(Self { path: Some(path), data: Mutex::new(data) }))
    }

    /// Non-persistent store for previews and tests.
    #[uniffi::constructor]
    pub fn in_memory() -> Arc<Self> {
        Arc::new(Self { path: None, data: Mutex::new(Data::default()) })
    }

    pub fn settings(&self) -> CycleSettings {
        self.with(|d| d.settings)
    }

    pub fn update_settings(&self, settings: CycleSettings) -> Result<(), CoreError> {
        self.mutate(|d| {
            d.settings = settings;
            Ok(())
        })
    }

    pub fn periods(&self) -> Vec<PeriodEntry> {
        self.with(|d| d.periods.clone())
    }

    /// Logs a period starting on `start`. Its length defaults to the average period length.
    /// A start within an existing period, or right after one, is ignored / merged.
    pub fn start_period(&self, start: CalDate) -> Result<(), CoreError> {
        let s = start.to_naive()?;
        let len = self.predictor()?.period_length() as i64;
        self.mutate(|d| {
            let end = s + Duration::days(len - 1);
            for p in d.periods.iter_mut() {
                let (ps, pe) = (p.start.to_naive()?, p.end.to_naive()?);
                if s >= ps && s <= pe + Duration::days(1) {
                    return Ok(()); // already covered
                }
                if s < ps && end + Duration::days(1) >= ps {
                    p.start = start; // extends an upcoming log backwards
                    return Ok(());
                }
            }
            d.periods.push(PeriodEntry { start, end: end.into() });
            Ok(())
        })
    }

    /// Marks `date` as the last bleeding day of the period that contains it (or ended just before it).
    pub fn end_period(&self, date: CalDate) -> Result<(), CoreError> {
        let day = date.to_naive()?;
        self.mutate(|d| {
            let target = find_period(&d.periods, day, 7)?;
            match target {
                Some(i) if d.periods[i].start <= date => {
                    d.periods[i].end = date;
                    Ok(())
                }
                _ => Err(CoreError::NoPeriod),
            }
        })
    }

    /// Toggles a single day: removes it from a period, or adds it (extending a neighbouring period).
    pub fn toggle_period_day(&self, date: CalDate) -> Result<(), CoreError> {
        let day = date.to_naive()?;
        self.mutate(|d| {
            if let Some(i) = find_period(&d.periods, day, 0)? {
                let p = d.periods[i];
                if p.start == p.end {
                    d.periods.remove(i);
                } else if p.start == date {
                    d.periods[i].start = date.add_days(1)?;
                } else if p.end == date {
                    d.periods[i].end = date.add_days(-1)?;
                } else {
                    // Split in two.
                    d.periods[i].end = date.add_days(-1)?;
                    d.periods.push(PeriodEntry { start: date.add_days(1)?, end: p.end });
                }
                return Ok(());
            }
            let before = d.periods.iter().position(|p| p.end == date.add_days(-1).unwrap_or(date));
            let after = d.periods.iter().position(|p| p.start == date.add_days(1).unwrap_or(date));
            match (before, after) {
                (Some(b), Some(a)) => {
                    d.periods[b].end = d.periods[a].end;
                    d.periods.remove(a);
                }
                (Some(b), None) => d.periods[b].end = date,
                (None, Some(a)) => d.periods[a].start = date,
                (None, None) => d.periods.push(PeriodEntry { start: date, end: date }),
            }
            Ok(())
        })
    }

    pub fn delete_period(&self, start: CalDate) -> Result<(), CoreError> {
        self.mutate(|d| {
            d.periods.retain(|p| p.start != start);
            Ok(())
        })
    }

    pub fn log_for(&self, date: CalDate) -> Option<DayLog> {
        self.with(|d| d.logs.iter().find(|l| l.date == date).cloned())
    }

    /// Saves a day log; an empty log deletes the entry.
    pub fn save_log(&self, log: DayLog) -> Result<(), CoreError> {
        log.date.to_naive()?;
        self.mutate(|d| {
            d.logs.retain(|l| l.date != log.date);
            let empty = log.flow.is_none() && log.mood.is_none() && log.symptoms.is_empty() && log.note.trim().is_empty()
                && log.intimacy.is_none();
            if !empty {
                d.logs.push(log);
            }
            Ok(())
        })
    }

    /// Calendar grid for a month. `first_weekday`: 0 = Sunday … 6 = Saturday.
    pub fn month(&self, year: i32, month: u32, today: CalDate, first_weekday: u8) -> Result<MonthView, CoreError> {
        self.predictor()?.month(year, month, today.to_naive()?, weekday_from_index(first_weekday))
    }

    pub fn status(&self, today: CalDate) -> Result<Option<CycleStatus>, CoreError> {
        Ok(self.predictor()?.status(today.to_naive()?))
    }

    pub fn stats(&self) -> Result<CycleStats, CoreError> {
        Ok(self.predictor()?.stats())
    }

    /// Full JSON export of all data (for backups / moving devices).
    pub fn export_json(&self) -> Result<String, CoreError> {
        self.with(|d| Ok(serde_json::to_string_pretty(d)?))
    }

    /// Replaces all data with a previous export.
    pub fn import_json(&self, json: String) -> Result<(), CoreError> {
        let incoming: Data = serde_json::from_str(&json)?;
        self.mutate(|d| {
            *d = incoming;
            Ok(())
        })
    }

    pub fn erase_all(&self) -> Result<(), CoreError> {
        self.mutate(|d| {
            *d = Data::default();
            Ok(())
        })
    }
}

/// Index of the period containing `day`, or ending at most `grace` days before it.
fn find_period(periods: &[PeriodEntry], day: NaiveDate, grace: i64) -> Result<Option<usize>, CoreError> {
    for (i, p) in periods.iter().enumerate() {
        let (s, e) = (p.start.to_naive()?, p.end.to_naive()?);
        if day >= s && day <= e + Duration::days(grace) {
            return Ok(Some(i));
        }
    }
    Ok(None)
}

#[cfg(test)]
mod tests {
    use super::*;

    fn cd(y: i32, m: u32, d: u32) -> CalDate {
        CalDate::new(y, m, d)
    }

    #[test]
    fn start_end_toggle() {
        let s = CycleStore::in_memory();
        s.start_period(cd(2024, 1, 1)).unwrap();
        assert_eq!(s.periods(), vec![PeriodEntry { start: cd(2024, 1, 1), end: cd(2024, 1, 5) }]);
        s.end_period(cd(2024, 1, 3)).unwrap();
        assert_eq!(s.periods()[0].end, cd(2024, 1, 3));
        s.toggle_period_day(cd(2024, 1, 4)).unwrap();
        assert_eq!(s.periods()[0].end, cd(2024, 1, 4));
        s.toggle_period_day(cd(2024, 1, 2)).unwrap();
        assert_eq!(s.periods().len(), 2);
        s.toggle_period_day(cd(2024, 1, 2)).unwrap();
        assert_eq!(s.periods(), vec![PeriodEntry { start: cd(2024, 1, 1), end: cd(2024, 1, 4) }]);
    }

    #[test]
    fn old_logs_without_intimacy_still_load() {
        let json = r#"{"version":1,"settings":{"cycle_length":28,"period_length":5,"luteal_length":14,"auto_adjust":true},"periods":[],"logs":[{"date":{"year":2024,"month":1,"day":2},"flow":null,"mood":null,"symptoms":[],"note":"hi"}]}"#;
        let s = CycleStore::in_memory();
        s.import_json(json.into()).unwrap();
        assert_eq!(s.log_for(cd(2024, 1, 2)).unwrap().intimacy, None);
    }

    #[test]
    fn persists_and_reloads() {
        let dir = tempfile::tempdir().unwrap();
        let path = dir.path().join("data.json").to_string_lossy().to_string();
        let s = CycleStore::open(path.clone()).unwrap();
        s.start_period(cd(2024, 5, 1)).unwrap();
        s.save_log(DayLog { date: cd(2024, 5, 2), flow: Some(Flow::Heavy), mood: None, symptoms: vec![Symptom::Cramps], note: String::new(), intimacy: None })
            .unwrap();
        s.save_log(DayLog {
            date: cd(2024, 5, 10),
            flow: None,
            mood: None,
            symptoms: vec![],
            note: String::new(),
            intimacy: Some(Intimacy { protected: true, note: "anniversary".into() }),
        })
        .unwrap();
        let s2 = CycleStore::open(path).unwrap();
        assert_eq!(s2.periods().len(), 1);
        assert_eq!(s2.log_for(cd(2024, 5, 2)).unwrap().flow, Some(Flow::Heavy));
        assert_eq!(s2.log_for(cd(2024, 5, 10)).unwrap().intimacy.unwrap().note, "anniversary");
        let json = s2.export_json().unwrap();
        let s3 = CycleStore::in_memory();
        s3.import_json(json).unwrap();
        assert_eq!(s3.periods(), s2.periods());
    }
}
