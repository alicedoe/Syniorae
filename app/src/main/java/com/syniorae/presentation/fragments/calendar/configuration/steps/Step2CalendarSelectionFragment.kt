package com.syniorae.presentation.fragments.calendar.configuration.steps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.syniorae.databinding.FragmentStep2CalendarSelectionBinding
import com.syniorae.presentation.fragments.calendar.configuration.CalendarConfigurationViewModel
import com.syniorae.presentation.fragments.calendar.configuration.adapters.CalendarListAdapter
import kotlinx.coroutines.launch

/**
 * Étape 2 : Choix du calendrier
 */
class Step2CalendarSelectionFragment : Fragment() {

    private var _binding: FragmentStep2CalendarSelectionBinding? = null
    private val binding get() = _binding!!

    private val configViewModel: CalendarConfigurationViewModel by activityViewModels()
    private lateinit var calendarAdapter: CalendarListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStep2CalendarSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupRecyclerView()
        observeViewModel()
        loadCalendars()
    }

    private fun setupUI() {
        binding.stepTitle.text = "Étape 2/6 - Choix du calendrier"
        binding.stepDescription.text = "Sélectionnez le calendrier à synchroniser"

        // Bouton suivant
        binding.nextButton.setOnClickListener {
            configViewModel.nextStep()
        }

        // Bouton précédent
        binding.previousButton.setOnClickListener {
            configViewModel.previousStep()
        }
    }

    private fun setupRecyclerView() {
        calendarAdapter = CalendarListAdapter { calendar ->
            configViewModel.selectCalendar(calendar.id, calendar.name)
        }

        binding.calendarsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = calendarAdapter
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
        binding.nextButton.isEnabled = state.selectedCalendarId.isNotBlank()

        if (state.selectedCalendarId.isNotBlank()) {
            binding.selectedCalendarInfo.visibility = View.VISIBLE
            binding.selectedCalendarName.text = "Calendrier sélectionné : ${state.selectedCalendarName}"
        } else {
            binding.selectedCalendarInfo.visibility = View.GONE
        }
    }

    /**
     * Simule le chargement des calendriers Google
     */
    private fun loadCalendars() {
        // TODO: Remplacer par vraie API Google Calendar
        val fakeCalendars = listOf(
            CalendarItem("1", "Principal", "Mon calendrier principal", false),
            CalendarItem("2", "Travail", "Calendrier professionnel", false),
            CalendarItem("3", "Famille", "Événements familiaux", true),
            CalendarItem("4", "Anniversaires", "Calendrier partagé des anniversaires", true)
        )

        calendarAdapter.submitList(fakeCalendars)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * Modèle pour représenter un calendrier
 */
data class CalendarItem(
    val id: String,
    val name: String,
    val description: String,
    val isShared: Boolean
)