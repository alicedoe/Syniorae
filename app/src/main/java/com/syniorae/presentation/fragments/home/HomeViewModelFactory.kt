package com.syniorae.presentation.fragments.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.syniorae.core.di.DependencyInjection

/**
 * Factory pour créer HomeViewModel avec ses dépendances
 */
class HomeViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(
                widgetRepository = DependencyInjection.getWidgetRepository(),
                jsonFileManager = DependencyInjection.getJsonFileManager()
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}