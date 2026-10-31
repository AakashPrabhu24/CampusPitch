# CampusPitch — Technical Architecture & System Design
**2026 copyright Child Chiller Ventures**

---

### System Overview
CampusPitch is a native Android application engineered with Kotlin, Jetpack Compose, and Material Design 3. The platform provides a conversational deal flow ecosystem connecting campus founders with accredited investors and venture funds.

---

### Component Architecture

```
CampusPitch Architecture
├── Presentation Layer (Jetpack Compose + MVVM)
│   ├── Student: Voice & Text Intake Studio, Pitch Feed, Deal Status
│   ├── Investor: Natural Query Search, Deal Discovery, Pipeline Management
│   ├── Onboarding: Role Gate, Authentication, Domain Verification
│   └── Admin: Institutional Review & Code Issuance
├── Domain & Business Services
│   ├── Voice & Audio: 16 kHz Mono AudioRecord streaming with RMS visualization
│   ├── Natural Language Service: Entity extraction and structured schema parsing
│   ├── Currency Engine: Indian Numbering System normalization (Crore/Lakh/Koti)
│   └── Crypto Security: Salted SHA-256 Aadhaar masking and verification
└── Data Lake Layer (Firebase Firestore)
    ├── Raw Zone: Ephemeral audio transcripts & telemetry logs
    ├── Curated Zone: User profiles, MVP pitches, investor pipelines, funding terms
    └── Audit Zone: Ingestion verification and request latency tracking
```

---

### Core Data Lake Schemas

```
Central Data Lake
├── Raw Zone
│   └── raw_inputs (id, uid, inputChannel, rawTranscriptRedacted, transcriptEnglish, languageCode, languageName, modelVersion, createdAt)
├── Curated Zone
│   ├── users (uid, email, role, ageVerified18Plus, privacyConsentGranted)
│   ├── students (uid, studentName, collegeName, collegeId, aadhaarSaltedHash, aadhaarLast4, mobile)
│   ├── mvps (id, studentUid, appName, description, sector, stage, raiseAmountInr, links)
│   ├── investors (uid, fundName, aifCategory, officialEmail, mobile, website, status, code)
│   ├── outreach (id, investorUid, studentUid, mvpId, introMessage, status, updatedAt)
│   └── funding (id, studentUid, investorUid, mvpId, amountInr, termSheetDate, status)
└── Audit Zone
    └── audit_runs (id, uid, serviceName, rawInputId, latencyMs, timestamp)
```

---

### Security, Compliance & Privacy Engineering
1. **Audio Ephemerality**: Spoken audio is recorded via native AudioRecord at 16 kHz mono, processed in-flight for speech recognition, and temporary cache files are immediately destroyed. No raw audio is retained on disk or cloud storage.
2. **Identity Privacy**: Student Aadhaar identification complies with standard data minimization guidelines. The raw 12-digit number is never stored in plaintext. It is hashed using a salted SHA-256 algorithm (`CCV_CAMPUSPITCH_2026_SALT_V1`), masked on UI displays as `XXXX-XXXX-1234`, and redacted from transcripts prior to saving.
3. **Currency Parsing**: Full support for Indian financial numbering notation (`1 cr`, `50 lakh`, `1 koti`, `dedh crore`, etc.) with boundary validation and deterministic fallbacks.
4. **Scoped Permissions**: Utilizes the Android Storage Access Framework (SAF) to access user-selected attachments without requesting broad storage permissions.
