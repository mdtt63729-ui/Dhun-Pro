/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package dev.brahmkshatriya.echo.dhun.constants

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

// ── AI Translation preference keys ─────────────────────────────────────────

/** Which AI backend to use: "openai", "gemini", or "custom_openai". */
val AIProviderKey = stringPreferencesKey("aiProvider")

/** The user's API key for the chosen AI provider. */
val AIApiKeyKey = stringPreferencesKey("aiApiKey")

/** Optional custom model ID (e.g. "gpt-4o", "gemini-1.5-flash"). */
val CustomModelIdKey = stringPreferencesKey("customModelId")

/** Custom base URL for OpenAI-compatible endpoints (e.g. Ollama, LM Studio). */
val CustomOpenAIBaseUrlKey = stringPreferencesKey("customOpenAIBaseUrl")

/** Extra HTTP headers as JSON for custom OpenAI-compatible endpoints. */
val CustomOpenAIHeadersKey = stringPreferencesKey("customOpenAIHeaders")

/** Master toggle for AI lyrics translation. */
val UseAITranslationKey = booleanPreferencesKey("useAITranslation")

/** Target language code for translation (e.g. "bn", "hi", "es"). */
val AITranslationLanguageKey = stringPreferencesKey("aiTranslationLanguage")

// ── AI provider constants ───────────────────────────────────────────────────

object AIProvider {
    const val OPENAI = "openai"
    const val GEMINI = "gemini"
    const val CUSTOM_OPENAI = "custom_openai"
}

/** Default models per provider. */
object AIModels {
    const val OPENAI_DEFAULT = "gpt-4o-mini"
    const val GEMINI_DEFAULT = "gemini-1.5-flash"
    const val CUSTOM_OPENAI_DEFAULT = "" // user must specify

    fun getDefault(provider: String): String = when (provider) {
        AIProvider.OPENAI -> OPENAI_DEFAULT
        AIProvider.GEMINI -> GEMINI_DEFAULT
        AIProvider.CUSTOM_OPENAI -> CUSTOM_OPENAI_DEFAULT
        else -> OPENAI_DEFAULT
    }
}

/** Default base URL for custom OpenAI-compatible endpoints. */
const val DEFAULT_CUSTOM_OPENAI_BASE_URL = "https://api.openai.com/v1/"
