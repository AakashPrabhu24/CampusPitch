# 3-Minute Live Evaluation Script: CampusPitch
**2026 copyright Child Chiller Ventures**

This script is designed for live evaluation by venture firm reviewers. It covers all 3 mandatory acceptance tests (T1, T2, T3), zero-form voice intake, voice edits, domain verification, deal pipeline tracking, and Data Lake auditability in exactly 180 seconds.

---

### Minute 0:00 – 1:00: Role Gating & Student Voice Intake (Test T1)

**Action**: Launch the app from Google Play or open `CampusPitch-Firebase-Connected.apk`.
- **Reviewer Note**: Point out the mandatory copyright notice on the Splash & Footer: `"2026 copyright Child Chiller Ventures"`.
- **Opening Screen**:
  - Tap **Student Founder**.
  - Show the Identity Key field: Enter or speak any College ID (e.g. `IITB-2026-IT`) or a 12-digit Aadhaar number (e.g. `9876 5432 1098`).
  - *Highlight*: Explain that raw Aadhaar is **never** saved; only a salted SHA-256 hash is computed, and the screen displays `XXXX-XXXX-1098`.
  - Tap **Authenticate as Student**.
- **Student Intake (Acceptance Test T1)**:
  - In the Student Studio, tap the **big microphone** or the one-tap preset:
  > *"I want to upload an app called MediTrack. It does hospital management for small clinics. I am looking to raise 1 cr to scale the app."*
  - Watch the live animated waveform and real-time speech transcript.
  - The **"Did I get this right?"** card appears immediately with **zero form fields**:
    - **App Name**: MediTrack
    - **Sector**: HealthTech & Hospital Management
    - **Seeking**: ₹1,00,00,000 (1 cr)
    - **Confidence**: 98%
- **Dynamic Voice Edit**:
  - Say or tap: *"Change my raise to 2 cr"*.
  - Show the amount dynamically update to **₹2,00,00,000** without opening any edit field.
  - Tap **Confirm & Upload** ➔ Point out that it publishes directly to the live Firestore Data Lake (`mvps`, `raw_inputs`, and `agent_runs`).

---

### Minute 1:00 – 2:00: Investor Registration & Verification (Test T3)

**Action**: Return to the opening screen and tap **New Fund? Register Spoken Credentials**.
- **Investor Registration (Acceptance Test T3)**:
  - Tap the microphone or the one-tap preset:
  > *"My fund is BluePeak Ventures and we are an AIF Category I. My official email is partner@bluepeak.vc, mobile 9876543210, website bluepeak.vc."*
  - The **InvestorRegistrationAgent** extracts all credentials and verifies:
    - ✅ **Corporate Email**: `partner@bluepeak.vc`
    - ✅ **Indian Mobile (+91)**: `9876543210`
    - ✅ **Domain Matching**: Verifies `@bluepeak.vc` matches `bluepeak.vc` (rejects personal Gmail/Yahoo addresses).
- **Admin Approval Path & Code Issuance**:
  - Tap **Confirm & Submit for Review** ➔ Status moves to **Pending Approval**.
  - Tap **Fast-Track Admin Approval & Issue Code** ➔ A unique deterministic code is issued (e.g. `CCV-BPV-4821`).
  - Tap **Copy Code & Log In** ➔ Enters the Investor Deal Flow immediately.

---

### Minute 2:00 – 3:00: Spoken Search, AI Summary & Pipeline (Test T2)

**Action**: In the Investor Deal Flow screen:
- **Spoken Deal Search (Acceptance Test T2)**:
  - Tap the microphone or the one-tap preset:
  > *"I want a list of all apps in Hospital Management systems where the investment amount is between 1 cr and 5 cr."*
  - The **InvestorSearchAgent** automatically parses:
    - **Sector**: `HealthTech & Hospital Management` (with synonyms for hospital, clinic, ehr, doctor).
    - **Bounds**: `₹1,00,00,000 - ₹5,00,00,000`.
  - **Results**: Ranks **MediTrack** at the top with a one-line AI summary:
    > *"MediTrack: OPD queue & clinic management for tier-2 hospitals raising ₹1 Cr."*
- **Contact Founder Action**:
  - Tap **Contact Student Founder**.
  - Show the auto-drafted intro message:
    > *"Hi MediTrack team! We at BluePeak Ventures reviewed your MVP on CampusPitch and are interested in your solution for HealthTech & Hospital Management. We would like to discuss your raise of ₹1,00,00,000. Are you open for a quick introductory video call this week?"*
  - Tap **Send Intro**.
- **Deal Pipeline Tracking**:
  - Switch to the **Pipeline** tab.
  - Point out MediTrack in the active deal flow.
  - Advance the status: `Contacted` ➔ `Replied` ➔ `Meeting` ➔ `Term sheet` ➔ `Funded`.

**Conclusion**: Show the Central Data Lake in Firebase Console / local state: all raw inputs, curated records, and agent audit runs with latency and confidence are captured.
