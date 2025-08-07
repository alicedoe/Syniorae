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
     * Charge les calendriers depuis l'API Google
     */
    private fun loadCalendars() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                binding.calendarsRecyclerView.visibility = View.GONE
                // TODO: Ajouter un indicateur de chargement
                val googleCalendarApi = com.syniorae.core.di.DependencyInjection.getGoogleCalendarApi()
                val googleCalendars = googleCalendarApi.getCalendarList()

                // Convertir les calendriers Google en CalendarItem
                val calendarItems = googleCalendars.map { googleCalendar ->
                    CalendarItem(
                        id = googleCalendar.id,
                        name = googleCalendar.name,
                        description = googleCalendar.description,
                        isShared = googleCalendar.isShared
                    )
                }

                calendarAdapter.submitList(calendarItems)
                binding.calendarsRecyclerView.visibility = View.VISIBLE

            } catch (e: Exception) {
                // En cas d'erreur, afficher les calendriers de test
                showError("Erreur lors du chargement: ${e.message}")
                loadFallbackCalendars()
            }
        }
    }

    /**
     * Affiche des calendriers de secours en cas d'erreur API
     */
    private fun loadFallbackCalendars() {
        val fallbackCalendars = listOf(
            CalendarItem("primary", "Principal", "Mon calendrier principal", false),
            CalendarItem("work", "Travail", "Calendrier professionnel", false),
            CalendarItem("family", "Famille", "Événements familiaux", true)
        )

        calendarAdapter.submitList(fallbackCalendars)
        binding.calendarsRecyclerView.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        // TODO: Afficher une Snackbar ou un Toast
        android.util.Log.e("CalendarSelection", message)
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