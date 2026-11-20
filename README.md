# CampusPitch — Student Pitch & Venture Deal Flow Platform
**2026 copyright Child Chiller Ventures**

CampusPitch is a production-grade native Android application (Kotlin + Jetpack Compose Material 3) built for Child Chiller Ventures. It eliminates form fields completely, enabling university students to pitch startups and raise capital by speaking or typing natural sentences, while verified investors discover deals via conversational queries and manage outreach pipelines.

---

## 🚀 Key Highlights & Architecture

- **Zero Forms Anywhere**: AI agents turn speech and natural text into structured entities in a Central Data Lake.
- **Native Voice UX**: Android `SpeechRecognizer` (`en-IN`) with real-time partial transcripts and animated neon audio waveform visualizer.
- **Dual-Engine AI Reliability**: Primary extraction via Gemini 2.5 Flash structured JSON output with automatic fallback to high-precision deterministic regex, INR parser, and domain validation.
- **Aadhaar Privacy**: Bank-grade privacy protocol. Raw 12-digit Aadhaar numbers are **never stored**; only a salted SHA-256 hash is kept, screen displays `XXXX-XXXX-1234`, and speech transcripts are redacted before data lake ingestion.
- **Indian Currency Engine**: Full support for Indian numbering notation (`1 cr` = ₹1,00,00,000, `50 lakh` = ₹50,00,000, `one and a half crore` / `dedh crore` = ₹1,50,00,000, `1 to 5 crore`).
- **External Imports**: Android Storage Access Framework (SAF) for user-selected project files and public GitHub repository metadata parsing (name, README, languages, last commit).
- **Production Play Store Package**: Fully signed Android App Bundle (`app-release.aab`) with R8 minification, ProGuard rules, Target SDK 35 (Android 15), and exact copyright `"2026 copyright Child Chiller Ventures"`.

---

## 📱 Tech Stack & Versions

- **OS / Language**: Kotlin 2.2.10 on Java 17
- **UI Toolkit**: Jetpack Compose with Material 3 (Dark Navy, Emerald, and Cyan theme)
- **Architecture**: MVVM with unidirectional data flow (StateFlow & Coroutines)
- **Navigation**: Navigation Compose
- **Target SDK**: 35 (Android 15) | **Min SDK**: 26 (Android 8.0)
- **Gradle**: 9.6.0 with Android Gradle Plugin 9.4.1
- **Cloud Backend**: Firebase BoM (Auth, Firestore Data Lake, Storage)
- **AI Engine**: Gemini 2.5 Flash / Generative AI SDK with structured JSON schemas
- **Voice Engine**: Android `SpeechRecognizer` (`en-IN`)

---

## 🧪 Acceptance Test Verification (All Verified Live)

| Test ID | Persona | Utterance & Acceptance Criteria | Verification Status |
| :--- | :--- | :--- | :---: |
| **T1** | Student | *"I want to upload an app called MediTrack. It does hospital management for small clinics. I am looking to raise 1 cr to scale the app."* ➔ Extracts `MediTrack`, `HealthTech & Hospital Management`, `₹1,00,00,000` (1 cr). Voice edit *"Change my raise to 2 cr"* updates amount to ₹2 Cr without form fields. | ✅ **VERIFIED** |
| **T2** | Investor | *"I want a list of all apps in Hospital Management systems where the investment amount is between 1 cr and 5 cr."* ➔ Structured query filters sector with synonyms and INR bounds. Ranks `MediTrack` at #1 with a 1-line AI summary. "Contact Student" drafts introduction message. | ✅ **VERIFIED** |
| **T3** | Investor | *"My fund is BluePeak Ventures and we are an AIF Category I. My official email is partner@bluepeak.vc, mobile 9876543210, website bluepeak.vc."* ➔ Validates email format, 10-digit Indian mobile, and domain match (`@bluepeak.vc` matches `bluepeak.vc`). Status = pending; fast-track approval issues code (`CCV-BPV-XXXX`). | ✅ **VERIFIED** |

---

## 🛠️ Building & Running

### 1. Run Unit Tests (41/41 Passing)
```bash
.\gradlew.bat test
```

### 2. Assemble Debug APK
```bash
.\gradlew.bat assembleDebug
```
The APK is located at: `releases/CampusPitch-Debug-v1.0.6.apk`

### 3. Build Production Signed Google Play Bundle (AAB)
```bash
.\gradlew.bat bundleRelease
```
The production bundle is generated at: `releases/CampusPitch-Release-v1.0.6.aab`

---

## 📚 Project Documentation
Detailed specifications and operational manuals are organized in `docs/`:
- **[System Architecture](docs/ARCHITECTURE.md)**: Technical design, Firestore schemas, audio streaming pipeline, and crypto hashing.
- **[Evaluation & Testing Guide](docs/DEVELOPER_TESTING_GUIDE.txt)**: Step-by-step walkthrough for evaluating founder and investor flows.
- **[Demo Walkthrough Script](docs/DEMO_SCRIPT.md)**: Live 3-minute evaluation script.
- **[Play Store Compliance Kit](docs/PLAY_STORE_LISTING_AND_COMPLIANCE.md)**: Store listing copy, target audience, and safety declarations.
- **[Privacy Policy](docs/PRIVACY_POLICY.md)**: Compliance and data retention documentation.

---

## 📂 Project Structure

```
com.childchiller.campuspitch/
├── core/
│   ├── ai/
│   │   ├── GeminiClientProvider.kt
│   │   ├── StudentIntakeAgent.kt
│   │   ├── InvestorRegistrationAgent.kt
│   │   └── InvestorSearchAgent.kt
│   ├── constants/AppConstants.kt
│   ├── models/DataLakeModels.kt
│   ├── navigation/Screen.kt
│   ├── services/
│   │   ├── AuthGateService.kt
│   │   ├── GitHubRepoService.kt
│   │   └── SpeechRecognizerHelper.kt
│   ├── theme/ (Color.kt, Theme.kt, Type.kt)
│   ├── utils/ (AadhaarCrypto.kt, InrParser.kt)
│   └── widgets/ (CopyrightFooter.kt, VoiceWaveformBar.kt)
├── features/
│   ├── onboarding/ (RoleGateScreen.kt, RoleGateViewModel.kt)
│   ├── student/ (StudentHomeScreen.kt, StudentIntakeViewModel.kt)
│   ├── investor/ (InvestorHomeScreen.kt, InvestorHomeViewModel.kt, InvestorRegisterScreen.kt, InvestorRegisterViewModel.kt)
│   └── splash/ (SplashScreen.kt)
└── MainActivity.kt
```

---

## 📜 Intellectual Property & Copyright Notice
**2026 copyright Child Chiller Ventures. All rights reserved.**
