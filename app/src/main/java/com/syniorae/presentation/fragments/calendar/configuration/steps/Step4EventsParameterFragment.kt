package com.syniorae.presentation.fragments.calendar.configuration.steps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.syniorae.databinding.FragmentStep4EventsParameterBinding
import com.syniorae.presentation.fragments.calendar.configuration.CalendarConfigurationViewModel
import kotlinx.coroutines.launch

/**
 * Étape 4 : Nombre d'événements maximum
 */
class Step4EventsParameterFragment : Fragment() {

    private var _binding: FragmentStep4EventsParameterBinding? = null
    private val binding get() = _binding!!

    private val configViewModel: CalendarConfigurationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStep4EventsParameterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.stepTitle.text = "Étape 4/6 - Nombre d'événements"
        binding.stepDescription.text = "Combien d'événements maximum voulez-vous récupérer ?"

        // Configuration du SeekBar
        binding.eventsSeekBar.min = 10
        binding.eventsSeekBar.max = 200
        binding.eventsSeekBar.progress = 50 // Valeur par défaut

        updateEventsDisplay(50)

        binding.eventsSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    updateEventsDisplay(progress)
                    configViewModel.setMaxEvents(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Boutons prédéfinis
        binding.preset25Events.setOnClickListener { setEvents(25) }
        binding.preset50Events.setOnClickListener { setEvents(50) }
        binding.preset100Events.setOnClickListener { setEvents(100) }
        binding.preset200Events.setOnClickListener { setEvents(200) }

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
        binding.eventsSeekBar.progress = state.maxEvents
        updateEventsDisplay(state.maxEvents)
        binding.nextButton.isEnabled = state.maxEvents > 0
    }

    private fun setEvents(events: Int) {
        binding.eventsSeekBar.progress = events
        updateEventsDisplay(events)
        configViewModel.setMaxEvents(events)
    }

    private fun updateEventsDisplay(events: Int) {
        binding.eventsValue.text = "$events"

        // Explication selon le nombre
        val explanation = when {
            events <= 25 -> "Idéal pour un agenda léger"
            events <= 50 -> "Parfait pour un usage normal"
            events <= 100 -> "Convient pour un agenda chargé"
            else -> "Pour les agendas très denses"
        }
        binding.eventsExplanation.text = explanation

        // Impact sur les performances
        val performanceNote = when {
            events <= 50 -> "Synchronisation rapide"
            events <= 100 -> "Synchronisation normale"
            else -> "Synchronisation plus lente"
        }
        binding.performanceNote.text = "⚡ $performanceNote"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}