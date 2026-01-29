# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added
- **PDF Export**:
  - Implemented correct travel time calculation using `travelPaidMinutes` field.
  - Added "Reisezeit" column to PDF table.
  - Added summary totals for travel time and paid time.
  - Added explicit storage space check (min 5MB) before generating PDF to prevent partial file corruption.
  - Improved error handling during file writing.

### Fixed
- **Logic**: Clarified boolean precedence in `ConfirmWorkDay`, `RecordEveningCheckIn`, and `RecordMorningCheckIn` to correctly determine `needsReview` status.
- **CSV Export**: Sanitized note fields by removing newlines and returns to prevent CSV format corruption.

### Changed
- **UI**:
  - Improved `ExportPreviewViewModel` error messaging.
  - Updated `TodayScreen` status card layout to include an edit icon for better discoverability.
