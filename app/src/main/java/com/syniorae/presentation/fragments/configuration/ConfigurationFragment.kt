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
 * Version simple sans injection de dépendances
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
        // Bouton retour
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        // Toggle du widget calendrier
        binding.calendarWidgetToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Widget activé - afficher les informations
                binding.calendarWidgetInfo.visibility = View.VISIBLE
                binding.calendarWidgetButtons.visibility = View.VISIBLE

                // Simuler des données
                binding.calendarLastSync.text = "Dernière synchro: il y a 5 min ✓"
                binding.calendarEventsCount.text = "12 événements récupérés"
                binding.calendarSyncStatus.text = "✓ Synchronisé"
                binding.calendarSyncStatus.setTextColor(
                    resources.getColor(android.R.color.holo_green_dark, null)
                )

                showMessage("Widget calendrier activé")
            } else {
                // Widget désactivé
                binding.calendarWidgetInfo.visibility = View.GONE
                binding.calendarWidgetButtons.visibility = View.GONE
                showMessage("Widget calendrier désactivé")
            }
        }

        // Bouton de configuration
        binding.calendarConfigButton.setOnClickListener {
            findNavController().navigate(
                com.syniorae.R.id.action_configuration_to_settings
            )
        }

        // Bouton de synchronisation
        binding.calendarSyncButton.setOnClickListener {
            showMessage("Synchronisation en cours...")
            // TODO: Implémenter la vraie synchronisation plus tard
        }
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