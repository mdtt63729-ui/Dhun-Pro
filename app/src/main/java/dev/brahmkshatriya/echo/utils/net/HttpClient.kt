package dev.brahmkshatriya.echo.utils.net

import android.content.Context
import android.content.SharedPreferences
import dev.brahmkshatriya.echo.utils.ContextUtils.getSettings
import dev.brahmkshatriya.echo.utils.SettingsKeys
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

/**
 * The one place the app builds its HTTP client, so the "Internet" settings added from
 * ArchiveTune (DNS over HTTPS and a proxy) apply everywhere: Coil's image loading, extension
 * downloads and the crash reporter all go through [client].
 */
object HttpClient {

    val dohProviders = arrayOf("Cloudflare", "Google", "AdGuard", "Custom")

    val dohProviderValues = arrayOf(
        "https://cloudflare-dns.com/dns-query",
        "https://dns.google/resolve",
        "https://dns.adguard-dns.com/dns-query",
        SettingsKeys.DOH_PROVIDER_CUSTOM
    )

    fun client(context: Context): OkHttpClient = client(context.getSettings())

    fun client(settings: SharedPreferences): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)

        if (settings.getBoolean(SettingsKeys.PROXY_ENABLED, false)) {
            val host = settings.getString(SettingsKeys.PROXY_HOST, null)
            val port = settings.getString(SettingsKeys.PROXY_PORT, null)?.trim()?.toIntOrNull()
            if (!host.isNullOrBlank() && port != null && port in 1..65535) {
                builder.proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(host, port)))
            }
        }

        if (settings.getBoolean(SettingsKeys.DOH_ENABLED, false)) {
            dohUrl(settings)?.let { url -> builder.dns(DohDns(builder.build(), url)) }
        }

        return builder.build()
    }

    private fun dohUrl(settings: SharedPreferences): String? {
        val provider = settings.getString(SettingsKeys.DOH_PROVIDER, dohProviderValues[0])
        if (provider == null || provider == SettingsKeys.DOH_PROVIDER_CUSTOM) {
            return settings.getString(SettingsKeys.DOH_URL, null)?.trim()?.takeIf { it.isNotBlank() }
        }
        return provider
    }

    /**
     * Resolves hostnames through a DNS-over-HTTPS JSON endpoint (the `application/dns-json`
     * API that Cloudflare, Google and AdGuard all expose). Falls back to the system resolver if
     * the endpoint cannot be reached, so playback is never blocked by a bad DoH setting.
     */
    private class DohDns(
        private val client: OkHttpClient,
        private val endpoint: String,
    ) : Dns {

        override fun lookup(hostname: String): List<InetAddress> {
            if (hostname.matches(Regex("[0-9.]+")) || hostname.contains(':')) {
                return Dns.SYSTEM.lookup(hostname)
            }
            val base = endpoint.toHttpUrlOrNull() ?: return Dns.SYSTEM.lookup(hostname)
            val addresses = mutableListOf<InetAddress>()
            for (type in listOf("A", "AAAA")) {
                val url = base.newBuilder()
                    .addQueryParameter("name", hostname)
                    .addQueryParameter("type", type)
                    .build()
                runCatching {
                    client.newCall(
                        Request.Builder()
                            .url(url)
                            .header("accept", "application/dns-json")
                            .build()
                    ).execute().use { response ->
                        val body = response.body?.string() ?: return@use
                        val answers = JSONObject(body).optJSONArray("Answer") ?: return@use
                        for (i in 0 until answers.length()) {
                            val data = answers.getJSONObject(i).optString("data")
                            if (data.isNotBlank()) {
                                runCatching { addresses.add(InetAddress.getByName(data)) }
                            }
                        }
                    }
                }
            }
            return addresses.ifEmpty { Dns.SYSTEM.lookup(hostname) }
        }
    }
}
