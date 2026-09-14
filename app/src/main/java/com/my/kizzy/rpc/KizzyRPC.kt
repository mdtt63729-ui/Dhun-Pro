package com.my.kizzy.rpc

import com.my.kizzy.DefaultKizzyLogger
import com.my.kizzy.KizzyLogger
import com.my.kizzy.repository.KizzyRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Bundled Kizzy-compatible Discord Rich Presence client.
 *
 * It intentionally keeps the small API surface used by Dhun so the app does not
 * depend on an unavailable third-party Kizzy artifact at build time.
 */
open class KizzyRPC(
    private val token: String,
    private val injectedLogger: KizzyLogger? = null,
) {
    private val repository = KizzyRepository()
    private val logger = injectedLogger ?: DefaultKizzyLogger("KizzyRPC")
    private var platform: String? = null
    @Volatile private var running = false

    fun closeRPC() {
        running = false
    }

    fun isRpcRunning(): Boolean = running

    fun setPlatform(platform: String? = null) {
        this.platform = platform
    }

    suspend fun stopActivity() {
        sendPresence(null)
        running = false
    }

    suspend fun preloadImage(image: RpcImage?): String? = image?.resolveImage(repository)

    suspend fun refreshRPC(
        name: String,
        state: String?,
        stateUrl: String? = null,
        details: String?,
        detailsUrl: String? = null,
        largeImage: RpcImage?,
        smallImage: RpcImage?,
        largeText: String? = null,
        smallText: String? = null,
        buttons: List<Pair<String, String>>? = null,
        startTime: Long? = null,
        endTime: Long? = null,
        type: Type = Type.LISTENING,
        statusDisplayType: StatusDisplayType = StatusDisplayType.NAME,
        streamUrl: String? = null,
        applicationId: String? = null,
        status: String? = "online",
        since: Long? = null,
    ) {
        val large = largeImage?.resolveImage(repository)
        val small = smallImage?.resolveImage(repository)

        val activity = buildJsonObject {
            put("name", name.take(128))
            put("type", type.value)
            state?.let { put("state", it.take(128)) }
            stateUrl?.let { put("state_url", it) }
            details?.let { put("details", it.take(128)) }
            detailsUrl?.let { put("details_url", it) }
            streamUrl?.let { put("url", it) }
            platform?.takeIf { it.isNotBlank() }?.let { put("platform", it.take(128)) }
            put("status_display_type", statusDisplayType.value)
            if (startTime != null || endTime != null) {
                putJsonObject("timestamps") {
                    startTime?.let { put("start", it) }
                    endTime?.let { put("end", it) }
                }
            }
            if (large != null || small != null || largeText != null || smallText != null) {
                putJsonObject("assets") {
                    large?.let { put("large_image", it) }
                    small?.let { put("small_image", it) }
                    largeText?.let { put("large_text", it.take(128)) }
                    smallText?.let { put("small_text", it.take(128)) }
                }
            }
            buttons?.take(2)?.takeIf { it.isNotEmpty() }?.let { list ->
                putJsonArray("buttons") { list.forEach { add(it.first.take(32)) } }
                putJsonObject("metadata") {
                    putJsonArray("button_urls") { list.forEach { add(it.second) } }
                }
            }
            applicationId?.let { put("application_id", it) }
        }

        sendPresence(buildJsonObject {
            put("since", since ?: 0L)
            putJsonArray("activities") { add(activity) }
            put("status", status ?: "online")
            put("afk", false)
        })
    }

    private suspend fun sendPresence(activity: JsonObject?) {
        if (token.isBlank()) {
            logger.warning("Discord RPC token is empty; skipping presence")
            return
        }

        val client = HttpClient(OkHttp) {
            install(io.ktor.client.plugins.websocket.WebSockets)
        }
        try {
            client.webSocket("wss://gateway.discord.gg/?v=10&encoding=json") {
                var heartbeatMs = 30_000L
                var identified = false
                var heartbeatJob: kotlinx.coroutines.Job? = null

                while (!identified) {
                    val frame = incoming.receiveCatching().getOrNull() ?: break
                    if (frame !is Frame.Text) continue
                    val payload = runCatching { Json.parseToJsonElement(frame.readText()).jsonObject }.getOrNull() ?: continue
                    val op = payload["op"]?.jsonPrimitive?.intOrNull ?: continue
                    if (op == 10) {
                        heartbeatMs = payload["d"]?.jsonObject?.get("heartbeat_interval")?.jsonPrimitive?.longOrNull ?: heartbeatMs
                        send(Frame.Text(buildJsonObject {
                            put("op", 2)
                            putJsonObject("d") {
                                put("token", token)
                                putJsonObject("properties") {
                                    put("os", "android")
                                    put("browser", "Dhun")
                                    put("device", "Dhun")
                                }
                                put("compress", false)
                            }
                        }.toString()))
                        identified = true
                        running = true
                        heartbeatJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            while (kotlinx.coroutines.currentCoroutineContext().isActive) {
                                delay(heartbeatMs)
                                send(Frame.Text(buildJsonObject {
                                    put("op", 1)
                                    put("d", kotlinx.serialization.json.JsonNull)
                                }.toString()))
                            }
                        }
                    }
                }

                if (identified) {
                    send(Frame.Text(buildJsonObject {
                        put("op", 3)
                        put("d", activity ?: buildJsonObject {
                            put("since", 0L)
                            putJsonArray("activities") {}
                            put("status", "online")
                            put("afk", false)
                        })
                    }.toString()))
                    delay(250)
                }
                heartbeatJob?.cancel()
            }
        } catch (t: Throwable) {
            running = false
            logger.warning("Discord RPC send failed: ${t.message ?: t::class.simpleName}")
        } finally {
            client.close()
        }
    }

    enum class Type(val value: Int) {
        PLAYING(0), STREAMING(1), LISTENING(2), WATCHING(3), COMPETING(5)
    }

    enum class StatusDisplayType(val value: Int) {
        NAME(0), STATE(1), DETAILS(2)
    }
}
