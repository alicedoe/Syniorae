package com.syniorae.data.remote.google

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Client HTTP générique pour les APIs Google
 * Gère l'authentification, les headers et les timeouts
 */
class GoogleApiClient(
    private val context: Context,
    private val tokenManager: GoogleTokenManager
) {

    companion object {
        private const val TAG = "GoogleApiClient"
        private const val BASE_URL = "https://www.googleapis.com"
        private const val USER_AGENT = "SyniOrae/1.0 Android"
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 1000L
    }

    // Client HTTP configuré avec intercepteurs et timeouts
    private val httpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(createAuthInterceptor())
            .addInterceptor(createUserAgentInterceptor())
            .addInterceptor(createRetryInterceptor())

        // Logging en mode debug uniquement
        if (android.util.Log.isLoggable(TAG, Log.DEBUG)) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }

        builder.build()
    }

    /**
     * Effectue un appel GET vers une API Google
     */
    suspend fun get(endpoint: String, params: Map<String, String> = emptyMap()): ApiResponse {
        return withContext(Dispatchers.IO) {
            try {
                val url = buildUrl(endpoint, params)
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                executeRequest(request)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors du GET $endpoint", e)
                ApiResponse.Error("Erreur réseau: ${e.message}")
            }
        }
    }

    /**
     * Effectue un appel POST vers une API Google
     */
    suspend fun post(endpoint: String, body: RequestBody? = null): ApiResponse {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$BASE_URL$endpoint"
                val request = Request.Builder()
                    .url(url)
                    .post(body ?: RequestBody.create(null, ByteArray(0)))
                    .build()

                executeRequest(request)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors du POST $endpoint", e)
                ApiResponse.Error("Erreur réseau: ${e.message}")
            }
        }
    }

    /**
     * Exécute une requête HTTP avec retry automatique
     */
    private suspend fun executeRequest(request: Request): ApiResponse {
        var lastException: Exception? = null

        repeat(MAX_RETRIES) { attempt ->
            try {
                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                return when {
                    response.isSuccessful -> {
                        Log.d(TAG, "Requête réussie: ${request.url}")
                        ApiResponse.Success(responseBody, response.headers.toMap())
                    }
                    response.code == 401 -> {
                        // Token expiré, essayer de le rafraîchir
                        Log.w(TAG, "Token expiré, tentative de rafraîchissement")
                        if (tokenManager.refreshToken()) {
                            // Retry avec le nouveau token
                            kotlinx.coroutines.delay(RETRY_DELAY_MS)
                            return@repeat
                        } else {
                            ApiResponse.Error("Authentification expirée", response.code)
                        }
                    }
                    response.code >= 500 -> {
                        // Erreur serveur, retry
                        Log.w(TAG, "Erreur serveur ${response.code}, tentative ${attempt + 1}")
                        if (attempt < MAX_RETRIES - 1) {
                            kotlinx.coroutines.delay(RETRY_DELAY_MS * (attempt + 1))
                            return@repeat
                        }
                        ApiResponse.Error("Erreur serveur: $responseBody", response.code)
                    }
                    else -> {
                        // Erreur client
                        Log.w(TAG, "Erreur client ${response.code}: $responseBody")
                        ApiResponse.Error("Erreur API: $responseBody", response.code)
                    }
                }
            } catch (e: IOException) {
                Log.w(TAG, "Erreur réseau tentative ${attempt + 1}: ${e.message}")
                lastException = e
                if (attempt < MAX_RETRIES - 1) {
                    kotlinx.coroutines.delay(RETRY_DELAY_MS * (attempt + 1))
                }
            }
        }

        return ApiResponse.Error("Échec après $MAX_RETRIES tentatives: ${lastException?.message}")
    }

    /**
     * Construit une URL avec paramètres de requête
     */
    private fun buildUrl(endpoint: String, params: Map<String, String>): String {
        val urlBuilder = StringBuilder("$BASE_URL$endpoint")

        if (params.isNotEmpty()) {
            urlBuilder.append("?")
            params.entries.forEachIndexed { index, (key, value) ->
                if (index > 0) urlBuilder.append("&")
                urlBuilder.append("$key=${java.net.URLEncoder.encode(value, "UTF-8")}")
            }
        }

        return urlBuilder.toString()
    }

    /**
     * Intercepteur d'authentification
     */
    private fun createAuthInterceptor(): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()
            val token = tokenManager.getAccessToken()

            if (token.isNullOrBlank()) {
                Log.w(TAG, "Token d'accès manquant pour la requête: ${originalRequest.url}")
                return@Interceptor chain.proceed(originalRequest)
            }

            val authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()

            chain.proceed(authenticatedRequest)
        }
    }

    /**
     * Intercepteur User-Agent
     */
    private fun createUserAgentInterceptor(): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()
            val requestWithUserAgent = originalRequest.newBuilder()
                .header("User-Agent", USER_AGENT)
                .build()

            chain.proceed(requestWithUserAgent)
        }
    }

    /**
     * Intercepteur de retry pour les erreurs temporaires
     */
    private fun createRetryInterceptor(): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()
            var response = chain.proceed(request)

            // Retry pour les erreurs 429 (Rate Limiting) et 503 (Service Unavailable)
            if (response.code == 429 || response.code == 503) {
                Log.w(TAG, "Rate limiting ou service indisponible, attente...")

                // Attendre selon les headers de retry
                val retryAfter = response.header("Retry-After")?.toLongOrNull() ?: 5
                Thread.sleep(retryAfter * 1000)

                response.close()
                response = chain.proceed(request)
            }

            response
        }
    }

    /**
     * Teste la connectivité avec les APIs Google
     */
    suspend fun testConnectivity(): Boolean {
        return try {
            val response = get("/oauth2/v2/userinfo")
            response is ApiResponse.Success
        } catch (e: Exception) {
            Log.e(TAG, "Test de connectivité échoué", e)
            false
        }
    }

    /**
     * Nettoie les ressources
     */
    fun cleanup() {
        httpClient.connectionPool.evictAll()
    }
}

/**
 * Réponse d'une API Google
 */
sealed class ApiResponse {
    data class Success(
        val body: String,
        val headers: Map<String, String> = emptyMap()
    ) : ApiResponse()

    data class Error(
        val message: String,
        val code: Int? = null
    ) : ApiResponse()
}

/**
 * Extension pour convertir Headers en Map
 */
private fun Headers.toMap(): Map<String, String> {
    val map = mutableMapOf<String, String>()
    for (i in 0 until size) {
        map[name(i)] = value(i)
    }
    return map
}