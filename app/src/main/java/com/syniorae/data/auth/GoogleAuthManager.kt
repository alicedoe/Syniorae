package com.syniorae.data.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.calendar.CalendarScopes
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Manager pour l'authentification Google
 * Gère la connexion, déconnexion et les permissions Calendar
 */
class GoogleAuthManager(private val context: Context) {

    private val googleSignInClient: GoogleSignInClient

    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                Scope(CalendarScopes.CALENDAR_READONLY),
                Scope(CalendarScopes.CALENDAR_EVENTS_READONLY)
            )
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    /**
     * Vérifie si l'utilisateur est déjà connecté
     */
    fun isSignedIn(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account != null && !GoogleSignIn.hasPermissions(
            account,
            Scope(CalendarScopes.CALENDAR_READONLY),
            Scope(CalendarScopes.CALENDAR_EVENTS_READONLY)
        )
    }

    /**
     * Récupère le compte connecté
     */
    fun getSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    /**
     * Lance le processus de connexion
     */
    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    /**
     * Vérifie si les permissions Calendar sont accordées
     */
    fun hasCalendarPermissions(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account != null && GoogleSignIn.hasPermissions(
            account,
            Scope(CalendarScopes.CALENDAR_READONLY),
            Scope(CalendarScopes.CALENDAR_EVENTS_READONLY)
        )
    }

    /**
     * Demande les permissions Calendar si nécessaire
     */
    fun requestCalendarPermissions(): Intent? {
        val account = getSignedInAccount() ?: return null

        if (!hasCalendarPermissions()) {
            return GoogleSignIn.getClient(context, GoogleSignInOptions.Builder()
                .requestScopes(
                    Scope(CalendarScopes.CALENDAR_READONLY),
                    Scope(CalendarScopes.CALENDAR_EVENTS_READONLY)
                )
                .build()
            ).signInIntent
        }

        return null
    }

    /**
     * Déconnecte l'utilisateur
     */
    suspend fun signOut(): Boolean = suspendCancellableCoroutine { continuation ->
        googleSignInClient.signOut().addOnCompleteListener { task ->
            continuation.resume(task.isSuccessful)
        }
    }

    /**
     * Révoque l'accès complètement
     */
    suspend fun revokeAccess(): Boolean = suspendCancellableCoroutine { continuation ->
        googleSignInClient.revokeAccess().addOnCompleteListener { task ->
            continuation.resume(task.isSuccessful)
        }
    }

    /**
     * Récupère l'email du compte connecté
     */
    fun getAccountEmail(): String? {
        return getSignedInAccount()?.email
    }

    /**
     * Récupère le nom du compte connecté
     */
    fun getAccountName(): String? {
        return getSignedInAccount()?.displayName
    }
}