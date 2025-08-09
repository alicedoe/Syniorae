package com.syniorae.presentation.fragments.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.syniorae.R
import com.syniorae.databinding.FragmentCalendarSettingsBinding
import com.syniorae.presentation.activities.CalendarConfigurationActivity
import com.syniorae.presentation.fragments.calendar.settings.CalendarSettingsEvent
import com.syniorae.presentation.fragments.calendar.settings.CalendarSettingsUiState
import com.syniorae.presentation.fragments.calendar.settings.CalendarSettingsViewModel
import com.syniorae.presentation.fragments.calendar.settings.CalendarSettingsViewModelFactory
import kotlinx.coroutines.launch

/**
 * Page 3 - Paramètres détaillés du widget Calendrier Google
 * Affiche toutes les configurations actuelles avec possibilité de les modifier
 */
class CalendarSettingsFragment : Fragment() {

    private var _binding: FragmentCalendarSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CalendarSettingsViewModel by viewModels {
        CalendarSettingsViewModelFactory()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupClickListeners()
        observeViewModel()

        // Charger les données
        viewModel.loadSettings()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupClickListeners() {
        // Carte Calendrier sélectionné
        binding.calendarSelectionCard.setOnClickListener {
            viewModel.editCalendarSelection()
        }

        // Carte Période de récupération
        binding.periodCard.setOnClickListener {
            viewModel.editPeriod()
        }

        // Carte Nombre d'événements
        binding.eventsCountCard.setOnClickListener {
            viewModel.editEventsCount()
        }

        // Carte Fréquence de synchronisation
        binding.syncFrequencyCard.setOnClickListener {
            viewModel.editSyncFrequency()
        }

        // Carte Associations d'icônes
        binding.iconAssociationsCard.setOnClickListener {
            viewModel.editIconAssociations()
        }

        // Bouton Déconnecter Google
        binding.disconnectButton.setOnClickListener {
            viewModel.showDisconnectConfirmation()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                updateUI(state)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.events.collect { event ->
                handleEvent(event)
            }
        }
    }

    private fun updateUI(state: CalendarSettingsUiState) {
        // Carte Compte Google
        binding.googleAccountEmail.text = state.googleAccountEmail
        binding.googleAccountCard.visibility = if (state.googleAccountEmail.isNotEmpty()) View.VISIBLE else View.GONE

        // Carte Calendrier sélectionné
        binding.selectedCalendarName.text = state.selectedCalendarName
        binding.calendarSelectionCard.visibility = if (state.selectedCalendarName.isNotEmpty()) View.VISIBLE else View.GONE

        // Carte Période de récupération
        binding.periodText.text = getString(R.string.period_weeks_format, state.weeksAhead)
        binding.periodCard.visibility = if (state.weeksAhead > 0) View.VISIBLE else View.GONE

        // Carte Nombre d'événements
        binding.eventsCountText.text = getString(R.string.events_max_format, state.maxEvents)
        binding.eventsCountCard.visibility = if (state.maxEvents > 0) View.VISIBLE else View.GONE

        // Carte Fréquence de synchronisation
        binding.syncFrequencyText.text = getString(R.string.sync_frequency_format, state.syncFrequencyHours)
        binding.syncFrequencyCard.visibility = if (state.syncFrequencyHours > 0) View.VISIBLE else View.GONE

        // Carte Associations d'icônes
        binding.iconAssociationsText.text = getString(R.string.icon_associations_format, state.iconAssociationsCount)
        binding.iconAssociationsCard.visibility = View.VISIBLE

        // États de chargement
        binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        binding.settingsContainer.visibility = if (state.isLoading) View.GONE else View.VISIBLE
    }

    private fun handleEvent(event: CalendarSettingsEvent) {
        when (event) {
            is CalendarSettingsEvent.ShowDisconnectDialog -> {
                showDisconnectConfirmationDialog()
            }
            is CalendarSettingsEvent.NavigateToStep -> {
                navigateToConfigurationStep(event.step)
            }
            is CalendarSettingsEvent.NavigateBack -> {
                findNavController().navigateUp()
            }
            is CalendarSettingsEvent.ShowMessage -> {
                showMessage(event.message)
            }
            is CalendarSettingsEvent.ShowError -> {
                showError(event.message)
            }
        }
    }

    private fun showDisconnectConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.disconnect_google_title)
            .setMessage(R.string.disconnect_google_message)
            .setPositiveButton(R.string.disconnect) { _, _ ->
                viewModel.disconnectGoogle()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun navigateToConfigurationStep(step: Int) {
        // Utiliser CalendarConfigurationActivity pour accéder aux étapes spécifiques
        val intent = CalendarConfigurationActivity.newIntent(requireContext()).apply {
            // Ajouter l'étape spécifique à lancer en extra
            putExtra("start_step", step)
        }
        startActivity(intent)
    }

    private fun showMessage(message: String) {
        ContextCompat.getMainExecutor(requireContext()).execute {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showError(message: String) {
        ContextCompat.getMainExecutor(requireContext()).execute {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}