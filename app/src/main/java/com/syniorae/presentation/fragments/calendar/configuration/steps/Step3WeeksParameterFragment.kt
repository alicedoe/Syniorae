package com.syniorae.presentation.fragments.calendar.configuration.steps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.syniorae.databinding.FragmentStep3WeeksParameterBinding
import com.syniorae.presentation.fragments.calendar.configuration.CalendarConfigurationViewModel
import kotlinx.coroutines.launch

/**
 * Étape 3 : Période de récupération (nombre de semaines)
 */
class Step3WeeksParameterFragment : Fragment() {

    private var _binding: FragmentStep3WeeksParameterBinding? = null
    private val binding get() = _binding!!

    private val configViewModel: CalendarConfigurationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStep3WeeksParameterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.stepTitle.text = "Étape 3/6 - Période de récupération"
        binding.stepDescription.text = "Combien de semaines dans le futur voulez-vous récupérer ?"

        // Configuration du SeekBar
        binding.weeksSeekBar.min = 1
        binding.weeksSeekBar.max = 12
        binding.weeksSeekBar.progress = 4 // Valeur par défaut

        updateWeeksDisplay(4)

        binding.weeksSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    updateWeeksDisplay(progress)
                    configViewModel.setWeeksAhead(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Boutons prédéfinis
        binding.preset2Weeks.setOnClickListener { setWeeks(2) }
        binding.preset4Weeks.setOnClickListener { setWeeks(4) }
        binding.preset8Weeks.setOnClickListener { setWeeks(8) }
        binding.preset12Weeks.setOnClickListener { setWeeks(12) }

        // Boutons de navigation
        binding.nextButton.setOnClickListener {
            configViewModel.nextStep()
        }

        binding.previousButton.setOnClickListener {
            configViewModel.previousStep()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            configViewModel.configState.collect { state ->
                updateUI(state)
            }
        }
    }

    private fun updateUI(state: com.syniorae.presentation.fragments.calendar.configuration.CalendarConfigState) {
        binding.weeksSeekBar.progress = state.weeksAhead
        updateWeeksDisplay(state.weeksAhead)
        binding.nextButton.isEnabled = state.weeksAhead > 0
    }

    private fun setWeeks(weeks: Int) {
        binding.weeksSeekBar.progress = weeks
        updateWeeksDisplay(weeks)
        configViewModel.setWeeksAhead(weeks)
    }

    private fun updateWeeksDisplay(weeks: Int) {
        binding.weeksValue.text = "$weeks"
        binding.weeksLabel.text = if (weeks == 1) "semaine" else "semaines"

        // Explication de la durée
        val explanation = when {
            weeks <= 2 -> "Idéal pour les événements immédiats"
            weeks <= 4 -> "Parfait pour un aperçu mensuel"
            weeks <= 8 -> "Bon pour la planification à moyen terme"
            else -> "Planification à long terme"
        }
        binding.weeksExplanation.text = explanation
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}