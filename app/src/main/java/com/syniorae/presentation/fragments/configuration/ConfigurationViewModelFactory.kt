package com.syniorae.presentation.fragments.configuration

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.syniorae.core.di.DependencyInjection

/**
 * Factory pour créer ConfigurationViewModel avec ses dépendances
 * ✅ Version sans fuite mémoire - Context passé lors de la création
 */
class ConfigurationViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ConfigurationViewModel::class.java)) {
            return ConfigurationViewModel(
                widgetRepository = DependencyInjection.getWidgetRepository(context)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}