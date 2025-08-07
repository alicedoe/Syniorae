package com.syniorae.presentation.fragments.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.syniorae.core.di.DependencyInjection

/**
 * Factory pour créer HomeViewModel avec ses dépendances
 * ✅ Version sans fuite mémoire - Context passé lors de la création
 */
class HomeViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(
                widgetRepository = DependencyInjection.getWidgetRepository(context),
                jsonFileManager = DependencyInjection.getJsonFileManager(context)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}