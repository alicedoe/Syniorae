package com.syniorae.presentation.fragments.configuration

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.syniorae.databinding.FragmentConfigurationBinding
import com.syniorae.presentation.activities.CalendarConfigurationActivity
import com.syniorae.presentation.common.NavigationEvent
import kotlinx.coroutines.launch

/**
 * Fragment de la page de configuration des widgets (Page 2)
 * ✅ Version sans fuite mémoire
 */
class ConfigurationFragment : Fragment() {

    private var _binding: FragmentConfigurationBinding? = null
    private val binding get() = _binding!!

    // ✅ ViewModel avec factory qui prend le Context
    private val viewModel: ConfigurationViewModel by viewModels {
        ConfigurationViewModelFactory(requireContext().applicationContext)
    }

    // Activity Result Launcher pour remplacer startActivityForResult
    private val calendarConfigurationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                viewModel.onConfigurationCompleted()
            }
            Activity.RESULT_CANCELED -> {
                viewModel.onConfigurationCancelled()
            }
        }
    }

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
            viewModel.viewState.collect { state ->
                updateUI(state)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                updateLoadingState(isLoading)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.navigationEvent.collect { event ->
                handleNavigationEvent(event)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                showError(error)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.message.collect { message ->
                showMessage(message)
            }
        }

        // Gestion du lancement du tunnel de configuration
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.configurationLaunchEvent.collect {
                launchCalendarConfiguration()
            }
        }
    }

    /**
     * Met à jour l'interface utilisateur selon l'état
     */
    private fun updateUI(state: ConfigurationViewState) {
        // Widget Calendrier

        // Éviter les boucles infinies sur le toggle
        if (binding.calendarWidgetToggle.isChecked != state.calendarWidget.isEnabled) {
            binding.calendarWidgetToggle.isChecked = state.calendarWidget.isEnabled
        }

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
        binding.calendarConfigButton.isEnabled = true
        binding.calendarSyncButton.isEnabled = state.calendarWidget.isEnabled && !state.calendarWidget.hasError
    }

    /**
     * Met à jour l'état de chargement
     */
    private fun updateLoadingState(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE

        // Désactiver les interactions pendant le chargement
        binding.calendarWidgetToggle.isEnabled = !isLoading
        binding.calendarConfigButton.isEnabled = !isLoading
        binding.calendarSyncButton.isEnabled = !isLoading
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
     * Lance le tunnel de configuration du calendrier
     * Utilise la nouvelle API Activity Result
     */
    private fun launchCalendarConfiguration() {
        val intent = CalendarConfigurationActivity.newIntent(requireContext())
        calendarConfigurationLauncher.launch(intent)
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