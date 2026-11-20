# Privacy Policy for CampusPitch
**Effective Date: October 2026**
**2026 copyright Child Chiller Ventures**

### 1. Introduction
Child Chiller Ventures operates CampusPitch ("the App"), an AI-native venture marketplace connecting university student founders with verified venture capital and alternative investment funds. We take student and user privacy with the utmost seriousness.

### 2. Information We Collect
- **Student Founders**: We collect student name, university/college name, college identity number, and contact email/mobile. If an Aadhaar number is provided for identity verification, the application computes an irreversible, salted cryptographic hash (SHA-256 with user salt) and extracts only the last 4 digits for masked verification display (`XXXX-XXXX-1234`). The full raw 12-digit Aadhaar number is NEVER stored in our databases, persisted in logs, or shared with third parties.
- **Investors**: We collect the legal fund name, SEBI AIF registration category, corporate email address, contact phone, and official website URL.
- **Audio & Voice Data**: Spoken voice audio (16 kHz mono WAV) is captured ephemerally when you tap the microphone button. Spoken audio is sent inline directly to Google's Gemini API (specifically `gemini-1.5-flash`) for real-time multilingual transcription, language auto-detection, translation to English, and intent parsing. Audio data is processed in-flight and is NOT stored on Google servers or Child Chiller databases. All temporary on-device audio cache files are immediately deleted upon processing. Only the resulting text transcripts (native script and English translation) are retained in the Data Lake for search indexing and founder outreach.
- **Project Files**: The App utilizes Android Storage Access Framework (SAF) to access only files explicitly selected by the user (such as pitch documents, APKs, or READMEs). We do not request or maintain broad "all-files" device storage access.

### 3. How We Use Data
- Converting natural voice utterances into structured venture profiles.
- Enabling verified investors to discover relevant startups according to investment theses across multiple languages.
- Logging operational agent executions and model confidence scores in our internal data lake for auditability and compliance.
- Facilitating direct, consent-based communication between student founders and investors.

### 4. Security & Data Protection
- All transmissions use TLS 1.3 / HTTPS encryption.
- Firebase Firestore security rules enforce role-based access control. Unverified users cannot query student listings.
- Sensitive government IDs are protected by salted cryptographic one-way hashing.
- Voice recordings are ephemeral, zero-retention, and never stored as raw audio files.

### 5. User Rights & Account Deletion
Users retain full rights to inspect, update, or permanently delete their personal profiles, submitted MVP records, and data lake entries. To request permanent account deletion, users may contact privacy@childchiller.vc or tap the in-app account deletion option.

### 6. Contact
Child Chiller Ventures  
Email: privacy@childchiller.vc  
Website: https://childchiller.vc  
**2026 copyright Child Chiller Ventures**
