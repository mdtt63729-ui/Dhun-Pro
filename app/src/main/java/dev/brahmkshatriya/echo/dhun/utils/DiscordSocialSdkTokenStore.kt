package dev.brahmkshatriya.echo.dhun.utils

import android.content.Context
import android.util.Base64
import java.security.GeneralSecurityException

/**
 * Stores the Discord Social SDK access/refresh tokens.
 *
 * The upstream implementation used `androidx.security:security-crypto`
 * (EncryptedSharedPreferences + MasterKey), which is not part of this project's
 * dependency set. This in-source replacement keeps the exact same public API
 * but falls back to a private `SharedPreferences` file whose values are kept
 * only lightly obfuscated (Base64). If the security-crypto artifact is added
 * back to the build, this class can be swapped to the Keystore-backed version
 * without touching any callers.
 */
object DiscordSocialSdkTokenStore {
    private const val FILE_NAME = "discord_social_sdk_tokens"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    private fun encode(value: String): String =
        try {
            Base64.encodeToString(value.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        } catch (_: GeneralSecurityException) {
            value
        }

    private fun decode(value: String?): String? =
        value?.let {
            runCatching {
                Base64.decode(it, Base64.NO_WRAP).toString(Charsets.UTF_8)
            }.getOrNull() ?: it
        }

    fun save(context: Context, accessToken: String, refreshToken: String) {
        prefs(context).edit()
            .putString(KEY_ACCESS_TOKEN, encode(accessToken))
            .putString(KEY_REFRESH_TOKEN, encode(refreshToken))
            .apply()
    }

    fun accessToken(context: Context): String? = decode(prefs(context).getString(KEY_ACCESS_TOKEN, null))

    fun refreshToken(context: Context): String? = decode(prefs(context).getString(KEY_REFRESH_TOKEN, null))

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
