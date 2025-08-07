package com.syniorae.presentation.fragments.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.syniorae.databinding.FragmentHomeBinding
import com.syniorae.presentation.common.NavigationEvent
import com.syniorae.presentation.fragments.home.adapters.TodayEventsAdapter
import com.syniorae.presentation.fragments.home.adapters.FutureEventsAdapter
import kotlinx.coroutines.launch

/**
 * Fragment de la page d'accueil (Page 1)
 * Version complète avec données JSON
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // Adaptateurs
    private lateinit var todayEventsAdapter: TodayEventsAdapter
    private lateinit var futureEventsAdapter: FutureEventsAdapter

    // ViewModel avec factory pour injection de dépendances - CORRIGÉ
    private val viewModel: HomeViewModel by viewModels {
        HomeViewModelFactory()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeViewModel()
    }

    /**
     * Configure l'interface utilisateur
     */
    private fun setupUI() {
        setupSettingsIcon()
        setupAdapters()
        setupSwipeRefresh()
    }

    /**
     * Configure les adaptateurs RecyclerView
     */
    private fun setupAdapters() {
        todayEventsAdapter = TodayEventsAdapter()
        binding.todayEventsList.apply {
            adapter = todayEventsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        futureEventsAdapter = FutureEventsAdapter()
        binding.futureEventsList.apply {
            adapter = futureEventsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    /**
     * Configure l'icône paramètre avec appui long
     */
    private fun setupSettingsIcon() {
        binding.settingsIcon.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    viewModel.onSettingsIconPressed()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    viewModel.onSettingsIconReleased()
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Configure le SwipeRefreshLayout
     */
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshData()
        }
    }

    /**
     * Observe les changements du ViewModel
     */
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            // État de la vue
            viewModel.viewState.collect { state ->
                updateUI(state)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Progression de l'appui long
            viewModel.longPressProgress.collect { progress ->
                updateLongPressProgress(progress)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Navigation
            viewModel.navigationEvent.collect { event ->
                handleNavigationEvent(event)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // État de chargement
            viewModel.isLoading.collect { isLoading ->
                binding.swipeRefresh.isRefreshing = isLoading
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Erreurs
            viewModel.error.collect { error ->
                showError(error)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Messages
            viewModel.message.collect { message ->
                showMessage(message)
            }
        }
    }

    /**
     * Met à jour l'interface utilisateur selon l'état
     */
    private fun updateUI(state: HomeViewState) {
        // Date
        binding.dayOfWeek.text = state.dayOfWeek
        binding.dayOfMonth.text = state.dayOfMonth
        binding.monthYear.text = state.monthYear

        // Colonne droite
        if (state.hasCalendarWidget) {
            if (state.hasTodayEvents()) {
                // Afficher les événements du jour
                binding.rightColumnMessage.visibility = View.GONE
                binding.todayEventsList.visibility = View.VISIBLE
                todayEventsAdapter.submitList(state.todayEvents)
            } else {
                // Afficher le message "Aucun événement"
                binding.rightColumnMessage.visibility = View.VISIBLE
                binding.todayEventsList.visibility = View.GONE
                binding.rightColumnMessage.text = state.getRightColumnMessage()
            }
        } else {
            // Pas de widget calendrier activé - colonnes vides
            binding.rightColumnMessage.visibility = View.GONE
            binding.todayEventsList.visibility = View.GONE
        }

        // Événements futurs
        if (state.hasCalendarWidget) {
            binding.futureEventsSection.visibility = View.VISIBLE
            if (state.hasFutureEvents()) {
                binding.futureEventsTitle.text = "Événements à venir (${state.futureEvents.size})"
                futureEventsAdapter.submitEventsList(state.futureEvents)
            } else {
                binding.futureEventsTitle.text = "Aucun événement à venir"
                futureEventsAdapter.submitEventsList(emptyList())
            }
        } else {
            // Pas de widget calendrier
            binding.futureEventsSection.visibility = View.GONE
        }
    }

    /**
     * Met à jour la progression de l'appui long
     */
    private fun updateLongPressProgress(progress: Float) {
        if (progress > 0f) {
            // Afficher et animer le cercle de progression
            binding.progressCircle.visibility = View.VISIBLE
            binding.progressCircle.setProgress(progress)

            // Légère transparence de l'icône pendant l'appui
            binding.settingsIcon.alpha = 0.7f
        } else {
            // Masquer le cercle et remettre l'icône normale
            binding.progressCircle.visibility = View.INVISIBLE
            binding.progressCircle.reset()
            binding.settingsIcon.alpha = 1f
        }
    }

    /**
     * Gère les événements de navigation
     */
    private fun handleNavigationEvent(event: NavigationEvent) {
        when (event) {
            is NavigationEvent.NavigateToConfiguration -> {
                findNavController().navigate(
                    com.syniorae.R.id.action_home_to_configuration
                )
            }
            else -> {
                // Autres événements de navigation
            }
        }
    }

    /**
     * Affiche un message d'erreur
     */
    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("Réessayer") {
                viewModel.refreshData()
            }
            .show()
    }

    /**
     * Affiche un message d'information
     */
    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        // Rafraîchir les données quand on revient sur la page
        viewModel.refreshData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}