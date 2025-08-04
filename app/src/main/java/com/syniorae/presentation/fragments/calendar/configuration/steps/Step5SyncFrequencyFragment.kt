package com.syniorae.presentation.fragments.calendar.configuration.steps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.syniorae.databinding.FragmentStep5SyncFrequencyBinding
import com.syniorae.presentation.fragments.calendar.configuration.CalendarConfigurationViewModel
import kotlinx.coroutines.launch

/**
 * Étape 5 : Fréquence de synchronisation
 */
class Step5SyncFrequencyFragment : Fragment() {

    private var _binding: FragmentStep5SyncFrequencyBinding? = null
    private val binding get() = _binding!!

    private val configViewModel: CalendarConfigurationViewModel by activityViewModels()

    private var selectedFrequency = 4 // Valeur par défaut : 4 heures

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStep5SyncFrequencyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.stepTitle.text = "Étape 5/6 - Fréquence de synchronisation"
        binding.stepDescription.text = "À quelle fréquence voulez-vous synchroniser votre calendrier ?"

        // Configuration des boutons de fréquence
        setupFrequencyButtons()

        // Sélection par défaut
        selectFrequency(4)

        // Boutons de navigation
        binding.nextButton.setOnClickListener {
            configViewModel.nextStep()
        }

        binding.previousButton.setOnClickListener {
            configViewModel.previousStep()
        }
    }

    private fun setupFrequencyButtons() {
        binding.frequency1Hour.setOnClickListener { selectFrequency(1) }
        binding.frequency2Hours.setOnClickListener { selectFrequency(2) }
        binding.frequency4Hours.setOnClickListener { selectFrequency(4) }
        binding.frequency8Hours.setOnClickListener { selectFrequency(8) }
        binding.frequency24Hours.setOnClickListener { selectFrequency(24) }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            configViewModel.configState.collect { state ->
                updateUI(state)
            }
        }
    }

    private fun updateUI(state: com.syniorae.presentation.fragments.calendar.configuration.CalendarConfigState) {
        selectedFrequency = state.syncFrequencyHours
        updateFrequencySelection()
        binding.nextButton.isEnabled = state.syncFrequencyHours > 0
    }

    private fun selectFrequency(hours: Int) {
        selectedFrequency = hours
        configViewModel.setSyncFrequency(hours)
        updateFrequencySelection()
        updateRecommendations(hours)
    }

    private fun updateFrequencySelection() {
        // Reset tous les boutons
        listOf(
            binding.frequency1Hour,
            binding.frequency2Hours,
            binding.frequency4Hours,
            binding.frequency8Hours,
            binding.frequency24Hours
        ).forEach { button ->
            button.alpha = 0.6f
            button.strokeWidth = 1
        }

        // Mettre en évidence le bouton sélectionné
        val selectedButton = when (selectedFrequency) {
            1 -> binding.frequency1Hour
            2 -> binding.frequency2Hours
            4 -> binding.frequency4Hours
            8 -> binding.frequency8Hours
            24 -> binding.frequency24Hours
            else -> binding.frequency4Hours
        }

        selectedButton.alpha = 1.0f
        selectedButton.strokeWidth = 3
    }

    private fun updateRecommendations(hours: Int) {
        val (batteryImpact, dataUsage, recommendation) = when (hours) {
            1 -> Triple(
                "🔋 Impact batterie : Élevé",
                "📊 Consommation data : Élevée",
                "💡 Idéal pour les agendas qui changent souvent"
            )
            2 -> Triple(
                "🔋 Impact batterie : Modéré",
                "📊 Consommation data : Modérée",
                "💡 Bon compromis entre actualité et économie"
            )
            4 -> Triple(
                "🔋 Impact batterie : Faible",
                "📊 Consommation data : Faible",
                "💡 Recommandé pour un usage normal"
            )
            8 -> Triple(
                "🔋 Impact batterie : Très faible",
                "📊 Consommation data : Très faible",
                "💡 Parfait pour les agendas stables"
            )
            24 -> Triple(
                "🔋 Impact batterie : Minimal",
                "📊 Consommation data : Minimale",
                "💡 Pour les calendriers peu changeants"
            )
            else -> Triple("", "", "")
        }

        binding.batteryImpact.text = batteryImpact
        binding.dataUsage.text = dataUsage
        binding.recommendation.text = recommendation
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}