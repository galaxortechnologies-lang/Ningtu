package com.nigutu.app.core.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Reports whether the device is online and whether it is on an unmetered link. */
@Singleton
class NetworkStatus @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager: ConnectivityManager?
        get() = context.getSystemService(ConnectivityManager::class.java)

    fun isOnline(): Boolean {
        val active = manager ?: return false
        val capabilities = active.getNetworkCapabilities(active.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun isUnmetered(): Boolean {
        val active = manager ?: return false
        val capabilities = active.getNetworkCapabilities(active.activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}
