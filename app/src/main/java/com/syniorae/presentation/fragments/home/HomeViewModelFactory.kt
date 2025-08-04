package com.syniorae.presentation.fragments.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.syniorae.domain.usecases.navigation.HandleLongPressUseCase
import com.syniorae.domain.usecases.navigation.NavigateToPageUseCase
import com.syniorae.domain.usecases.widgets.common.GetAllWidgetsUseCase

/**
 * Factory pour créer HomeViewModel avec ses dépendances
 */
class HomeViewModelFactory(
    private val getAllWidgetsUseCase: GetAllWidgetsUseCase,
    private val handleLongPressUseCase: HandleLongPressUseCase,
    private val navigateToPageUseCase: NavigateToPageUseCase
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(
                getAllWidgetsUseCase,
                handleLongPressUseCase,
                navigateToPageUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}