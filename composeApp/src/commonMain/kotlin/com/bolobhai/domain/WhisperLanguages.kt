package com.bolobhai.domain

/**
 * Spoken-language options offered on the Models screen (BoloBhai.md §13.5). The list shown is
 * driven by the *selected* speech-to-text model: a multilingual Whisper model exposes the full
 * language set plus "Auto-detect"; an English-only (`.en`) model exposes only English.
 *
 * The stored value is a Whisper language code ("hi", "en", …) or "auto", passed straight to
 * whisper-server, which uses it as a strict decode language (or auto-detects when "auto").
 */
data class SpokenLanguage(val code: String, val label: String)

const val AUTO_DETECT = "auto"

/** Whisper's full multilingual set (code → English name), as supported by whisper.cpp. */
val whisperLanguages: List<SpokenLanguage> = listOf(
    SpokenLanguage("en", "English"),
    SpokenLanguage("hi", "Hindi / Hinglish"),
    SpokenLanguage("zh", "Chinese"),
    SpokenLanguage("de", "German"),
    SpokenLanguage("es", "Spanish"),
    SpokenLanguage("ru", "Russian"),
    SpokenLanguage("ko", "Korean"),
    SpokenLanguage("fr", "French"),
    SpokenLanguage("ja", "Japanese"),
    SpokenLanguage("pt", "Portuguese"),
    SpokenLanguage("tr", "Turkish"),
    SpokenLanguage("pl", "Polish"),
    SpokenLanguage("ca", "Catalan"),
    SpokenLanguage("nl", "Dutch"),
    SpokenLanguage("ar", "Arabic"),
    SpokenLanguage("sv", "Swedish"),
    SpokenLanguage("it", "Italian"),
    SpokenLanguage("id", "Indonesian"),
    SpokenLanguage("uk", "Ukrainian"),
    SpokenLanguage("el", "Greek"),
    SpokenLanguage("cs", "Czech"),
    SpokenLanguage("ro", "Romanian"),
    SpokenLanguage("da", "Danish"),
    SpokenLanguage("hu", "Hungarian"),
    SpokenLanguage("ta", "Tamil"),
    SpokenLanguage("no", "Norwegian"),
    SpokenLanguage("th", "Thai"),
    SpokenLanguage("ur", "Urdu"),
    SpokenLanguage("hr", "Croatian"),
    SpokenLanguage("bg", "Bulgarian"),
    SpokenLanguage("lt", "Lithuanian"),
    SpokenLanguage("la", "Latin"),
    SpokenLanguage("mi", "Maori"),
    SpokenLanguage("ml", "Malayalam"),
    SpokenLanguage("cy", "Welsh"),
    SpokenLanguage("sk", "Slovak"),
    SpokenLanguage("te", "Telugu"),
    SpokenLanguage("fa", "Persian"),
    SpokenLanguage("lv", "Latvian"),
    SpokenLanguage("bn", "Bengali"),
    SpokenLanguage("sr", "Serbian"),
    SpokenLanguage("az", "Azerbaijani"),
    SpokenLanguage("sl", "Slovenian"),
    SpokenLanguage("kn", "Kannada"),
    SpokenLanguage("et", "Estonian"),
    SpokenLanguage("mk", "Macedonian"),
    SpokenLanguage("br", "Breton"),
    SpokenLanguage("eu", "Basque"),
    SpokenLanguage("is", "Icelandic"),
    SpokenLanguage("hy", "Armenian"),
    SpokenLanguage("ne", "Nepali"),
    SpokenLanguage("mn", "Mongolian"),
    SpokenLanguage("bs", "Bosnian"),
    SpokenLanguage("kk", "Kazakh"),
    SpokenLanguage("sq", "Albanian"),
    SpokenLanguage("sw", "Swahili"),
    SpokenLanguage("gl", "Galician"),
    SpokenLanguage("mr", "Marathi"),
    SpokenLanguage("pa", "Punjabi"),
    SpokenLanguage("si", "Sinhala"),
    SpokenLanguage("km", "Khmer"),
    SpokenLanguage("sn", "Shona"),
    SpokenLanguage("yo", "Yoruba"),
    SpokenLanguage("so", "Somali"),
    SpokenLanguage("af", "Afrikaans"),
    SpokenLanguage("oc", "Occitan"),
    SpokenLanguage("ka", "Georgian"),
    SpokenLanguage("be", "Belarusian"),
    SpokenLanguage("tg", "Tajik"),
    SpokenLanguage("sd", "Sindhi"),
    SpokenLanguage("gu", "Gujarati"),
    SpokenLanguage("am", "Amharic"),
    SpokenLanguage("yi", "Yiddish"),
    SpokenLanguage("lo", "Lao"),
    SpokenLanguage("uz", "Uzbek"),
    SpokenLanguage("fo", "Faroese"),
    SpokenLanguage("ht", "Haitian Creole"),
    SpokenLanguage("ps", "Pashto"),
    SpokenLanguage("tk", "Turkmen"),
    SpokenLanguage("nn", "Nynorsk"),
    SpokenLanguage("mt", "Maltese"),
    SpokenLanguage("sa", "Sanskrit"),
    SpokenLanguage("lb", "Luxembourgish"),
    SpokenLanguage("my", "Myanmar"),
    SpokenLanguage("bo", "Tibetan"),
    SpokenLanguage("tl", "Tagalog"),
    SpokenLanguage("mg", "Malagasy"),
    SpokenLanguage("as", "Assamese"),
    SpokenLanguage("tt", "Tatar"),
    SpokenLanguage("haw", "Hawaiian"),
    SpokenLanguage("ln", "Lingala"),
    SpokenLanguage("ha", "Hausa"),
    SpokenLanguage("ba", "Bashkir"),
    SpokenLanguage("jw", "Javanese"),
    SpokenLanguage("su", "Sundanese"),
)

/**
 * Spoken-language options for an STT model. [modelLanguages] is the model's explicit code list from
 * the catalog (null = infer from the file name). An English-only Whisper build (`.en`) supports only
 * English; every other build is multilingual, so we offer the full set with "Auto-detect" on top.
 */
fun spokenLanguagesFor(modelFile: String?, modelLanguages: List<String>?): List<SpokenLanguage> {
    val byCode = whisperLanguages.associateBy { it.code }
    val codes = when {
        modelLanguages != null -> modelLanguages
        modelFile != null && isEnglishOnlyModel(modelFile) -> listOf("en")
        else -> null // multilingual: full set
    }
    if (codes == null) {
        return listOf(SpokenLanguage(AUTO_DETECT, "Auto-detect")) + whisperLanguages
    }
    val resolved = codes.mapNotNull { byCode[it] }
    return if (resolved.size > 1) listOf(SpokenLanguage(AUTO_DETECT, "Auto-detect")) + resolved else resolved
}

private fun isEnglishOnlyModel(file: String): Boolean {
    val name = file.lowercase().removeSuffix(".bin").removeSuffix(".gguf")
    return name.endsWith(".en") || name.endsWith("-en") || name.endsWith("_en")
}
