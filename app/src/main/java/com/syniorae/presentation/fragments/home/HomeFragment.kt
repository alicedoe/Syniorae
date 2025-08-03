package com.syniorae.presentation.fragments.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.syniorae.databinding.FragmentHomeBinding
import com.syniorae.presentation.common.NavigationEvent
import kotlinx.coroutines.launch

/**
 * Fragment de la page d'accueil (Page 1)
 * Affiche la date du jour et les événements du calendrier
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // TODO: Remplacer par l'injection de dépendances (Hilt) plus tard
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeViewModel()
    }

    /**
     * Configure l'interface utilisateur
     */
    private fun setupUI() {
        setupSettingsIcon()
    }

    /**
     * Configure l'icône paramètre avec appui long
     */
    private fun setupSettingsIcon() {
        binding.settingsIcon.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    viewModel.onSettingsIconPressed()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    viewModel.onSettingsIconReleased()
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Observe les changements du ViewModel
     */
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            // État de la vue
            viewModel.viewState.collect { state ->
                updateUI(state)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Progression de l'appui long
            viewModel.longPressProgress.collect { progress ->
                updateLongPressProgress(progress)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Navigation
            viewModel.navigationEvent.collect { event ->
                handleNavigationEvent(event)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Erreurs
            viewModel.error.collect { error ->
                showError(error)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Messages
            viewModel.message.collect { message ->
                showMessage(message)
            }
        }
    }

    /**
     * Met à jour l'interface utilisateur selon l'état
     */
    private fun updateUI(state: HomeViewState) {
        // Date
        binding.dayOfWeek.text = state.dayOfWeek
        binding.dayOfMonth.text = state.dayOfMonth
        binding.monthYear.text = state.monthYear

        // Colonne droite
        if (state.hasCalendarWidget) {
            if (state.hasTodayEvents()) {
                // Afficher les événements du jour
                binding.rightColumnMessage.visibility = View.GONE
                binding.todayEventsList.visibility = View.VISIBLE
                // TODO: Adapter pour afficher la liste des événements
            } else {
                // Afficher le message "Aucun événement"
                binding.rightColumnMessage.visibility = View.VISIBLE
                binding.todayEventsList.visibility = View.GONE
                binding.rightColumnMessage.text = state.getRightColumnMessage()
            }
        } else {
            // Pas de widget calendrier activé
            binding.rightColumnMessage.visibility = View.GONE
            binding.todayEventsList.visibility = View.GONE
        }

        // Événements futurs
        if (state.hasFutureEvents()) {
            binding.futureEventsSection.visibility = View.VISIBLE
            // TODO: Adapter pour afficher les événements futurs groupés
        } else {
            binding.futureEventsSection.visibility = View.GONE
        }
    }

    /**
     * Met à jour la progression de l'appui long
     */
    private fun updateLongPressProgress(progress: Float) {
        // TODO: Animer un cercle de progression autour de l'icône paramètre
        val alpha = if (progress > 0) 0.3f + (progress * 0.7f) else 1f
        binding.settingsIcon.alpha = alpha

        // TODO: Ajouter un cercle de progression visuel
    }

    /**
     * Gère les événements de navigation
     */
    private fun handleNavigationEvent(event: NavigationEvent) {
        when (event) {
            is NavigationEvent.NavigateToConfiguration -> {
                findNavController().navigate(
                    com.syniorae.R.id.action_home_to_configuration
                )
            }
            else -> {
                // Autres événements de navigation
            }
        }
    }

    /**
     * Affiche un message d'erreur
     */
    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("OK") { }
            .show()
    }

    /**
     * Affiche un message d'information
     */
    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}