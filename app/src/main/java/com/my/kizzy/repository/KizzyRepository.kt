package com.my.kizzy.repository

import java.util.concurrent.ConcurrentHashMap

/**
 * Lightweight image resolver/cache compatible with the Kizzy API used by Dhun.
 * External URLs are kept as external assets; Discord-hosted IDs are cached as-is.
 */
class KizzyRepository {
    private val cache = ConcurrentHashMap<String, String>()

    fun putToCache(url: String, resolved: String) {
        if (url.isNotBlank() && resolved.isNotBlank()) cache[url] = resolved
    }

    fun peekCache(url: String): String? = cache[url]

    suspend fun getImage(url: String): String? {
        if (url.isBlank()) return null
        return cache[url] ?: url.also { cache[url] = it }
    }
}
