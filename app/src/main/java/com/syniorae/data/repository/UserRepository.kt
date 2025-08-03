package com.syniorae.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.syniorae.core.constants.AppConstants
import com.syniorae.domain.models.User
import com.syniorae.domain.models.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository pour la gestion des utilisateurs et rôles
 */
class UserRepository(context: Context) {

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(
        AppConstants.SHARED_PREFS_NAME,
        Context.MODE_PRIVATE
    )

    // Flow pour observer les changements d'utilisateur
    private val _currentUser = MutableStateFlow(loadUserFromPrefs())
    val currentUser: Flow<User> = _currentUser.asStateFlow()

    /**
     * Récupère l'utilisateur actuel
     */
    fun getCurrentUser(): User {
        return _currentUser.value
    }

    /**
     * Sauvegarde un utilisateur
     */
    suspend fun saveUser(user: User): Boolean {
        return try {
            with(sharedPrefs.edit()) {
                putString(AppConstants.PREF_USER_ROLE, user.role.name)
                putBoolean(AppConstants.PREF_FIRST_LAUNCH, user.isFirstLaunch)
                putLong("last_role_switch", user.lastRoleSwitch)
                apply()
            }
            _currentUser.value = user
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Passe en mode configurateur
     */
    suspend fun switchToConfigurator(): Boolean {
        val currentUser = _currentUser.value
        val newUser = currentUser.copy(
            role = UserRole.CONFIGURATOR,
            isFirstLaunch = false,
            lastRoleSwitch = System.currentTimeMillis()
        )
        return saveUser(newUser)
    }

    /**
     * Passe en mode utilisateur
     */
    suspend fun switchToUser(): Boolean {
        val currentUser = _currentUser.value
        val newUser = currentUser.copy(
            role = UserRole.USER,
            lastRoleSwitch = System.currentTimeMillis()
        )
        return saveUser(newUser)
    }

    /**
     * Marque le premier lancement comme terminé
     */
    suspend fun markFirstLaunchComplete(): Boolean {
        val currentUser = _currentUser.value
        if (currentUser.isFirstLaunch) {
            val newUser = currentUser.copy(isFirstLaunch = false)
            return saveUser(newUser)
        }
        return true
    }

    /**
     * Vérifie si l'utilisateur peut accéder à la configuration
     */
    fun canAccessConfiguration(): Boolean {
        return _currentUser.value.canAccessConfiguration()
    }

    /**
     * Vérifie si c'est le premier lancement
     */
    fun isFirstLaunch(): Boolean {
        return _currentUser.value.isFirstLaunch
    }

    /**
     * Retourne le rôle actuel
     */
    fun getCurrentRole(): UserRole {
        return _currentUser.value.role
    }

    /**
     * Remet l'utilisateur à zéro (nouveau utilisateur)
     */
    suspend fun resetUser(): Boolean {
        val defaultUser = User.createDefault()
        return saveUser(defaultUser)
    }

    /**
     * Charge l'utilisateur depuis les préférences
     */
    private fun loadUserFromPrefs(): User {
        return try {
            val roleString = sharedPrefs.getString(AppConstants.PREF_USER_ROLE, UserRole.USER.name)
            val role = UserRole.valueOf(roleString ?: UserRole.USER.name)
            val isFirstLaunch = sharedPrefs.getBoolean(AppConstants.PREF_FIRST_LAUNCH, true)
            val lastRoleSwitch = sharedPrefs.getLong("last_role_switch", System.currentTimeMillis())

            User(
                role = role,
                isFirstLaunch = isFirstLaunch,
                lastRoleSwitch = lastRoleSwitch
            )
        } catch (e: Exception) {
            // En cas d'erreur, retourne un utilisateur par défaut
            User.createDefault()
        }
    }
}