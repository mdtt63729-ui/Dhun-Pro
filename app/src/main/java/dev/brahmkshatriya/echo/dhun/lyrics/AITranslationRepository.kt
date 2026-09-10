/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package dev.brahmkshatriya.echo.dhun.lyrics

import android.content.Context
import android.util.Log
import dev.brahmkshatriya.echo.dhun.constants.AIProvider
import dev.brahmkshatriya.echo.dhun.constants.AIModels
import dev.brahmkshatriya.echo.dhun.constants.AIApiKeyKey
import dev.brahmkshatriya.echo.dhun.constants.AIProviderKey
import dev.brahmkshatriya.echo.dhun.constants.CustomModelIdKey
import dev.brahmkshatriya.echo.dhun.constants.CustomOpenAIBaseUrlKey
import dev.brahmkshatriya.echo.dhun.constants.CustomOpenAIHeadersKey
import dev.brahmkshatriya.echo.dhun.constants.DEFAULT_CUSTOM_OPENAI_BASE_URL
import dev.brahmkshatriya.echo.dhun.db.DatabaseDao
import dev.brahmkshatriya.echo.dhun.db.entities.AITranslatedLyricsEntity
import dev.brahmkshatriya.echo.dhun.utils.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Repository for AI-powered lyrics translation.
 *
 * Supports three providers:
 * 1. OpenAI — uses the official OpenAI Chat Completions API
 * 2. Gemini — uses Google's Generative Language API (generateContent)
 * 3. Custom OpenAI — any OpenAI-compatible endpoint (Ollama, LM Studio, etc.)
 *
 * The translation flow:
 * 1. Check local cache (Room DB) for previously translated lyrics
 * 2. If no cache, build a prompt and call the AI API
 * 3. Parse the response, preserve timestamps from the original lyrics
 * 4. Save the result to the local cache for future use
 *
 * Timestamp preservation is key: the AI only translates text, and the
 * original timestamps are programmatically copied to the translated lines.
 * This keeps the translated lyrics in sync with the music.
 */
class AITranslationRepository(
    private val context: Context,
    private val databaseDao: DatabaseDao,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Gets AI-translated lyrics for a song, using cache when available.
     *
     * @param videoId The YouTube video ID of the song
     * @param originalLyrics The LRC-format lyrics (with timestamps) to translate
     * @return Translated LRC-format lyrics with timestamps preserved, or null on failure
     */
    suspend fun getTranslatedLyrics(videoId: String, originalLyrics: String): String? {
        val data = context.dataStore.data.first()
        val provider = data[AIProviderKey] ?: AIProvider.OPENAI
        val apiKey = data[AIApiKeyKey] ?: ""
        val targetLanguage = data[dev.brahmkshatriya.echo.dhun.constants.AITranslationLanguageKey] ?: "en"
        val customModelId = data[CustomModelIdKey] ?: ""
        val customBaseUrl = data[CustomOpenAIBaseUrlKey] ?: DEFAULT_CUSTOM_OPENAI_BASE_URL
        val customHeaders = data[CustomOpenAIHeadersKey] ?: ""

        if (apiKey.isEmpty() && provider != AIProvider.CUSTOM_OPENAI) return null

        // Check local cache first
        val cached = databaseDao.getAITranslatedLyrics(videoId, targetLanguage).first()
        if (cached != null && !cached.error && cached.translatedLyrics.isNotEmpty()) {
            Log.d(TAG, "AI translation cache hit for $videoId:$targetLanguage")
            return cached.translatedLyrics
        }

        // Parse original lyrics into (timestamp, text) pairs
        val lines = parseLrcLines(originalLyrics)
        if (lines.isEmpty()) return null

        // Build the prompt
        val linesText = lines.joinToString("\n") { it.second }
        val prompt = buildPrompt(linesText, targetLanguage)

        // Call the AI API
        val result = withContext(Dispatchers.IO) {
            when (provider) {
                AIProvider.OPENAI -> callOpenAI(apiKey, prompt, AIModels.getDefault(provider), "https://api.openai.com/v1/", null)
                AIProvider.GEMINI -> callGemini(apiKey, prompt, AIModels.getDefault(provider))
                AIProvider.CUSTOM_OPENAI -> {
                    val model = if (customModelId.isNotEmpty()) customModelId else AIModels.OPENAI_DEFAULT
                    val headers = parseCustomHeaders(customHeaders)
                    callOpenAI(apiKey, prompt, model, customBaseUrl, headers)
                }
                else -> null
            }
        }

        if (result == null) {
            Log.w(TAG, "AI translation failed for $videoId")
            return null
        }

        // Parse translated lines and re-attach timestamps
        val translatedLines = result.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        val mergedLyrics = mergeTimestampsWithTranslations(lines, translatedLines)

        // Save to cache
        databaseDao.insertAITranslatedLyrics(
            AITranslatedLyricsEntity(
                id = AITranslatedLyricsEntity.compositeId(videoId, targetLanguage),
                videoId = videoId,
                language = targetLanguage,
                translatedLyrics = mergedLyrics,
                error = false,
            ),
        )

        Log.d(TAG, "AI translation success for $videoId:$targetLanguage")
        return mergedLyrics
    }

    /**
     * Builds the translation prompt for the LLM.
     */
    private fun buildPrompt(lyricsText: String, targetLanguage: String): String {
        val langName = LANGUAGE_NAMES[targetLanguage] ?: targetLanguage
        return """
            You are a lyrics translator. Translate the following song lyrics into $langName.
            Keep the same number of lines. Do not add or remove lines.
            Preserve the meaning and poetic feel. Do not translate proper nouns (artist names, place names).
            Return ONLY the translated lines, one per line, no numbering.

            Lyrics:
            $lyricsText
        """.trimIndent()
    }

    /**
     * Calls the OpenAI Chat Completions API (or any OpenAI-compatible endpoint).
     */
    private fun callOpenAI(
        apiKey: String,
        prompt: String,
        model: String,
        baseUrl: String,
        extraHeaders: Map<String, String>?,
    ): String? {
        val url = "${baseUrl.trimEnd('/')}/chat/completions"

        val requestBody = buildJsonObject {
            put("model", model)
            put("messages", kotlinx.serialization.json.buildJsonArray {
                add(buildJsonObject {
                    put("role", "system")
                    put("content", "You are a professional lyrics translator.")
                })
                add(buildJsonObject {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("temperature", 0.3)
        }

        val requestBuilder = Request.Builder()
            .url(url)
            .post(requestBody.toString().toRequestBody(JSON_MEDIA_TYPE))
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")

        extraHeaders?.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }

        return try {
            val response = client.newCall(requestBuilder.build()).execute()
            val body = response.body?.string() ?: return null
            if (!response.isSuccessful) {
                Log.w(TAG, "OpenAI API error ${response.code}: $body")
                return null
            }
            val parsed = json.parseToJsonElement(body).jsonObject
            parsed["choices"]?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull
        } catch (e: Exception) {
            Log.e(TAG, "OpenAI API call failed", e)
            null
        }
    }

    /**
     * Calls the Gemini (Google Generative Language) API.
     */
    private fun callGemini(apiKey: String, prompt: String, model: String): String? {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val requestBody = buildJsonObject {
            put("contents", kotlinx.serialization.json.buildJsonArray {
                add(buildJsonObject {
                    put("parts", kotlinx.serialization.json.buildJsonArray {
                        add(buildJsonObject { put("text", prompt) })
                    })
                })
            })
            put("generationConfig", buildJsonObject {
                put("temperature", 0.3)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(requestBody.toString().toRequestBody(JSON_MEDIA_TYPE))
            .addHeader("Content-Type", "application/json")
            .build()

        return try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return null
            if (!response.isSuccessful) {
                Log.w(TAG, "Gemini API error ${response.code}: $body")
                return null
            }
            val parsed = json.parseToJsonElement(body).jsonObject
            parsed["candidates"]?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("content")?.jsonObject?.get("parts")?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API call failed", e)
            null
        }
    }

    /**
     * Parses LRC-format lyrics into a list of (timestamp_string, text) pairs.
     * Preserves the original timestamp format for re-attachment.
     */
    private fun parseLrcLines(lrc: String): List<Pair<String, String>> {
        val lines = mutableListOf<Pair<String, String>>()
        for (line in lrc.split("\n")) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            val match = TIMESTAMP_REGEX.find(trimmed)
            if (match != null) {
                val timestamp = match.value
                val text = trimmed.substring(match.range.last + 1).trim()
                lines.add(timestamp to text)
            } else {
                // Lines without timestamps (metadata, plain text)
                lines.add("" to trimmed)
            }
        }
        return lines
    }

    /**
     * Merges original timestamps with translated text.
     * If the AI returned the wrong number of lines, we match by index
     * and skip extra/missing lines gracefully.
     */
    private fun mergeTimestampsWithTranslations(
        originalLines: List<Pair<String, String>>,
        translatedLines: List<String>,
    ): String {
        val result = StringBuilder()
        val maxLen = minOf(originalLines.size, translatedLines.size)
        for (i in 0 until maxLen) {
            val (timestamp, _) = originalLines[i]
            val translation = translatedLines[i]
            if (timestamp.isNotEmpty()) {
                result.append(timestamp).append(" ")
            }
            result.append(translation).append("\n")
        }
        // Handle case where original has more lines than translation
        for (i in maxLen until originalLines.size) {
            val (timestamp, originalText) = originalLines[i]
            if (timestamp.isNotEmpty()) {
                result.append(timestamp).append(" ")
            }
            result.append(originalText).append("\n")
        }
        return result.toString().trimEnd()
    }

    /**
     * Parses custom headers from JSON string.
     */
    private fun parseCustomHeaders(headersJson: String): Map<String, String> {
        if (headersJson.isBlank()) return emptyMap()
        return try {
            val parsed = json.parseToJsonElement(headersJson).jsonObject
            parsed.mapValues { it.value.jsonPrimitive.content }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse custom headers JSON", e)
            emptyMap()
        }
    }

    /**
     * Removes cached translation for a song/language (e.g. when retrying).
     */
    suspend fun clearCache(videoId: String, language: String) {
        databaseDao.removeAITranslatedLyrics(videoId, language)
    }

    companion object {
        private const val TAG = "AITranslationRepo"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
        private val TIMESTAMP_REGEX = Regex("""\[\d{1,2}:\d{2}(?:\.\d{1,3})?]""")

        /** Maps language codes to full names for better LLM prompts. */
        private val LANGUAGE_NAMES = mapOf(
            "bn" to "Bengali",
            "hi" to "Hindi",
            "es" to "Spanish",
            "fr" to "French",
            "de" to "German",
            "it" to "Italian",
            "pt" to "Portuguese",
            "ru" to "Russian",
            "ja" to "Japanese",
            "ko" to "Korean",
            "zh" to "Chinese",
            "ar" to "Arabic",
            "tr" to "Turkish",
            "id" to "Indonesian",
            "th" to "Thai",
            "vi" to "Vietnamese",
            "ta" to "Tamil",
            "te" to "Telugu",
            "ml" to "Malayalam",
            "kn" to "Kannada",
            "mr" to "Marathi",
            "gu" to "Gujarati",
            "pa" to "Punjabi",
            "or" to "Odia",
            "en" to "English",
        )
    }
}
