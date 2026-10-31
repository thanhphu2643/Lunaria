//! Lunaria core: cycle prediction, calculators and local storage.
//! Exposed to Swift (iOS) and Kotlin (Android) through UniFFI.

mod calculators;
mod cycle;
mod date;
mod store;

pub use calculators::*;
pub use cycle::*;
pub use date::CalDate;
pub use store::*;

uniffi::setup_scaffolding!();

#[derive(uniffi::Error, thiserror::Error, Debug)]
#[uniffi(flat_error)]
pub enum CoreError {
    #[error("invalid date {year}-{month}-{day}")]
    InvalidDate { year: i32, month: u32, day: u32 },
    #[error("no period found near this date")]
    NoPeriod,
    #[error("storage error: {0}")]
    Io(#[from] std::io::Error),
    #[error("data format error: {0}")]
    Json(#[from] serde_json::Error),
}

#[uniffi::export]
pub fn core_version() -> String {
    env!("CARGO_PKG_VERSION").to_string()
}
