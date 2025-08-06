package com.syniorae.presentation.fragments.configuration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.syniorae.core.di.DependencyInjection

/**
 * Factory pour créer ConfigurationViewModel avec ses dépendances
 */
class ConfigurationViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ConfigurationViewModel::class.java)) {
            return ConfigurationViewModel(
                widgetRepository = DependencyInjection.getWidgetRepository()
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}