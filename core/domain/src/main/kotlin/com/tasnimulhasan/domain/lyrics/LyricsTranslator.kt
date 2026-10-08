package com.tasnimulhasan.domain.lyrics

/** Detects what language lyrics are written in and translates them to English. The
 * implementation lives in core:data so the domain layer stays free of any ML/Android types. */
interface LyricsTranslator {

    /** BCP-47 language tag of the dominant language in [text] (e.g. "hi", "ja", "es"), or
     * null if it couldn't be determined. */
    suspend fun detectLanguage(text: String): String?

    /** True if [languageTag] has an on-device translation model available. Romanized text
     * (tags like "hi-Latn") and rarer languages return false. */
    fun canTranslate(languageTag: String): Boolean

    /**
     * Translates each entry of [lines] to English, one-to-one and in order (blank entries
     * stay blank, so the result can be indexed alongside the original lines). Downloads the
     * language pack on first use for that language; throws on failure (offline with no pack
     * yet, unsupported language, etc.) so the UI can say what went wrong.
     */
    suspend fun translateToEnglish(sourceLanguageTag: String, lines: List<String>): List<String>
}
