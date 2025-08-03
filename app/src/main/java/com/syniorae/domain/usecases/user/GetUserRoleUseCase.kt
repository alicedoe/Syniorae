package com.syniorae.domain.usecases.user

import com.syniorae.data.repository.UserRepository
import com.syniorae.domain.models.User
import com.syniorae.domain.models.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Use case pour récupérer le rôle de l'utilisateur actuel
 */
class GetUserRoleUseCase(
    private val userRepository: UserRepository
) {

    /**
     * Récupère l'utilisateur actuel
     */
    fun execute(): User {
        return userRepository.getCurrentUser()
    }

    /**
     * Observe les changements d'utilisateur
     */
    fun observeUser(): Flow<User> {
        return userRepository.currentUser
    }

    /**
     * Vérifie si l'utilisateur peut accéder à la configuration
     */
    fun canAccessConfiguration(): Boolean {
        return userRepository.canAccessConfiguration()
    }

    /**
     * Récupère le rôle actuel
     */
    fun getCurrentRole(): UserRole {
        return userRepository.getCurrentRole()
    }

    /**
     * Vérifie si c'est le premier lancement
     */
    fun isFirstLaunch(): Boolean {
        return userRepository.isFirstLaunch()
    }
}

/**
 * Use case pour passer en mode configurateur
 */
class SwitchToConfiguratorUseCase(
    private val userRepository: UserRepository
) {

    suspend fun execute(): Result<Unit> {
        return try {
            val success = userRepository.switchToConfigurator()
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Impossible de passer en mode configurateur"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Use case pour passer en mode utilisateur
 */
class SwitchToUserUseCase(
    private val userRepository: UserRepository
) {

    suspend fun execute(): Result<Unit> {
        return try {
            val success = userRepository.switchToUser()
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Impossible de passer en mode utilisateur"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Use case pour marquer le premier lancement comme terminé
 */
class MarkFirstLaunchCompleteUseCase(
    private val userRepository: UserRepository
) {

    suspend fun execute(): Result<Unit> {
        return try {
            val success = userRepository.markFirstLaunchComplete()
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Impossible de marquer le premier lancement comme terminé"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}