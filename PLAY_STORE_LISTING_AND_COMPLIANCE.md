# CampusPitch - Google Play Console Release Kit & Compliance Guide
**2026 copyright Child Chiller Ventures**

---

## 1. Store Listing Metadata

### App Title (max 30 characters)
`CampusPitch - AI Deal Flow`

### Short Description (max 80 characters)
`Zero-form multi-language voice platform connecting campus founders with VCs.`

### Full Description (max 4000 characters)
```text
CampusPitch is an AI-native venture platform where university student founders pitch startups and raise capital by simply speaking or typing one natural sentence in any language—with zero forms and zero language pickers.

Verified angel investors, venture capital funds, and Alternative Investment Funds (AIFs) find student startups using conversational spoken thesis queries in any language (e.g. Hindi, English, Spanish), explore ranked deal cards with one-line AI summaries, and manage intro pipelines from initial contact to term sheets.

KEY CAPABILITIES:
- ZERO FORMS & ZERO LANGUAGE PICKERS: User speaks in any language (Hindi, Marathi, Gujarati, Bengali, Tamil, Telugu, Kannada, Malayalam, Punjabi, Odia, Urdu, Assamese, Nepali, Sinhala, Indian English, Spanish, French, German, and 29+ languages). The AI engine auto-detects the language, transcribes in native script, and translates to English.
- MULTIMODAL GEMINI AUDIO ENGINE: Direct 16 kHz mono audio recording sent inline to Google Gemini Flash API for instant language detection, transcription, and entity extraction. Audio is processed ephemerally and never stored.
- CONFIRMATION CARD WITH OVERRIDE: Displays detected language chip (tap to override), native script transcript, English translation underneath, and extracted fields with instant voice/typing edit.
- INTELLIGENT INDIAN CURRENCY PARSER: Seamlessly understands "1 cr", "50 lakh", "one and a half crore", "dedh crore", "1 koti", "1 kodi", and investment ranges ("1 to 5 crore") across languages.
- CROSS-LANGUAGE DISCOVERY: An investor searching in Hindi or Spanish discovers apps uploaded by students speaking Marathi or Tamil.
- ONE-SENTENCE VOICE EDITS: Update terms on the fly by speaking naturally (e.g., "Change my raise to 2 cr").
- DEAL PIPELINE DASHBOARD: Track outreach stages from Contacted > Replied > Meeting > Term Sheet > Funded.
- ENTERPRISE PRIVACY ARCHITECTURE: Built with bank-grade privacy. Aadhaar numbers are never stored in raw form; only salted SHA-256 hashes are preserved with masked screen display (XXXX-XXXX-1234). Audio is never stored on servers.
- SCOPED PERMISSIONS: Safe Storage Access Framework (SAF) allows founders to pick project attachments without granting all-files device access.

2026 copyright Child Chiller Ventures. All rights reserved.
```

---

## 2. Play Store Target Audience & Content Rating
- **Target Age**: 18 and older (Adults only).
- **Category**: Business / Productivity.
- **Content Rating Questionnaire**:
  - Violence: No
  - Sexual content: No
  - Profanity: No
  - Controlled substances: No
  - User interaction: Users can send introductory messages to student founders.
  - Location sharing: No

---

## 3. Financial Features Declaration
In Google Play Console > App Content > Financial Features:
- **Declaration**: **Directory / Information discovery only**.
- **Explanation**:
  > "CampusPitch does not execute financial transactions, loan processing, crowdfunding, or securities brokerages within the app. It provides an informational directory and introduction network connecting university student projects with licensed investment funds."

---

## 4. Google Play Data Safety Answers

| Data Type | Collected | Shared | Ephemeral | Purpose |
| :--- | :---: | :---: | :---: | :--- |
| **Name & College ID** | Yes | Yes (with investor consent) | No | Account management, founder verification |
| **Email Address** | Yes | Yes | No | 2FA authentication, investor outreach |
| **Phone Number** | Yes | With consent | No | Account security, 2FA OTP |
| **Government ID (Aadhaar)** | Salted SHA-256 only | NEVER | Masked | Anti-impersonation role gating (No raw ID kept) |
| **Voice Recordings** | Ephemeral | Sent to Google Gemini API | Yes | Real-time multilingual speech-to-text, auto-detection, translation; audio is processed in-flight and NOT stored |
| **Files & Documents** | User-selected | Yes | No | Project pitch decks, READMEs via SAF |

- **Security Practices**:
  - Data is encrypted in transit using industry-standard TLS 1.3 / HTTPS.
  - Sensitive government IDs are hashed using one-way cryptographic salted SHA-256.
  - Audio is strictly ephemeral, never stored on servers, and temp files deleted immediately.
  - Users can request complete account and data deletion.

---

## 5. Google Play 14-Day Closed Testing Track Roadmap
For Google Play developer accounts created after November 2023, Google requires a minimum of **20 testers opted in for at least 14 consecutive days** before production access.

### Step-by-Step Instructions:
1. **Upload Release Bundle**:
   - Go to Google Play Console > **Testing** > **Closed testing**.
   - Create a new track named `Alpha Reviewers`.
   - Upload the pre-signed production bundle:
     `C:\Users\ADMIN\Desktop\CampusPitch-Release-v1.0.2.aab`
2. **Setup Feedback Email**:
   - Provide `support@childchiller.vc` or your college email.
3. **Add Testers**:
   - Create an email list with 20 colleagues/reviewers.
   - Share the join-link generated by Google Play Console.
4. **Internal Fast-Track Alternative (For Immediate Evaluation)**:
   - Go to **Testing** > **Internal testing**.
   - Internal testing track requires **NO 14-day waiting period** and **NO 20-tester minimum**.
   - Upload `CampusPitch-Release-v1.0.2.aab`.
   - Testers install the app instantly via the private Play Store link within 5 minutes of upload!

---

## 6. Release Artifact Information
- **AAB File**: `CampusPitch-Release-v1.0.2.aab`
- **Keystore**: `app/release-key.jks`
- **Key Alias**: `campuspitch`
- **Compile / Target SDK**: 35 (Android 15)
- **Min SDK**: 26 (Android 8.0)
- **Version Code**: 3
- **Version Name**: 1.0.2
- **2026 copyright Child Chiller Ventures**
