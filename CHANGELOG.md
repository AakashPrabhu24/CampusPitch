# Changelog

All notable changes to the **CampusPitch** Android project are documented in this file.

---

## [1.0.6] - 2026-10-31
### Added
- Multi-language voice auto-detection pipeline with 16 kHz Mono audio capture.
- Real-time audio waveform visualizer component (`VoiceWaveformBar`).
- Full INR currency parser supporting Indian numbering notation (`1 cr`, `50 lakh`, `1 koti`).
- End-to-end acceptance test coverage across student intake, investor search, and multi-language routing.

### Changed
- Migrated branding and package namespace to `com.childchiller.campuspitch`.
- Updated release keystore configuration and ProGuard obfuscation rules.

---

## [1.0.5] - 2026-10-30
### Added
- Two-way in-app conversation dialog between student founders and verified investors.
- Detailed pitch view dialog with GitHub repository metadata link.

### Fixed
- Fixed silence detection timing in audio recorder to gracefully finalize recording after 2.5 seconds.
- Ensured deterministic fallback parsing when network connectivity is degraded.

---

## [1.0.4] - 2026-10-30
### Added
- Institutional investor registration gate with corporate email and domain matching validation.
- Admin review dashboard for reviewing pending fund registrations and issuing unique investor codes.

---

## [1.0.2] - 2026-10-29
### Added
- Real-time Firestore snapshot listeners via Kotlin coroutines `callbackFlow`.
- Student founder pitch intake screen with instant extraction confirmation card.
- Bank-grade Aadhaar privacy hashing with salted SHA-256 and display masking.

---

## [1.0.0] - 2026-10-28
### Added
- Initial project structure with Jetpack Compose Material 3 UI.
- Firebase Authentication and user role resolution.
