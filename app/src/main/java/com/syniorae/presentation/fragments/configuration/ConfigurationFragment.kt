package com.syniorae.presentation.fragments.configuration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.syniorae.databinding.FragmentConfigurationBinding
import com.syniorae.presentation.common.NavigationEvent
import kotlinx.coroutines.launch

/**
 * Fragment de la page de configuration des widgets (Page 2)
 * Permet d'activer/désactiver les widgets et d'accéder à leurs paramètres
 */
class ConfigurationFragment : Fragment() {

    private var _binding: FragmentConfigurationBinding? = null
    private val binding get() = _binding!!

    // TODO: Remplacer par l'injection de dépendances (Hilt) plus tard
    private val viewModel: ConfigurationViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfigurationBinding.inflate(inflater, container, false)
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
        setupToolbar()
        setupCalendarWidget()
    }

    /**
     * Configure la barre d'outils
     */
    private fun setupToolbar() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    /**
     * Configure le widget calendrier
     */
    private fun setupCalendarWidget() {
        // Toggle du widget calendrier
        binding.calendarWidgetToggle.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleCalendarWidget(isChecked)
        }

        // Bouton de configuration
        binding.calendarConfigButton.setOnClickListener {
            viewModel.navigateToCalendarSettings()
        }

        // Bouton de synchronisation
        binding.calendarSyncButton.setOnClickListener {
            viewModel.syncCalendar()
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
            // État de chargement
            viewModel.isLoading.collect { isLoading ->
                updateLoadingState(isLoading)
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
    private fun updateUI(state: ConfigurationViewState) {
        // Widget Calendrier
        binding.calendarWidgetToggle.isChecked = state.calendarWidget.isEnabled

        if (state.calendarWidget.isEnabled) {
            // Widget activé - afficher les informations
            binding.calendarWidgetInfo.visibility = View.VISIBLE
            binding.calendarWidgetButtons.visibility = View.VISIBLE

            // Statut de synchronisation
            binding.calendarLastSync.text = state.calendarWidget.lastSyncDisplay
            binding.calendarEventsCount.text = state.calendarWidget.eventsCountDisplay
            binding.calendarSyncStatus.text = state.calendarWidget.syncStatusDisplay

            // Couleur du statut
            val statusColor = if (state.calendarWidget.hasError) {
                android.R.color.holo_red_dark
            } else {
                android.R.color.holo_green_dark
            }
            binding.calendarSyncStatus.setTextColor(
                resources.getColor(statusColor, null)
            )
        } else {
            // Widget désactivé - masquer les informations
            binding.calendarWidgetInfo.visibility = View.GONE
            binding.calendarWidgetButtons.visibility = View.GONE
        }

        // État des boutons
        binding.calendarConfigButton.isEnabled = !state.isLoading
        binding.calendarSyncButton.isEnabled = !state.isLoading && state.calendarWidget.isEnabled
    }

    /**
     * Met à jour l'état de chargement
     */
    private fun updateLoadingState(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    /**
     * Gère les événements de navigation
     */
    private fun handleNavigationEvent(event: NavigationEvent) {
        when (event) {
            is NavigationEvent.NavigateToSettings -> {
                findNavController().navigate(
                    com.syniorae.R.id.action_configuration_to_settings
                )
            }
            is NavigationEvent.NavigateBack -> {
                findNavController().navigateUp()
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