package com.syniorae.core.extensions

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Extensions Kotlin pour simplifier le développement SyniOrae
 */

// === Extensions pour Context ===

/**
 * Affiche un toast court
 */
fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

/**
 * Affiche un toast long
 */
fun Context.showLongToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

// === Extensions pour Fragment ===

/**
 * Affiche un toast depuis un fragment
 */
fun Fragment.showToast(message: String) {
    context?.showToast(message)
}

/**
 * Affiche un toast long depuis un fragment
 */
fun Fragment.showLongToast(message: String) {
    context?.showLongToast(message)
}

// === Extensions pour String ===

/**
 * Vérifie si une chaîne est un email valide
 */
fun String?.isValidEmail(): Boolean {
    return this != null && android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

/**
 * Capitalise la première lettre
 */
fun String.capitalizeFirst(): String {
    return if (isNotEmpty()) {
        this[0].uppercase() + substring(1).lowercase()
    } else {
        this
    }
}

/**
 * Tronque une chaîne à une longueur donnée avec des points de suspension
 */
fun String.truncate(maxLength: Int, suffix: String = "..."): String {
    return if (length <= maxLength) {
        this
    } else {
        take(maxLength - suffix.length) + suffix
    }
}

/**
 * Supprime les espaces en début et fin et remplace les espaces multiples par un seul
 */
fun String.cleanSpaces(): String {
    return trim().replace(Regex("\\s+"), " ")
}

// === Extensions pour LocalDateTime ===

/**
 * Vérifie si la date est aujourd'hui
 */
fun LocalDateTime.isToday(): Boolean {
    return toLocalDate() == java.time.LocalDate.now()
}

/**
 * Vérifie si la date est dans le futur
 */
fun LocalDateTime.isFuture(): Boolean {
    return isAfter(LocalDateTime.now())
}

/**
 * Vérifie si la date est dans le passé
 */
fun LocalDateTime.isPast(): Boolean {
    return isBefore(LocalDateTime.now())
}

/**
 * Formate la date selon la locale par défaut
 */
fun LocalDateTime.formatForDisplay(): String {
    return format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy 'à' HH'h'mm", Locale.getDefault()))
}

/**
 * Formate seulement l'heure
 */
fun LocalDateTime.formatTime(): String {
    return format(DateTimeFormatter.ofPattern("HH'h'mm"))
}

/**
 * Formate seulement la date
 */
fun LocalDateTime.formatDate(): String {
    return format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.getDefault()))
}

// === Extensions pour Collection ===

/**
 * Vérifie si une liste n'est ni null ni vide
 */
fun <T> Collection<T>?.isNotNullOrEmpty(): Boolean {
    return this != null && isNotEmpty()
}

/**
 * Retourne la liste ou une liste vide si null
 */
fun <T> Collection<T>?.orEmpty(): Collection<T> {
    return this ?: emptyList()
}

/**
 * Sépare une liste en deux selon un prédicat
 */
fun <T> Collection<T>.partition(predicate: (T) -> Boolean): Pair<List<T>, List<T>> {
    val first = mutableListOf<T>()
    val second = mutableListOf<T>()

    forEach { element ->
        if (predicate(element)) {
            first.add(element)
        } else {
            second.add(element)
        }
    }

    return Pair(first, second)
}

// === Extensions pour coroutines ===

/**
 * Lance une coroutine avec un délai
 */
fun CoroutineScope.launchDelayed(delayMs: Long, block: suspend CoroutineScope.() -> Unit): Job {
    return launch {
        delay(delayMs)
        block()
    }
}

/**
 * Lance une coroutine qui se répète à intervalles réguliers
 */
fun CoroutineScope.launchPeriodicAsync(
    intervalMs: Long,
    block: suspend CoroutineScope.() -> Unit
): Job {
    return launch {
        while (true) {
            block()
            delay(intervalMs)
        }
    }
}

// === Extensions pour Boolean ===

/**
 * Exécute une action si la valeur est true
 */
inline fun Boolean.ifTrue(action: () -> Unit): Boolean {
    if (this) action()
    return this
}

/**
 * Exécute une action si la valeur est false
 */
inline fun Boolean.ifFalse(action: () -> Unit): Boolean {
    if (!this) action()
    return this
}

// === Extensions pour Int ===

/**
 * Convertit des heures en millisecondes
 */
fun Int.hoursToMillis(): Long {
    return this * 60 * 60 * 1000L
}

/**
 * Convertit des minutes en millisecondes
 */
fun Int.minutesToMillis(): Long {
    return this * 60 * 1000L
}

/**
 * Limite une valeur entre min et max
 */
fun Int.clamp(min: Int, max: Int): Int {
    return when {
        this < min -> min
        this > max -> max
        else -> this
    }
}

// === Extensions pour Float ===

/**
 * Limite une valeur entre min et max
 */
fun Float.clamp(min: Float, max: Float): Float {
    return when {
        this < min -> min
        this > max -> max
        else -> this
    }
}

// === Extensions pour les exceptions ===

/**
 * Retourne un message d'erreur lisible
 */
fun Throwable.getUserFriendlyMessage(): String {
    return when (this) {
        is java.net.UnknownHostException -> "Pas de connexion internet"
        is java.net.SocketTimeoutException -> "Délai d'attente dépassé"
        is java.net.ConnectException -> "Impossible de se connecter au serveur"
        is SecurityException -> "Permissions insuffisantes"
        is IllegalArgumentException -> "Paramètre invalide"
        is IllegalStateException -> "État invalide de l'application"
        else -> message ?: "Erreur inconnue"
    }
}

// === Extensions pour les logs ===

/**
 * Log avec tag automatique basé sur la classe
 */
inline fun <reified T> T.logDebug(message: String) {
    android.util.Log.d(T::class.java.simpleName, message)
}

/**
 * Log d'erreur avec tag automatique
 */
inline fun <reified T> T.logError(message: String, throwable: Throwable? = null) {
    if (throwable != null) {
        android.util.Log.e(T::class.java.simpleName, message, throwable)
    } else {
        android.util.Log.e(T::class.java.simpleName, message)
    }
}

/**
 * Log d'avertissement avec tag automatique
 */
inline fun <reified T> T.logWarning(message: String) {
    android.util.Log.w(T::class.java.simpleName, message)
}

// === Extensions spécifiques SyniOrae ===

/**
 * Formate un nombre d'événements de façon lisible
 */
fun Int.formatEventsCount(): String {
    return when (this) {
        0 -> "Aucun événement"
        1 -> "1 événement"
        else -> "$this événements"
    }
}

/**
 * Formate une fréquence de synchronisation
 */
fun Int.formatSyncFrequency(): String {
    return when (this) {
        1 -> "Toutes les heures"
        24 -> "Une fois par jour"
        else -> "Toutes les $this heures"
    }
}

/**
 * Vérifie si un email est un compte Google valide
 */
fun String.isGoogleAccount(): Boolean {
    return isValidEmail() && (endsWith("@gmail.com") || endsWith("@googlemail.com") || contains("@"))
}