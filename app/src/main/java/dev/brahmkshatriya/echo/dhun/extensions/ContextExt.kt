/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package dev.brahmkshatriya.echo.dhun.extensions

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dev.brahmkshatriya.echo.dhun.constants.InnerTubeCookieKey
import dev.brahmkshatriya.echo.dhun.constants.YtmSyncKey
import dev.brahmkshatriya.echo.dhun.utils.dataStore
import dev.brahmkshatriya.echo.dhun.utils.get
import dev.brahmkshatriya.echo.dhun.innertube.utils.parseCookieString

fun Context.isSyncEnabled(): Boolean {
    return dataStore.get(YtmSyncKey, true) && isUserLoggedIn()
}

fun Context.isUserLoggedIn(): Boolean {
    val cookie = dataStore[InnerTubeCookieKey] ?: ""
    return "SAPISID" in parseCookieString(cookie) && isInternetConnected()
}

fun Context.isInternetConnected(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
}
