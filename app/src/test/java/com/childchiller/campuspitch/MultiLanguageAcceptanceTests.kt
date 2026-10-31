package com.childchiller.campuspitch

import com.childchiller.campuspitch.core.ai.InvestorRegistrationAgent
import com.childchiller.campuspitch.core.ai.InvestorSearchAgent
import com.childchiller.campuspitch.core.ai.MultiLanguageVoiceAgent
import com.childchiller.campuspitch.core.constants.AppConstants
import com.childchiller.campuspitch.core.models.MvpPitchRecord
import com.childchiller.campuspitch.core.utils.InrParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 2026 copyright Child Chiller Ventures
 * Comprehensive Acceptance Test Suite for Multi-Language Voice with Auto-Detection.
 */
class MultiLanguageAcceptanceTests {

    private val voiceAgent = MultiLanguageVoiceAgent(generativeModel = null)
    private val searchAgent = InvestorSearchAgent(generativeModel = null)
    private val registerAgent = InvestorRegistrationAgent(generativeModel = null)

    /**
     * ACCEPTANCE TEST 1:
     * "Upload MediTrack, hospital management for small clinics, raising 1 cr" in
     * Hindi, Marathi, and Tamil:
     * right language chip, right fields, raise = 10,000,000 INR.
     */
    @Test
    fun testAcceptanceTest1_UploadMediTrackInHindiMarathiTamil() = runBlocking {
        println("=== ACCEPTANCE TEST 1: Multi-Language Student Upload ===")

        // 1. Hindi (hi-IN)
        val hindiUtterance = "मैं मेडीट्रैक नाम का एक ऐप अपलोड करना चाहता हूं। यह छोटे क्लीनिकों के लिए अस्पताल प्रबंधन करता है। मैं 1 करोड़ जुटाना चाहता हूं।"
        val hindiResult = voiceAgent.processText(hindiUtterance, forcedIntentHint = "student_upload")
        println("Hindi Detection: [PASS] bcp47=${hindiResult.detectedLanguage.bcp47}, name=${hindiResult.detectedLanguage.name}")
        assertEquals("hi-IN", hindiResult.detectedLanguage.bcp47)
        assertEquals("MediTrack", hindiResult.fields["appName"])
        assertEquals("HealthTech & Hospital Management", hindiResult.fields["sector"])
        assertEquals(10_000_000L, hindiResult.fields["raiseAmountInr"])

        // 2. Marathi (mr-IN)
        val marathiUtterance = "मला मेडीट्रॅक नावाचे ॲप अपलोड करायचे आहे. हे लहान क्लिनिकसाठी रुग्णालय व्यवस्थापन करते. मला 1 कोटी उभे करायचे आहेत."
        val marathiResult = voiceAgent.processText(marathiUtterance, forcedIntentHint = "student_upload")
        println("Marathi Detection: [PASS] bcp47=${marathiResult.detectedLanguage.bcp47}, name=${marathiResult.detectedLanguage.name}")
        assertEquals("mr-IN", marathiResult.detectedLanguage.bcp47)
        assertEquals("MediTrack", marathiResult.fields["appName"])
        assertEquals("HealthTech & Hospital Management", marathiResult.fields["sector"])
        assertEquals(10_000_000L, marathiResult.fields["raiseAmountInr"])

        // 3. Tamil (ta-IN)
        val tamilUtterance = "நான் மெடிட்ராக் என்ற செயலியை பதிவேற்ற விரும்புகிறேன். இது சிறிய கிளினிக்குகளுக்கான மருத்துவமனை நிர்வாகம் செய்கிறது. 1 கோடி திரட்ட விரும்புகிறேன்."
        val tamilResult = voiceAgent.processText(tamilUtterance, forcedIntentHint = "student_upload")
        println("Tamil Detection: [PASS] bcp47=${tamilResult.detectedLanguage.bcp47}, name=${tamilResult.detectedLanguage.name}")
        assertEquals("ta-IN", tamilResult.detectedLanguage.bcp47)
        assertEquals("MediTrack", tamilResult.fields["appName"])
        assertEquals("HealthTech & Hospital Management", tamilResult.fields["sector"])
        assertEquals(10_000_000L, tamilResult.fields["raiseAmountInr"])
    }

    /**
     * ACCEPTANCE TEST 2:
     * Cross-Language Discovery:
     * An investor says "hospital management apps raising 1 to 5 cr" in Hindi
     * and finds the app a student uploaded in Marathi.
     */
    @Test
    fun testAcceptanceTest2_CrossLanguageInvestorDiscovery() = runBlocking {
        println("=== ACCEPTANCE TEST 2: Cross-Language Discovery ===")

        // Student uploaded MediTrack MVP with description in Marathi
        val marathiMvp = MvpPitchRecord(
            id = "mvp_marathi_meditrack_001",
            ownerUid = "student_pune_coep",
            ownerName = "Aditi Deshmukh",
            ownerCollege = "COEP Technological University",
            appName = "MediTrack",
            description = "लहान क्लिनिकसाठी रुग्णालय व्यवस्थापन (Hospital management for small clinics)",
            sector = "HealthTech & Hospital Management",
            stage = "MVP",
            raiseAmountInr = 10_000_000L,
            raiseOriginalWording = "1 कोटी",
            status = "published"
        )

        val unRelatedMvp = MvpPitchRecord(
            id = "mvp_fintech_002",
            ownerUid = "student_delhi",
            ownerName = "Rahul Sharma",
            ownerCollege = "IIT Delhi",
            appName = "PayFlow",
            description = "UPI micropayments for kirana stores",
            sector = "FinTech & Payments",
            stage = "MVP",
            raiseAmountInr = 50_000_000L,
            raiseOriginalWording = "5 cr",
            status = "published"
        )

        val pool = listOf(marathiMvp, unRelatedMvp)

        // 1. Investor speaks query in Hindi
        val investorHindiUtterance = "अस्पताल प्रबंधन 1 से 5 करोड़"
        val hindiParsed = voiceAgent.processText(investorHindiUtterance, forcedIntentHint = "investor_search")
        assertEquals("hi-IN", hindiParsed.detectedLanguage.bcp47)

        val searchInHindi = searchAgent.executeSearch(
            utterance = investorHindiUtterance,
            candidateMvps = pool,
            investorFundName = "Child Chiller Capital"
        )

        assertTrue(searchInHindi.rankedResults.isNotEmpty())
        assertEquals("MediTrack", searchInHindi.rankedResults.first().mvp.appName)
        assertEquals(10_000_000L, searchInHindi.rankedResults.first().mvp.raiseAmountInr)
        println("Cross-Language Search (Hindi Query -> Marathi MVP): [PASS] Ranked #1: ${searchInHindi.rankedResults.first().mvp.appName}")

        // 2. Investor speaks English with range
        val investorEnglishUtterance = "hospital management apps raising 1 to 5 cr"
        val searchInEnglish = searchAgent.executeSearch(
            utterance = investorEnglishUtterance,
            candidateMvps = pool,
            investorFundName = "Child Chiller Capital"
        )
        assertTrue(searchInEnglish.rankedResults.isNotEmpty())
        assertEquals("MediTrack", searchInEnglish.rankedResults.first().mvp.appName)
        println("Cross-Language Search (English Query -> Marathi MVP): [PASS] Ranked #1: ${searchInEnglish.rankedResults.first().mvp.appName}")
    }

    /**
     * ACCEPTANCE TEST 3:
     * Investor registration (fund name, AIF Category I, email, mobile, website)
     * in Spanish: all fields extracted.
     */
    @Test
    fun testAcceptanceTest3_InvestorRegistrationInSpanish() = runBlocking {
        println("=== ACCEPTANCE TEST 3: Spanish Investor Registration ===")

        val spanishRegistration = "Mi fondo es BluePeak Ventures y somos una categoría AIF I. Mi correo oficial es partner@bluepeak.vc, móvil 9876543210, sitio web bluepeak.vc."

        val voiceResult = voiceAgent.processText(spanishRegistration, forcedIntentHint = "investor_register")
        assertEquals("es-ES", voiceResult.detectedLanguage.bcp47)
        println("Spanish Detection: [PASS] bcp47=${voiceResult.detectedLanguage.bcp47}")

        val regResult = registerAgent.processRegistrationSentence(spanishRegistration, "test_investor_spanish")
        assertNotNull(regResult.profile)
        val profile = regResult.profile!!

        assertEquals("BluePeak Ventures", profile.fundName)
        assertEquals("AIF Category I", profile.aifCategory)
        assertEquals("partner@bluepeak.vc", profile.officialEmail)
        assertEquals("9876543210", profile.mobile)
        assertEquals("bluepeak.vc", profile.website)
        println("Spanish Registration Fields: [PASS] fundName=${profile.fundName}, category=${profile.aifCategory}, email=${profile.officialEmail}, mobile=${profile.mobile}, website=${profile.website}")
    }

    /**
     * ACCEPTANCE TEST 4:
     * A mixed Hinglish sentence works.
     */
    @Test
    fun testAcceptanceTest4_CodeMixedHinglishPitch() = runBlocking {
        println("=== ACCEPTANCE TEST 4: Code-Mixed Hinglish Pitch ===")

        val hinglishSentence = "Arre yaar, I want to pitch MediTrack. Hospital management for small clinics, 1 cr raise karna hai"
        val result = voiceAgent.processText(hinglishSentence, forcedIntentHint = "student_upload")

        assertTrue(result.detectedLanguage.bcp47 == "hi-IN" || result.detectedLanguage.bcp47 == "en-IN")
        assertEquals("MediTrack", result.fields["appName"])
        assertEquals("HealthTech & Hospital Management", result.fields["sector"])
        assertEquals(10_000_000L, result.fields["raiseAmountInr"])
        println("Hinglish Pitch Parsing: [PASS] Detected=${result.detectedLanguage.name}, AppName=${result.fields["appName"]}, Raise=₹${result.fields["raiseAmountInr"]}")
    }

    /**
     * ACCEPTANCE TEST 5:
     * Airplane mode -> clean fallback to typed input.
     */
    @Test
    fun testAcceptanceTest5_AirplaneModeCleanFallback() = runBlocking {
        println("=== ACCEPTANCE TEST 5: Airplane Mode Offline Fallback ===")

        // When offline (generativeModel is null or network unavailable)
        val offlineAgent = MultiLanguageVoiceAgent(generativeModel = null)
        val typedSentence = "MediTrack, hospital management for small clinics, raising 1 cr"

        val fallbackResult = offlineAgent.processText(typedSentence, forcedIntentHint = "student_upload")

        // Verifies no exception thrown, deterministic local fallback works seamlessly
        assertTrue(fallbackResult.isFallback)
        assertEquals("MediTrack", fallbackResult.fields["appName"])
        assertEquals("HealthTech & Hospital Management", fallbackResult.fields["sector"])
        assertEquals(10_000_000L, fallbackResult.fields["raiseAmountInr"])
        assertTrue(fallbackResult.executionLatencyMs < 100) // Fast local fallback
        println("Airplane Mode Fallback: [PASS] Handled gracefully without crash, latency=${fallbackResult.executionLatencyMs}ms")
    }

    /**
     * ALL 29 REQUIRED LANGUAGES AUTO-DETECTION MATRIX:
     * Verifies auto-detection across all 29 target languages and outputs a pass/fail report.
     */
    @Test
    fun testAll29LanguagesAutoDetectionMatrix() {
        println("=== ALL 29 LANGUAGES AUTO-DETECTION REPORT ===")

        data class LangTestSample(
            val expectedCode: String,
            val expectedName: String,
            val sampleSentence: String
        )

        val samples = listOf(
            LangTestSample("hi-IN", "Hindi", "नमस्ते, मैं अपना नया ऐप अपलोड करना चाहता हूँ।"),
            LangTestSample("mr-IN", "Marathi", "नमस्कार, मला माझे ॲप अपलोड करायचे आहे आणि 1 कोटी उभे करायचे आहेत."),
            LangTestSample("gu-IN", "Gujarati", "નમસ્તે, હું મારી હોસ્પિટલ એપ્લિકેશન અપલોડ કરવા માંગુ છું."),
            LangTestSample("bn-IN", "Bengali", "নমস্কার, আমি আমার স্বাস্থ্য ব্যবস্থাপনা অ্যাপ আপলোড করতে চাই।"),
            LangTestSample("ta-IN", "Tamil", "வணக்கம், எனது மருத்துவமனை செயலியை பதிவேற்ற விரும்புகிறேன்."),
            LangTestSample("te-IN", "Telugu", "నమస్కారం, నేను నా ఆసుపత్రి నిర్వహణ యాప్‌ను అప్‌లోడ్ చేయాలనుకుంటున్నాను."),
            LangTestSample("kn-IN", "Kannada", "ನಮಸ್ಕಾರ, ನಾನು ನನ್ನ ಆಸ್ಪತ್ರೆ ನಿರ್ವಹಣಾ ಅಪ್ಲಿಕೇಶನ್ ಅನ್ನು ಅಪ್‌ಲೋಡ್ ಮಾಡಲು ಬಯಸುತ್ತೇನೆ."),
            LangTestSample("ml-IN", "Malayalam", "നമസ്കാരം, എന്റെ ആശുപത്രി മാനേജ്മെന്റ് ആപ്പ് അപ്‌ലോഡ് ചെയ്യാൻ ഞാൻ ആഗ്രഹിക്കുന്നു."),
            LangTestSample("pa-IN", "Punjabi", "ਸਤਿ ਸ੍ਰੀ ਅਕਾਲ, ਮੈਂ ਆਪਣਾ ਨਵਾਂ ਹਸਪਤਾਲ ਪ੍ਰਬੰਧਨ ਐਪ ਅਪਲੋਡ ਕਰਨਾ ਚਾਹੁੰਦਾ ਹਾਂ।"),
            LangTestSample("or-IN", "Odia", "ନମସ୍କାର, ମୁଁ ମୋର ଡାକ୍ତରଖାନା ପରିଚାଳନା ଆପ୍ ଅପଲୋଡ୍ କରିବାକୁ ଚାହୁଁଛି।"),
            LangTestSample("ur-IN", "Urdu", "السلام علیکم، میں اپنا نیا ایپ اپ لوڈ کرنا چاہتا ہوں۔"),
            LangTestSample("as-IN", "Assamese", "নমস্কাৰ, মই মোৰ চিকিৎসালয় ব্যৱস্থাপনা এপ্প আপলোড কৰিব বিচাৰো।"),
            LangTestSample("ne-NP", "Nepali", "नमस्ते, म मेरो नयाँ एप अपलोड गर्न चाहन्छु र सहयोग चाहिन्छ।"),
            LangTestSample("si-LK", "Sinhala", "ආයුබෝවන්, මගේ රෝහල් කළමනාකරණ යෙදුම උඩුගත කිරීමට මට අවශ්‍යය."),
            LangTestSample("en-IN", "Indian English", "Upload MediTrack, hospital management for small clinics, raising 1 cr"),
            LangTestSample("es-ES", "Spanish", "Mi fondo es BluePeak Ventures y somos una categoría AIF I."),
            LangTestSample("fr-FR", "French", "Bonjour, je souhaite télécharger mon application pour hôpital."),
            LangTestSample("de-DE", "German", "Guten Tag, ich möchte meine App für Krankenhausverwaltung hochladen."),
            LangTestSample("ar-SA", "Arabic", "مرحباً، أود رفع تطبيقي لإدارة المستشفيات والعيادات."),
            LangTestSample("pt-BR", "Portuguese", "Olá, meu projeto é um sistema hospitalar para clínicas."),
            LangTestSample("ru-RU", "Russian", "Здравствуйте, я хочу загрузить свое приложение для клиник."),
            LangTestSample("ja-JP", "Japanese", "こんにちは、病院管理アプリをアップロードしたいです。"),
            LangTestSample("ko-KR", "Korean", "안녕하세요, 소형 병원을 위한 관리 앱을 등록하고 싶습니다."),
            LangTestSample("zh-CN", "Mandarin Chinese", "您好，我想上传一款适用于中小型诊所的医院管理系统。"),
            LangTestSample("id-ID", "Indonesian", "Halo, saya ingin mengunggah aplikasi manajemen rumah sakit untuk klinik."),
            LangTestSample("th-TH", "Thai", "สวัสดีครับ ผมต้องการอัปโหลดแอปจัดการโรงพยาบาลสำหรับคลินิกขนาดเล็ก"),
            LangTestSample("vi-VN", "Vietnamese", "Xin chào, tôi muốn tải lên ứng dụng quản lý phòng khám cho bệnh viện."),
            LangTestSample("tr-TR", "Turkish", "Merhaba, küçük klinikler için hastane yönetim uygulamamı yüklemek istiyorum."),
            LangTestSample("sw-KE", "Swahili", "Habari, mradi wangu ni mfumo wa usimamizi wa hospitali kwa zahanati ndogo.")
        )

        assertEquals(29, samples.size)

        var passCount = 0
        println("| # | Language | BCP-47 Code | Auto-Detection Result | Status |")
        println("|---|----------|-------------|-----------------------|--------|")

        samples.forEachIndexed { index, sample ->
            val detected = voiceAgent.detectLanguageLocally(sample.sampleSentence)
            val isMatch = detected.bcp47.equals(sample.expectedCode, ignoreCase = true)
            if (isMatch) passCount++
            val status = if (isMatch) "PASS" else "FAIL"
            println("| ${index + 1} | ${sample.expectedName} | ${sample.expectedCode} | ${detected.name} (${detected.bcp47}) | $status |")
            assertTrue("Language detection mismatch for ${sample.expectedName}", isMatch)
        }

        println("\nLanguage Auto-Detection Summary: $passCount / 29 languages passed (100% accuracy).")
        assertEquals(29, passCount)
    }
}
