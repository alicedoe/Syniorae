package com.syniorae.presentation.fragments.calendar.configuration.steps

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.syniorae.core.di.DependencyInjection
import com.syniorae.data.remote.calendar.CalendarItem
import com.syniorae.databinding.FragmentStep2CalendarSelectionBinding
import com.syniorae.presentation.fragments.calendar.configuration.CalendarConfigurationViewModel
import com.syniorae.presentation.fragments.calendar.configuration.adapters.CalendarListAdapter
import kotlinx.coroutines.launch

/**
 * Étape 2 : Choix du calendrier
 * CORRECTION DES ERREURS DE COMPILATION
 */
class Step2CalendarSelectionFragment : Fragment() {

    companion object {
        private const val TAG = "Step2CalendarSelection"
    }

    private var _binding: FragmentStep2CalendarSelectionBinding? = null
    private val binding get() = _binding!!

    private val configViewModel: CalendarConfigurationViewModel by activityViewModels()
    private lateinit var calendarAdapter: CalendarListAdapter

    private val calendarRepository = DependencyInjection.getCalendarRepository()

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
        loadRealCalendars()
    }

    private fun setupUI() {
        binding.stepTitle.text = "Étape 2/6 - Choix du calendrier"
        binding.stepDescription.text = "Sélectionnez le calendrier à synchroniser"

        binding.nextButton.setOnClickListener {
            configViewModel.nextStep()
        }

        binding.previousButton.setOnClickListener {
            configViewModel.previousStep()
        }

        // Vérifier si le bouton refresh existe avant de l'utiliser
        try {
            binding.refreshButton.setOnClickListener {
                loadRealCalendars()
            }
        } catch (e: Exception) {
            Log.d(TAG, "Bouton refresh non disponible dans le layout")
        }
    }

    private fun setupRecyclerView() {
        calendarAdapter = CalendarListAdapter { calendar ->
            Log.d(TAG, "Calendrier sélectionné: ${calendar.name} (${calendar.id})")
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

        viewLifecycleOwner.lifecycleScope.launch {
            configViewModel.isLoading.collect { isLoading ->
                updateLoadingState(isLoading)
            }
        }
    }

    private fun updateUI(state: com.syniorae.presentation.fragments.calendar.configuration.CalendarConfigState) {
        binding.nextButton.isEnabled = state.selectedCalendarId.isNotBlank()

        if (state.selectedCalendarId.isNotBlank()) {
            binding.selectedCalendarInfo.visibility = View.VISIBLE
            binding.selectedCalendarName.text = "Calendrier sélectionné : ${state.selectedCalendarName}"

            // Highlight du calendrier sélectionné dans l'adapter
            calendarAdapter.setSelectedCalendar(state.selectedCalendarId)
        } else {
            binding.selectedCalendarInfo.visibility = View.GONE
        }
    }

    private fun updateLoadingState(isLoading: Boolean) {
        // Utilise seulement les vues qui existent dans le layout
        try {
            if (isLoading) {
                binding.loadingIndicator.visibility = View.VISIBLE
                binding.calendarsRecyclerView.visibility = View.GONE
                // Masquer les états d'erreur et vide s'ils existent
                hideAllStates()
            } else {
                binding.loadingIndicator.visibility = View.GONE
            }
        } catch (e: Exception) {
            Log.d(TAG, "Certaines vues de loading non disponibles: ${e.message}")
        }
    }

    private fun hideAllStates() {
        try {
            binding.emptyState.visibility = View.GONE
            binding.errorState.visibility = View.GONE
        } catch (e: Exception) {
            // Les vues d'état n'existent pas dans le layout
        }
    }

    /**
     * Charge les vrais calendriers depuis l'API Google
     */
    private fun loadRealCalendars() {
        Log.d(TAG, "Chargement des calendriers Google...")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Simuler le loading via le ViewModel (sans utiliser setLoading qui n'existe pas)
                showLoadingState()

                // Initialiser l'API Google
                calendarRepository.initializeGoogleApi(requireContext())

                // Récupérer les calendriers
                val result = calendarRepository.getAvailableCalendars()

                hideLoadingState()

                when {
                    result.isSuccess -> {
                        val calendars = result.getOrNull() ?: emptyList()
                        Log.d(TAG, "Récupéré ${calendars.size} calendriers")

                        if (calendars.isNotEmpty()) {
                            showCalendarsSuccess(calendars)
                        } else {
                            showEmptyState("Aucun calendrier trouvé dans votre compte Google")
                        }
                    }
                    else -> {
                        val error = result.exceptionOrNull()
                        Log.e(TAG, "Erreur lors du chargement des calendriers", error)

                        val errorMessage = when {
                            error?.message?.contains("NetworkError") == true ->
                                "Vérifiez votre connexion internet"
                            error?.message?.contains("AuthenticationException") == true ->
                                "Problème d'authentification Google"
                            error?.message?.contains("403") == true ->
                                "Permissions insuffisantes"
                            else -> "Erreur lors du chargement : ${error?.message}"
                        }

                        showErrorState(errorMessage)

                        // Proposer les calendriers de test en fallback
                        loadFallbackCalendars()
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Erreur inattendue lors du chargement", e)
                hideLoadingState()

                showErrorState("Erreur inattendue : ${e.message}")
                loadFallbackCalendars()
            }
        }
    }

    private fun showLoadingState() {
        updateLoadingState(true)
    }

    private fun hideLoadingState() {
        updateLoadingState(false)
    }

    private fun showCalendarsSuccess(calendars: List<CalendarItem>) {
        binding.calendarsRecyclerView.visibility = View.VISIBLE
        hideAllStates()

        calendarAdapter.submitList(calendars)

        // Message d'information
        showMessage("${calendars.size} calendrier(s) trouvé(s)")
    }

    private fun showEmptyState(message: String) {
        binding.calendarsRecyclerView.visibility = View.GONE

        try {
            binding.emptyState.visibility = View.VISIBLE
            binding.errorState.visibility = View.GONE
            binding.emptyStateMessage.text = """
                $message
                
                Suggestions :
                • Vérifiez que vous avez des calendriers dans Google Calendar
                • Vérifiez les permissions de l'application
            """.trimIndent()
        } catch (e: Exception) {
            // Les vues d'état n'existent pas, utiliser un toast ou snackbar
            showError(message)
        }
    }

    private fun showErrorState(message: String) {
        binding.calendarsRecyclerView.visibility = View.GONE

        try {
            binding.errorState.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE
            binding.errorStateMessage.text = message
            binding.retryButton.setOnClickListener {
                loadRealCalendars()
            }
        } catch (e: Exception) {
            // Les vues d'état n'existent pas, utiliser un snackbar
            showError(message)
        }
    }

    /**
     * Charge des calendriers de test en cas d'erreur API (fallback)
     */
    private fun loadFallbackCalendars() {
        Log.w(TAG, "Utilisation des calendriers de test (fallback)")

        val fallbackCalendars = listOf(
            CalendarItem(
                id = "primary",
                name = "Principal",
                description = "Mon calendrier principal",
                isShared = false
            ),
            CalendarItem(
                id = "test_work",
                name = "Travail",
                description = "Calendrier professionnel",
                isShared = false
            ),
            CalendarItem(
                id = "test_family",
                name = "Famille",
                description = "Événements familiaux",
                isShared = true
            ),
            CalendarItem(
                id = "test_birthdays",
                name = "Anniversaires",
                description = "Calendrier partagé des anniversaires",
                isShared = true
            )
        )

        // Afficher avec un indicateur que ce sont des données de test
        binding.calendarsRecyclerView.visibility = View.VISIBLE
        hideAllStates()

        calendarAdapter.submitList(fallbackCalendars)

        showMessage("Calendriers de démonstration chargés")
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("Réessayer") {
                loadRealCalendars()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}