package com.syniorae.presentation.fragments.calendar.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.syniorae.R
import com.syniorae.databinding.FragmentCalendarSettingsBinding
import kotlinx.coroutines.launch

/**
 * Page 3 - Paramètres détaillés du widget Calendrier Google
 * Affiche toutes les configurations actuelles avec possibilité de les modifier via popups
 */
class CalendarSettingsFragment : Fragment() {

    private var _binding: FragmentCalendarSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CalendarSettingsViewModel by viewModels {
        CalendarSettingsViewModel.CalendarSettingsViewModelFactory()
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
        println("🔥 CalendarSettingsFragment.setupClickListeners() - DÉBUT")

        // Carte Calendrier sélectionné
        binding.calendarSelectionCard.setOnClickListener {
            println("🔥 CalendarSettingsFragment - Carte calendrier cliquée")
            viewModel.editCalendarSelection()
        }

        // Carte Période de récupération
        binding.periodCard.setOnClickListener {
            println("🔥 CalendarSettingsFragment - Carte période cliquée")
            viewModel.editPeriod()
        }

        // Carte Nombre d'événements
        binding.eventsCountCard.setOnClickListener {
            println("🔥 CalendarSettingsFragment - Carte événements cliquée")
            viewModel.editEventsCount()
        }

        // Carte Fréquence de synchronisation
        binding.syncFrequencyCard.setOnClickListener {
            println("🔥 CalendarSettingsFragment - Carte sync cliquée")
            viewModel.editSyncFrequency()
        }

        // Carte Associations d'icônes
        binding.iconAssociationsCard.setOnClickListener {
            println("🔥 CalendarSettingsFragment - Carte icônes cliquée")
            viewModel.editIconAssociations()
        }

        // Bouton Déconnecter Google
        binding.disconnectButton.setOnClickListener {
            println("🔥 CalendarSettingsFragment - Bouton DÉCONNEXION cliqué")
            viewModel.showDisconnectConfirmation()
        }

        println("🔥 CalendarSettingsFragment.setupClickListeners() - FIN")
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

    private fun updateUI(state: CalendarSettingsViewModel.CalendarSettingsUiState) {
        // Carte Compte Google
        binding.googleAccountEmail.text = state.googleAccountEmail
        binding.googleAccountCard.visibility = if (state.googleAccountEmail.isNotEmpty()) View.VISIBLE else View.GONE

        // Carte Calendrier sélectionné
        binding.selectedCalendarName.text = state.selectedCalendarName
        binding.calendarSelectionCard.visibility = if (state.selectedCalendarName.isNotEmpty()) View.VISIBLE else View.GONE

        // Carte Période de récupération
        println("🔥 CalendarSettingsFragment - state.weeksAhead : ${state.weeksAhead}")

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

    private fun handleEvent(event: CalendarSettingsViewModel.CalendarSettingsEvent) {
        println("🔥 CalendarSettingsFragment.handleEvent() - Event reçu: ${event::class.simpleName}")

        when (event) {
            is CalendarSettingsViewModel.CalendarSettingsEvent.ShowDisconnectDialog -> {
                println("🔥 CalendarSettingsFragment.handleEvent() - ShowDisconnectDialog")
                showDisconnectConfirmationDialog()
            }
            is CalendarSettingsViewModel.CalendarSettingsEvent.ShowCalendarSelectionDialog -> {
                println("🔥 CalendarSettingsFragment.handleEvent() - ShowCalendarSelectionDialog")
                showCalendarSelectionDialog()
            }
            is CalendarSettingsViewModel.CalendarSettingsEvent.ShowPeriodDialog -> {
                println("🔥 CalendarSettingsFragment.handleEvent() - ShowPeriodDialog")
                showPeriodDialog()
            }
            is CalendarSettingsViewModel.CalendarSettingsEvent.ShowEventsCountDialog -> {
                println("🔥 CalendarSettingsFragment.handleEvent() - ShowEventsCountDialog")
                showEventsCountDialog()
            }
            is CalendarSettingsViewModel.CalendarSettingsEvent.ShowSyncFrequencyDialog -> {
                println("🔥 CalendarSettingsFragment.handleEvent() - ShowSyncFrequencyDialog")
                showSyncFrequencyDialog()
            }
            is CalendarSettingsViewModel.CalendarSettingsEvent.ShowIconAssociationsDialog -> {
                println("🔥 CalendarSettingsFragment.handleEvent() - ShowIconAssociationsDialog")
                showIconAssociationsDialog()
            }
            is CalendarSettingsViewModel.CalendarSettingsEvent.NavigateBack -> {
                println("🔥 CalendarSettingsFragment.handleEvent() - NavigateBack")
                // Option 1: Essayer l'action spécifique
                try {
                    findNavController().navigate(R.id.action_calendarSettings_to_configuration)
                } catch (e: Exception) {
                    println("🔥 Action spécifique échouée, utilisation navigateUp: ${e.message}")
                    // Option 2: Si l'action n'existe pas, remonter jusqu'à la page 2
                    findNavController().popBackStack(R.id.configurationFragment, false)
                }
            }
            is CalendarSettingsViewModel.CalendarSettingsEvent.ShowMessage -> {
                println("🔥 CalendarSettingsFragment.handleEvent() - ShowMessage: ${event.message}")
                showMessage(event.message)
            }
            is CalendarSettingsViewModel.CalendarSettingsEvent.ShowError -> {
                println("🔥 CalendarSettingsFragment.handleEvent() - ShowError: ${event.message}")
                showError(event.message)
            }
        }
    }

    /**
     * Popup de confirmation de déconnexion Google
     */
    private fun showDisconnectConfirmationDialog() {
        println("🔥 CalendarSettingsFragment.showDisconnectConfirmationDialog() - DÉBUT")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.disconnect_google_title)
            .setMessage(R.string.disconnect_google_message)
            .setPositiveButton(R.string.disconnect) { _, _ ->
                println("🔥 CalendarSettingsFragment.showDisconnectConfirmationDialog() - Bouton DÉCONNECTER cliqué")
                viewModel.disconnectGoogle()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                println("🔥 CalendarSettingsFragment.showDisconnectConfirmationDialog() - Bouton ANNULER cliqué")
                dialog.dismiss()
            }
            .show()

        println("🔥 CalendarSettingsFragment.showDisconnectConfirmationDialog() - Dialog affiché")
    }

    /**
     * Popup de sélection de calendrier
     * TODO: Récupérer la liste des calendriers disponibles via l'API Google
     */
    private fun showCalendarSelectionDialog() {
        // Pour l'instant, popup d'information
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sélection de calendrier")
            .setMessage("Cette fonctionnalité sera implémentée dans une prochaine version.\n\nElle permettra de choisir parmi tous vos calendriers Google disponibles.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Popup de modification de la période de récupération
     */
    private fun showPeriodDialog() {
        val currentWeeks = viewModel.uiState.value.weeksAhead

        // Créer un NumberPicker
        val numberPicker = NumberPicker(requireContext()).apply {
            minValue = 1
            maxValue = 52 // Maximum 1 an
            value = currentWeeks
            wrapSelectorWheel = false
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Période de récupération")
            .setMessage("Choisissez le nombre de semaines dans le futur :")
            .setView(numberPicker)
            .setPositiveButton("Valider") { _, _ ->
                viewModel.updatePeriod(numberPicker.value)
            }
            .setNegativeButton("Annuler") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Popup de modification du nombre d'événements
     */
    private fun showEventsCountDialog() {
        val currentEvents = viewModel.uiState.value.maxEvents
        val options = arrayOf("25", "50", "100", "150", "200")
        val currentIndex = when (currentEvents) {
            25 -> 0
            50 -> 1
            100 -> 2
            150 -> 3
            200 -> 4
            else -> 1 // Par défaut 50
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nombre d'événements maximum")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                val selectedEvents = options[which].toInt()
                viewModel.updateEventsCount(selectedEvents)
                dialog.dismiss()
            }
            .setNegativeButton("Annuler") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Popup de modification de la fréquence de synchronisation
     */
    private fun showSyncFrequencyDialog() {
        val currentFrequency = viewModel.uiState.value.syncFrequencyHours
        val options = arrayOf(
            "Toutes les heures",
            "Toutes les 2 heures",
            "Toutes les 4 heures",
            "Toutes les 8 heures",
            "Une fois par jour"
        )
        val values = arrayOf(1, 2, 4, 8, 24)
        val currentIndex = values.indexOf(currentFrequency).takeIf { it >= 0 } ?: 2

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Fréquence de synchronisation")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                val selectedFrequency = values[which]
                viewModel.updateSyncFrequency(selectedFrequency)
                dialog.dismiss()
            }
            .setNegativeButton("Annuler") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Popup de gestion des associations d'icônes
     * TODO: Implémenter la gestion complète des associations
     */
    private fun showIconAssociationsDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Associations d'icônes")
            .setMessage("Cette fonctionnalité sera implémentée dans une prochaine version.\n\nElle permettra d'associer des mots-clés à des icônes pour personnaliser l'affichage des événements.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showMessage(message: String) {
        androidx.core.content.ContextCompat.getMainExecutor(requireContext()).execute {
            android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun showError(message: String) {
        androidx.core.content.ContextCompat.getMainExecutor(requireContext()).execute {
            android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        println("🔥 CalendarSettingsFragment.onResume() - Rechargement des données")
        // Recharger les données à chaque fois qu'on revient sur cette page
        viewModel.loadSettings()
    }
}