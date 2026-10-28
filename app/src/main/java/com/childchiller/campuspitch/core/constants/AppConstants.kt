package com.childchiller.campuspitch.core.constants

/**
 * Core constants & configuration for CampusPitch.
 * 2026 copyright Child Chiller Ventures
 */
object AppConstants {
    const val APP_NAME = "CampusPitch"
    const val APP_TAGLINE = "Speak Your Startup. Meet Verified Capital."

    // Application copyright notice
    const val MANDATORY_COPYRIGHT = "2026 copyright Child Chiller Ventures"

    // Flash-class model IDs
    // gemini-flash-lite-latest is active and verified for structured JSON extraction
    const val GEMINI_MODEL_ID = "gemini-flash-lite-latest"
    const val GEMINI_AUDIO_MODEL_ID = "gemini-flash-lite-latest"
    const val GEMINI_MODEL_FALLBACK = "gemini-3.1-flash-lite"

    // Default speech-to-text fallback locale
    const val STT_LOCALE_ID = "en-IN"

    // Multi-Language Support Catalog (All auto-detected with zero manual picker)
    data class LanguageDefinition(val bcp47: String, val name: String, val nativeName: String)

    val SUPPORTED_LANGUAGES = listOf(
        LanguageDefinition("hi-IN", "Hindi", "हिन्दी"),
        LanguageDefinition("mr-IN", "Marathi", "मराठी"),
        LanguageDefinition("gu-IN", "Gujarati", "ગુજરાતી"),
        LanguageDefinition("bn-IN", "Bengali", "বাংলা"),
        LanguageDefinition("ta-IN", "Tamil", "தமிழ்"),
        LanguageDefinition("te-IN", "Telugu", "తెలుగు"),
        LanguageDefinition("kn-IN", "Kannada", "ಕನ್ನಡ"),
        LanguageDefinition("ml-IN", "Malayalam", "മലയാളം"),
        LanguageDefinition("pa-IN", "Punjabi", "ਪੰਜਾਬੀ"),
        LanguageDefinition("or-IN", "Odia", "ଓଡ଼ିଆ"),
        LanguageDefinition("ur-IN", "Urdu", "اردو"),
        LanguageDefinition("as-IN", "Assamese", "অসমীয়া"),
        LanguageDefinition("ne-NP", "Nepali", "नेपाली"),
        LanguageDefinition("si-LK", "Sinhala", "සිංහල"),
        LanguageDefinition("en-IN", "Indian English", "English (India)"),
        LanguageDefinition("es-ES", "Spanish", "Español"),
        LanguageDefinition("fr-FR", "French", "Français"),
        LanguageDefinition("de-DE", "German", "Deutsch"),
        LanguageDefinition("ar-SA", "Arabic", "العربية"),
        LanguageDefinition("pt-BR", "Portuguese", "Português"),
        LanguageDefinition("ru-RU", "Russian", "Русский"),
        LanguageDefinition("ja-JP", "Japanese", "日本語"),
        LanguageDefinition("ko-KR", "Korean", "한국어"),
        LanguageDefinition("zh-CN", "Mandarin Chinese", "中文"),
        LanguageDefinition("id-ID", "Indonesian", "Bahasa Indonesia"),
        LanguageDefinition("th-TH", "Thai", "ไทย"),
        LanguageDefinition("vi-VN", "Vietnamese", "Tiếng Việt"),
        LanguageDefinition("tr-TR", "Turkish", "Türkçe"),
        LanguageDefinition("sw-KE", "Swahili", "Kiswahili")
    )

    // Seeded Demo Investor Credentials
    const val SEEDED_DEMO_INVESTOR_CODE = "CCV-DEMO-2026"
    const val SEEDED_DEMO_FUND_NAME = "Alpha Ventures Fund"
    const val SEEDED_DEMO_AIF_CATEGORY = "AIF Category I"
    const val SEEDED_DEMO_EMAIL = "demo.investor@childchiller.vc"
    const val SEEDED_DEMO_PASSWORD = "InvestorDemo2026!"
    const val SEEDED_DEMO_MOBILE = "9876543210"
    const val SEEDED_DEMO_WEBSITE = "https://alphaventures.in"

    // Admin account credentials
    const val SEEDED_ADMIN_EMAIL = "admin@childchiller.vc"
    const val SEEDED_ADMIN_PASSWORD = "AdminPassword2026!"

    // Application salt for salted SHA-256 Aadhaar hashing
    const val AADHAAR_APPLICATION_SALT = "CCV_CAMPUSPITCH_2026_SALT_V1"

    // Google Play minimum age declaration
    const val MINIMUM_AGE_YEARS = 18

    // Canonical sectors for structured taxonomy
    val CANONICAL_SECTORS = listOf(
        "HealthTech & Hospital Management",
        "FinTech & Payments",
        "EdTech & Campus Tools",
        "AgriTech & Supply Chain",
        "AI, SaaS & DevTools",
        "ClimateTech & EV",
        "Consumer & D2C",
        "DeepTech & Hardware"
    )
}
