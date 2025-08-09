package com.syniorae.presentation.fragments.calendar.configuration.steps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.syniorae.databinding.FragmentStep2CalendarSelectionBinding
import com.syniorae.presentation.fragments.calendar.configuration.CalendarConfigurationViewModel
import com.syniorae.presentation.fragments.calendar.configuration.adapters.CalendarListAdapter
import com.syniorae.data.remote.google.GoogleCalendarInfo
import com.syniorae.data.local.preferences.CalendarPreferenceManager
import kotlinx.coroutines.launch

/**
 * Étape 2 : Choix du calendrier RÉEL avec sauvegarde
 * Récupère les vrais calendriers Google et sauvegarde le choix
 */
class Step2CalendarSelectionFragment : Fragment() {

    private var _binding: FragmentStep2CalendarSelectionBinding? = null
    private val binding get() = _binding!!

    private val configViewModel: CalendarConfigurationViewModel by activityViewModels()
    private lateinit var calendarAdapter: CalendarListAdapter
    private lateinit var preferenceManager: CalendarPreferenceManager

    // État de chargement
    private var isLoading = false
    private var hasError = false

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

        // Initialiser le preference manager
        preferenceManager = CalendarPreferenceManager(requireContext())

        setupUI()
        setupRecyclerView()
        observeViewModel()
        loadCalendarsWithFallback()
    }

    private fun setupUI() {
        binding.stepTitle.text = "Étape 2/6 - Choix du calendrier"
        binding.stepDescription.text = "Sélectionnez le calendrier à synchroniser"

        // Bouton suivant (désactivé par défaut)
        binding.nextButton.isEnabled = false
        binding.nextButton.setOnClickListener {
            if (!isLoading) {
                configViewModel.nextStep()
            }
        }

        // Bouton précédent
        binding.previousButton.setOnClickListener {
            configViewModel.previousStep()
        }

        // Bouton actualiser (en cas d'erreur)
        binding.refreshButton?.setOnClickListener {
            loadCalendarsWithFallback()
        }
    }

    private fun setupRecyclerView() {
        calendarAdapter = CalendarListAdapter { calendar ->
            onCalendarSelected(calendar)
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
        // Activer le bouton suivant seulement si un calendrier est sélectionné
        binding.nextButton.isEnabled = state.selectedCalendarId.isNotBlank() && !isLoading

        // Afficher les informations du calendrier sélectionné
        if (state.selectedCalendarId.isNotBlank()) {
            binding.selectedCalendarInfo?.visibility = View.VISIBLE
            binding.selectedCalendarName?.text = "✓ ${state.selectedCalendarName}"
        } else {
            binding.selectedCalendarInfo?.visibility = View.GONE
        }
    }

    /**
     * Charge les calendriers avec gestion d'erreur et fallback
     */
    private fun loadCalendarsWithFallback() {
        viewLifecycleOwner.lifecycleScope.launch {
            setLoadingState(true)

            try {
                // D'abord, essayer de charger depuis le cache
                val cachedCalendars = preferenceManager.getAvailableCalendars()
                if (cachedCalendars.isNotEmpty()) {
                    displayCalendars(cachedCalendars, fromCache = true)
                }

                // Ensuite, essayer de récupérer les calendriers réels
                loadRealCalendars()

            } catch (e: Exception) {
                handleLoadingError("Erreur initialisation: ${e.message}")
            }
        }
    }

    /**
     * Charge les calendriers RÉELS depuis l'API Google
     */
    private suspend fun loadRealCalendars() {
        try {
            val googleCalendarApi = com.syniorae.core.di.DependencyInjection.getGoogleCalendarApi()

            // Vérifier d'abord si l'utilisateur est connecté
            val authManager = com.syniorae.core.di.DependencyInjection.getGoogleAuthManager()
            if (!authManager.isSignedIn()) {
                throw Exception("Utilisateur non connecté à Google")
            }

            // Récupérer les calendriers réels
            val realCalendars = googleCalendarApi.getCalendarList()

            if (realCalendars.isNotEmpty()) {
                // Sauvegarder en cache pour usage futur
                preferenceManager.saveAvailableCalendars(realCalendars)

                // Afficher les calendriers réels
                displayCalendars(realCalendars, fromCache = false)

                // Restaurer la sélection précédente si elle existe
                restorePreviousSelection(realCalendars)

                setLoadingState(false)
                hasError = false

                showSuccess("${realCalendars.size} calendriers chargés")

            } else {
                throw Exception("Aucun calendrier trouvé")
            }

        } catch (e: Exception) {
            android.util.Log.e("CalendarSelection", "Erreur chargement calendriers réels", e)

            // En cas d'erreur, essayer de charger les calendriers de fallback
            val cachedCalendars = preferenceManager.getAvailableCalendars()
            if (cachedCalendars.isEmpty()) {
                loadFallbackCalendars()
            }

            handleLoadingError("Erreur API: ${e.message}")
        }
    }

    /**
     * Affiche la liste des calendriers
     */
    private fun displayCalendars(calendars: List<GoogleCalendarInfo>, fromCache: Boolean) {
        val calendarItems = calendars.map { googleCalendar ->
            CalendarItem(
                id = googleCalendar.id,
                name = googleCalendar.name,
                description = googleCalendar.description,
                isShared = googleCalendar.isShared,
                backgroundColor = googleCalendar.backgroundColor
            )
        }

        calendarAdapter.submitList(calendarItems)
        binding.calendarsRecyclerView.visibility = View.VISIBLE

        // CORRECTION: Utiliser les éléments qui existent dans le layout
        // binding.cacheIndicator?.visibility = if (fromCache) View.VISIBLE else View.GONE
        // REMPLACER PAR : (commenté car élément inexistant)
        // if (fromCache) showInfo("Données depuis le cache")
    }

    /**
     * Restaure la sélection précédente du calendrier
     */
    private suspend fun restorePreviousSelection(calendars: List<GoogleCalendarInfo>) {
        try {
            val previousSelection = preferenceManager.getSelectedCalendar()
            if (previousSelection != null) {
                // Vérifier que le calendrier sélectionné existe toujours
                val stillExists = calendars.any { it.id == previousSelection.id }
                if (stillExists) {
                    configViewModel.selectCalendar(previousSelection.id, previousSelection.name)
                    calendarAdapter.setSelectedCalendarId(previousSelection.id)

                    showInfo("Calendrier précédent restauré: ${previousSelection.name}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("CalendarSelection", "Erreur restauration sélection", e)
        }
    }

    /**
     * Charge des calendriers de secours en cas d'erreur API
     */
    private fun loadFallbackCalendars() {
        val fallbackCalendars = listOf(
            GoogleCalendarInfo("primary", "Principal", "Mon calendrier principal", false, "#1976d2"),
            GoogleCalendarInfo("work", "Travail", "Calendrier professionnel", false, "#388e3c"),
            GoogleCalendarInfo("family", "Famille", "Événements familiaux", true, "#f57c00")
        )

        displayCalendars(fallbackCalendars, fromCache = false)
        setLoadingState(false)

        showWarning("Calendriers de test chargés (mode hors ligne)")
    }

    /**
     * Gère la sélection d'un calendrier
     */
    private fun onCalendarSelected(calendar: CalendarItem) {
        if (isLoading) return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Convertir CalendarItem en GoogleCalendarInfo
                val googleCalendarInfo = GoogleCalendarInfo(
                    id = calendar.id,
                    name = calendar.name,
                    description = calendar.description,
                    isShared = calendar.isShared,
                    backgroundColor = calendar.backgroundColor
                )

                // Sauvegarder le choix
                preferenceManager.saveSelectedCalendar(googleCalendarInfo)

                // Mettre à jour le ViewModel
                configViewModel.selectCalendar(calendar.id, calendar.name)

                // Mettre à jour l'affichage
                calendarAdapter.setSelectedCalendarId(calendar.id)

                showSuccess("Calendrier sélectionné: ${calendar.name}")

            } catch (e: Exception) {
                android.util.Log.e("CalendarSelection", "Erreur sauvegarde calendrier", e)
                showError("Erreur lors de la sauvegarde")
            }
        }
    }

    /**
     * Gère l'état de chargement
     */
    private fun setLoadingState(loading: Boolean) {
        isLoading = loading

        binding.loadingIndicator?.visibility = if (loading) View.VISIBLE else View.GONE
        binding.nextButton.isEnabled = !loading && configViewModel.configState.value.selectedCalendarId.isNotBlank()
        binding.refreshButton?.isEnabled = !loading
    }

    /**
     * Gère les erreurs de chargement
     */
    private fun handleLoadingError(message: String) {
        hasError = true
        setLoadingState(false)

        // CORRECTION: Utiliser errorState au lieu de errorMessage
        binding.errorState.visibility = View.VISIBLE
        binding.errorStateMessage.text = message

        binding.refreshButton.visibility = View.VISIBLE

        showError(message)
    }

    // Méthodes d'affichage des messages
    private fun showSuccess(message: String) {
        Toast.makeText(requireContext(), "✓ $message", Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), "✗ $message", Toast.LENGTH_LONG).show()
    }

    private fun showWarning(message: String) {
        Toast.makeText(requireContext(), "⚠ $message", Toast.LENGTH_LONG).show()
    }

    private fun showInfo(message: String) {
        Toast.makeText(requireContext(), "ℹ $message", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * Modèle pour représenter un calendrier dans l'UI
 */
data class CalendarItem(
    val id: String,
    val name: String,
    val description: String,
    val isShared: Boolean,
    val backgroundColor: String = "#1976d2"
)