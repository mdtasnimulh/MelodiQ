package com.tasnimulhasan.data.lyrics

import com.google.android.gms.tasks.Task
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.common.model.DownloadConditions
import com.tasnimulhasan.domain.lyrics.LyricsTranslator
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * On-device language detection + translation via ML Kit. Nothing about the lyrics is sent to
 * a server: the only network use is the one-time download of a language pack (tens of MB)
 * the first time a given language is translated.
 */
@Singleton
class MlKitLyricsTranslator @Inject constructor() : LyricsTranslator {

    private val identifier by lazy { LanguageIdentification.getClient() }

    override suspend fun detectLanguage(text: String): String? {
        if (text.isBlank()) return null
        val tag = identifier.identifyLanguage(text.take(2_000)).await()
        return tag.takeUnless { it == "und" }
    }

    // Romanized text ("hi-Latn" etc.) is Latin letters spelling another language - the models
    // expect native script, so translating it would produce nonsense. Treated as unsupported.
    override fun canTranslate(languageTag: String): Boolean =
        !languageTag.contains("-Latn", ignoreCase = true) &&
            TranslateLanguage.fromLanguageTag(languageTag) != null

    override suspend fun translateToEnglish(sourceLanguageTag: String, lines: List<String>): List<String> {
        val source = TranslateLanguage.fromLanguageTag(sourceLanguageTag)
            ?: throw IllegalArgumentException("No translation model for \"$sourceLanguageTag\"")
        val translator = Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(source)
                .setTargetLanguage(TranslateLanguage.ENGLISH)
                .build()
        )
        try {
            translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
            // Line by line: ML Kit translates a string at a time, and keeping the mapping
            // one-to-one is what lets the UI show each translation right under its original.
            return lines.map { line -> if (line.isBlank()) line else translator.translate(line).await() }
        } finally {
            translator.close()
        }
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}
