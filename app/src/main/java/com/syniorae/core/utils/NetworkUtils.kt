package com.syniorae.core.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.Dispatchers
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Utilitaires pour la gestion réseau et connectivité
 * Optimisé pour les besoins de synchronisation de SyniOrae
 */
object NetworkUtils {

    /**
     * Vérifie si une connexion internet est disponible
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            activeNetworkInfo?.isConnected == true
        }
    }

    /**
     * Vérifie si la connexion est en WiFi
     */
    fun isWifiConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } else {
            @Suppress("DEPRECATION")
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            activeNetworkInfo?.type == ConnectivityManager.TYPE_WIFI &&
                    @Suppress("DEPRECATION")
                    activeNetworkInfo.isConnected
        }
    }

    /**
     * Vérifie si la connexion est limitée (données mobiles avec limite)
     */
    fun isConnectionMetered(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return connectivityManager.isActiveNetworkMetered
    }

    /**
     * Observe les changements de connectivité réseau
     */
    fun observeNetworkState(context: Context): Flow<NetworkState> = callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(NetworkState.Available)
            }

            override fun onLost(network: Network) {
                trySend(NetworkState.Lost)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val isWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                val isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

                trySend(
                    if (isValidated) {
                        if (isWifi) NetworkState.WiFiAvailable else NetworkState.MobileAvailable
                    } else {
                        NetworkState.Limited
                    }
                )
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Envoyer l'état initial
        trySend(getCurrentNetworkState(context))

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()

    /**
     * Obtient l'état actuel du réseau
     */
    private fun getCurrentNetworkState(context: Context): NetworkState {
        if (!isNetworkAvailable(context)) return NetworkState.Lost

        return when {
            isWifiConnected(context) -> NetworkState.WiFiAvailable
            !isConnectionMetered(context) -> NetworkState.MobileAvailable
            else -> NetworkState.Limited
        }
    }

    /**
     * Teste la connectivité vers un serveur spécifique
     */
    suspend fun testServerConnectivity(host: String, port: Int, timeoutMs: Int = 5000): Boolean {
        return try {
            withTimeout(timeoutMs.toLong()) {
                withContext(Dispatchers.IO) {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(host, port), timeoutMs)
                        true
                    }
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Teste la connectivité vers Google Calendar API
     */
    suspend fun testGoogleCalendarConnectivity(): Boolean {
        return testHttpConnectivity("https://www.googleapis.com/calendar/v3/users/me/calendarList")
    }

    /**
     * Teste une URL HTTP avec timeout (version HttpsURLConnection)
     */
    suspend fun testHttpConnectivity(url: String, timeoutMs: Int = 10000): Boolean {
        return try {
            withTimeout(timeoutMs.toLong()) {
                withContext(Dispatchers.IO) {
                    val connection = URL(url).openConnection() as HttpsURLConnection
                    connection.apply {
                        requestMethod = "HEAD" // Utiliser HEAD pour économiser la bande passante
                        connectTimeout = timeoutMs
                        readTimeout = timeoutMs
                        instanceFollowRedirects = false
                    }

                    try {
                        connection.connect()
                        val responseCode = connection.responseCode
                        // Considérer comme succès si le serveur répond (même avec erreur 4xx)
                        responseCode in 200..499
                    } finally {
                        connection.disconnect()
                    }
                }
            }
        } catch (e: IOException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Calcule la qualité de connexion basée sur la latence
     */
    suspend fun measureConnectionQuality(host: String = "8.8.8.8"): ConnectionQuality {
        val startTime = System.currentTimeMillis()
        val isReachable = testServerConnectivity(host, 53, 3000) // DNS Google
        val latency = System.currentTimeMillis() - startTime

        return when {
            !isReachable -> ConnectionQuality.NO_CONNECTION
            latency < 100 -> ConnectionQuality.EXCELLENT
            latency < 300 -> ConnectionQuality.GOOD
            latency < 1000 -> ConnectionQuality.FAIR
            else -> ConnectionQuality.POOR
        }
    }

    /**
     * Vérifie si les conditions sont optimales pour la synchronisation
     */
    fun isOptimalForSync(context: Context, requireWifi: Boolean = false, requireCharging: Boolean = false): Boolean {
        if (!isNetworkAvailable(context)) return false
        if (requireWifi && !isWifiConnected(context)) return false
        if (requireCharging && !DeviceUtils.isCharging(context)) return false

        return true
    }

    /**
     * Estime la bande passante disponible (approximation)
     */
    suspend fun estimateBandwidth(): BandwidthEstimate {
        val startTime = System.currentTimeMillis()

        return try {
            // Test avec un petit fichier pour estimer la vitesse
            val testUrl = "https://www.google.com/robots.txt"

            withTimeout(10000L) {
                withContext(Dispatchers.IO) {
                    val connection = URL(testUrl).openConnection() as HttpsURLConnection
                    connection.apply {
                        connectTimeout = 5000
                        readTimeout = 10000
                    }

                    try {
                        connection.connect()
                        if (connection.responseCode == 200) {
                            val bytes = connection.contentLength.toLong()
                            val duration = System.currentTimeMillis() - startTime
                            val kbps = if (duration > 0) (bytes * 8) / duration else 0

                            when {
                                kbps > 1000 -> BandwidthEstimate.HIGH
                                kbps > 100 -> BandwidthEstimate.MEDIUM
                                kbps > 0 -> BandwidthEstimate.LOW
                                else -> BandwidthEstimate.UNKNOWN
                            }
                        } else {
                            BandwidthEstimate.UNKNOWN
                        }
                    } finally {
                        connection.disconnect()
                    }
                }
            }
        } catch (e: Exception) {
            BandwidthEstimate.UNKNOWN
        }
    }

    /**
     * Retourne le type de réseau actuel
     */
    fun getNetworkType(context: Context): NetworkType {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return NetworkType.NONE
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.NONE

            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.MOBILE
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
                else -> NetworkType.OTHER
            }
        } else {
            @Suppress("DEPRECATION")
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
                @Suppress("DEPRECATION")
                when (activeNetworkInfo.type) {
                    @Suppress("DEPRECATION")
                    ConnectivityManager.TYPE_WIFI -> NetworkType.WIFI
                    @Suppress("DEPRECATION")
                    ConnectivityManager.TYPE_MOBILE -> NetworkType.MOBILE
                    @Suppress("DEPRECATION")
                    ConnectivityManager.TYPE_ETHERNET -> NetworkType.ETHERNET
                    else -> NetworkType.OTHER
                }
            } else {
                NetworkType.NONE
            }
        }
    }
}

/**
 * États possibles du réseau
 */
enum class NetworkState {
    Available,          // Réseau disponible
    WiFiAvailable,     // WiFi disponible
    MobileAvailable,   // Données mobiles disponibles
    Limited,           // Connexion limitée
    Lost               // Pas de réseau
}

/**
 * Qualité de connexion basée sur la latence
 */
enum class ConnectionQuality {
    NO_CONNECTION,     // Pas de connexion
    POOR,             // > 1000ms
    FAIR,             // 300-1000ms
    GOOD,             // 100-300ms
    EXCELLENT         // < 100ms
}

/**
 * Estimation de la bande passante
 */
enum class BandwidthEstimate {
    UNKNOWN,          // Impossible à déterminer
    LOW,              // < 100 kbps
    MEDIUM,           // 100-1000 kbps
    HIGH              // > 1000 kbps
}

/**
 * Types de réseau
 */
enum class NetworkType {
    NONE,             // Pas de réseau
    WIFI,             // WiFi
    MOBILE,           // Données mobiles
    ETHERNET,         // Ethernet
    OTHER             // Autre type
}

/**
 * Utilitaires pour l'état de l'appareil
 */
object DeviceUtils {

    /**
     * Vérifie si l'appareil est en charge
     */
    fun isCharging(context: Context): Boolean {
        val batteryIntent = context.registerReceiver(null,
            android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))

        val status = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1)
        return status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                status == android.os.BatteryManager.BATTERY_STATUS_FULL
    }

    /**
     * Obtient le niveau de batterie (0-100)
     */
    fun getBatteryLevel(context: Context): Int {
        val batteryIntent = context.registerReceiver(null,
            android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))

        val level = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1

        return if (level >= 0 && scale > 0) {
            (level * 100) / scale
        } else {
            -1
        }
    }
}