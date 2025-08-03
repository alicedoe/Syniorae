package com.syniorae.domain.models

/**
 * Énumération des rôles utilisateur
 */
enum class UserRole {
    USER,           // Consultation simple (page 1 uniquement)
    CONFIGURATOR    // Accès aux configurations (pages 1, 2, 3)
}

/**
 * Modèle représentant l'utilisateur de l'application
 */
data class User(
    val role: UserRole = UserRole.USER,
    val isFirstLaunch: Boolean = true,
    val lastRoleSwitch: Long = System.currentTimeMillis()
) {

    /**
     * Vérifie si l'utilisateur peut accéder aux pages de configuration
     */
    fun canAccessConfiguration(): Boolean {
        return role == UserRole.CONFIGURATOR
    }

    /**
     * Vérifie si c'est le premier lancement de l'application
     */
    fun isNewUser(): Boolean {
        return isFirstLaunch
    }

    companion object {
        /**
         * Crée un utilisateur par défaut (nouveau)
         */
        fun createDefault() = User(
            role = UserRole.USER,
            isFirstLaunch = true
        )

        /**
         * Crée un configurateur
         */
        fun createConfigurator() = User(
            role = UserRole.CONFIGURATOR,
            isFirstLaunch = false,
            lastRoleSwitch = System.currentTimeMillis()
        )
    }
}