package com.syniorae.presentation.fragments.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.syniorae.databinding.FragmentSettingsBinding

/**
 * Fragment des paramètres détaillés (Page 3)
 * Paramètres spécifiques au widget Calendrier Google
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
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
        setupPlaceholders()
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
     * Configure les éléments temporaires
     */
    private fun setupPlaceholders() {
        binding.placeholderText.text = """
            Cette page contiendra les paramètres détaillés du widget Calendrier Google :
            
            • Calendrier sélectionné
            • Période de récupération (X semaines)  
            • Nombre d'événements max
            • Fréquence de synchronisation
            • Associations d'icônes
            • Compte Google connecté
            
            (À implémenter dans les prochaines versions)
        """.trimIndent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}