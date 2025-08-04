package com.syniorae.presentation.fragments.configuration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.syniorae.databinding.FragmentConfigurationBinding

/**
 * Fragment de la page de configuration des widgets (Page 2)
 * Permet d'activer/désactiver les widgets et d'accéder à leurs paramètres
 * Version temporaire sans ViewModel complet en attendant la mise en place de l'injection de dépendances
 */
class ConfigurationFragment : Fragment() {

    private var _binding: FragmentConfigurationBinding? = null
    private val binding get() = _binding!!

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
            toggleCalendarWidget(isChecked)
        }

        // Bouton de configuration
        binding.calendarConfigButton.setOnClickListener {
            navigateToCalendarSettings()
        }

        // Bouton de synchronisation
        binding.calendarSyncButton.setOnClickListener {
            syncCalendar()
        }

        // État initial - widget désactivé
        updateCalendarWidgetUI(false)
    }

    /**
     * Active/désactive le widget calendrier
     */
    private fun toggleCalendarWidget(isEnabled: Boolean) {
        if (isEnabled) {
            // TODO: Vérifier si le widget a sa configuration
            // Pour l'instant, on simule une activation
            showMessage("Widget calendrier activé")
            updateCalendarWidgetUI(true)
        } else {
            showMessage("Widget calendrier désactivé")
            updateCalendarWidgetUI(false)
        }
    }

    /**
     * Navigue vers les paramètres détaillés du calendrier
     */
    private fun navigateToCalendarSettings() {
        findNavController().navigate(
            com.syniorae.R.id.action_configuration_to_settings
        )
    }

    /**
     * Lance la synchronisation du calendrier
     */
    private fun syncCalendar() {
        // TODO: Implémenter la synchronisation réelle
        showMessage("Synchronisation en cours...")
    }

    /**
     * Met à jour l'interface du widget calendrier
     */
    private fun updateCalendarWidgetUI(isEnabled: Boolean) {
        binding.calendarWidgetToggle.isChecked = isEnabled

        if (isEnabled) {
            // Widget activé - afficher les informations
            binding.calendarWidgetInfo.visibility = View.VISIBLE
            binding.calendarWidgetButtons.visibility = View.VISIBLE

            // Informations simulées
            binding.calendarLastSync.text = "Dernière synchro : Jamais"
            binding.calendarEventsCount.text = "0 événements récupérés"
            binding.calendarSyncStatus.text = "✓ OK"
            binding.calendarSyncStatus.setTextColor(
                resources.getColor(android.R.color.holo_green_dark, null)
            )
        } else {
            // Widget désactivé - masquer les informations
            binding.calendarWidgetInfo.visibility = View.GONE
            binding.calendarWidgetButtons.visibility = View.GONE
        }
    }

    /**
     * Affiche un message d'information
     */
    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    /**
     * Affiche un message d'erreur
     */
    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("OK") { }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}