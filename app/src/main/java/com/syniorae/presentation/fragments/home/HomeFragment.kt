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
 * ✅ Version sans fuite mémoire
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // Adaptateurs
    private lateinit var todayEventsAdapter: TodayEventsAdapter
    private lateinit var futureEventsAdapter: FutureEventsAdapter

    // ✅ ViewModel avec factory qui prend le Context
    private val viewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(requireContext().applicationContext)
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

    private fun setupUI() {
        setupSettingsIcon()
        setupAdapters()
        setupSwipeRefresh()
    }

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

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshData()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.viewState.collect { state ->
                updateUI(state)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.longPressProgress.collect { progress ->
                updateLongPressProgress(progress)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.navigationEvent.collect { event ->
                handleNavigationEvent(event)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.swipeRefresh.isRefreshing = isLoading
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                showError(error)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.message.collect { message ->
                showMessage(message)
            }
        }
    }

    private fun updateUI(state: HomeViewState) {
        // Date
        binding.dayOfWeek.text = state.dayOfWeek
        binding.dayOfMonth.text = state.dayOfMonth
        binding.monthYear.text = state.monthYear

        // Colonne droite
        if (state.hasCalendarWidget) {
            if (state.hasTodayEvents()) {
                binding.rightColumnMessage.visibility = View.GONE
                binding.todayEventsList.visibility = View.VISIBLE
                todayEventsAdapter.submitList(state.todayEvents)
            } else {
                binding.rightColumnMessage.visibility = View.VISIBLE
                binding.todayEventsList.visibility = View.GONE
                binding.rightColumnMessage.text = state.getRightColumnMessage()
            }
        } else {
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
            binding.futureEventsSection.visibility = View.GONE
        }
    }

    private fun updateLongPressProgress(progress: Float) {
        if (progress > 0f) {
            binding.progressCircle.visibility = View.VISIBLE
            binding.progressCircle.setProgress(progress)
            binding.settingsIcon.alpha = 0.7f
        } else {
            binding.progressCircle.visibility = View.INVISIBLE
            binding.progressCircle.reset()
            binding.settingsIcon.alpha = 1f
        }
    }

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

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("Réessayer") {
                viewModel.refreshData()
            }
            .show()
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}