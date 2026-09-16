/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.my.kizzy.gateway.entities.presence

import kotlinx.serialization.Serializable

/**
 * Minimal Discord gateway presence activity payload used by the bundled
 * Kizzy-compatible RPC client.
 *
 * Shape matches the entities originally shipped with the kizzy library
 * (`com.my.kizzy.gateway.entities.presence.Activity`) so callers that build
 * these objects keep compiling against the in-source implementation.
 */
@Serializable
data class Activity(
    val name: String,
    val type: Int = 0,
    val state: String? = null,
    val details: String? = null,
    val timestamps: ActivityTimestamps? = null,
    val assets: ActivityAssets? = null,
)

@Serializable
data class ActivityTimestamps(
    val start: Long? = null,
    val end: Long? = null,
)

@Serializable
data class ActivityAssets(
    val largeImage: String? = null,
    val largeText: String? = null,
    val smallImage: String? = null,
    val smallText: String? = null,
)
