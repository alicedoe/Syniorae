package com.syniorae.presentation.fragments.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory pour créer SettingsViewModel avec ses dépendances
 * Pour l'instant simple car SettingsFragment n'a pas de logique complexe
 */
class SettingsViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

/**
 * ViewModel simple pour la page des paramètres
 * À développer quand la logique sera implémentée
 */
class SettingsViewModel : ViewModel() {
    // TODO: Implémenter la logique des paramètres détaillés
}