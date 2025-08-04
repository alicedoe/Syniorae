package com.syniorae.presentation.fragments.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory pour créer SettingsViewModel avec ses dépendances
 */
class SettingsViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

/**
 * ViewModel pour la page des paramètres détaillés (Page 3)
 * Pour l'instant, page placeholder selon le cahier des charges
 */
class SettingsViewModel : ViewModel() {

    init {
        // TODO: Implémenter la logique des paramètres détaillés
        // - Gestion des cartes de paramètres (étapes 2-6 du tunnel)
        // - Carte compte Google connecté
        // - Actions de déconnexion
        // - Navigation vers les étapes de configuration individuelles
    }
}