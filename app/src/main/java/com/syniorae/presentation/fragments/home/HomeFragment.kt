package com.syniorae.presentation.fragments.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.syniorae.databinding.FragmentHomeBinding
import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Fragment pour la page d'accueil (Page 1)
 * Version simple avec appui long basique
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var longPressJob: Job? = null
    private var isLongPressing = false

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
        updateCurrentDate()
    }

    /**
     * Configure l'interface utilisateur
     */
    private fun setupUI() {
        setupSettingsIcon()
        setupSwipeRefresh()

        // Message par défaut dans la colonne droite
        binding.rightColumnMessage.visibility = View.VISIBLE
        binding.rightColumnMessage.text = "Activez le widget calendrier pour voir vos événements"
    }

    /**
     * Met à jour la date actuelle
     */
    private fun updateCurrentDate() {
        val now = LocalDateTime.now()

        binding.dayOfWeek.text = now.format(DateTimeFormatter.ofPattern("EEEE", Locale.FRENCH))
            .replaceFirstChar { it.uppercase() }
        binding.dayOfMonth.text = now.format(DateTimeFormatter.ofPattern("d"))
        binding.monthYear.text = now.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH))
    }

    /**
     * Configure l'icône paramètre avec appui long
     */
    private fun setupSettingsIcon() {
        binding.settingsIcon.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startLongPress()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    stopLongPress()
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Démarre l'appui long
     */
    private fun startLongPress() {
        if (isLongPressing) return

        isLongPressing = true
        longPressJob = CoroutineScope(Dispatchers.Main).launch {
            var progress = 0f

            while (progress < 1f && isLongPressing) {
                progress += 0.02f // 50 étapes pour 1 seconde

                // Effet visuel
                binding.settingsIcon.alpha = 0.3f + (progress * 0.7f)
                val scale = 1f + (progress * 0.1f)
                binding.settingsIcon.scaleX = scale
                binding.settingsIcon.scaleY = scale

                delay(20) // 50 fps
            }

            if (isLongPressing) {
                // Appui complet - naviguer vers la configuration
                navigateToConfiguration()
            }
        }
    }

    /**
     * Arrête l'appui long
     */
    private fun stopLongPress() {
        isLongPressing = false
        longPressJob?.cancel()

        // Remettre l'icône normale
        binding.settingsIcon.alpha = 1f
        binding.settingsIcon.scaleX = 1f
        binding.settingsIcon.scaleY = 1f
    }

    /**
     * Navigue vers la page de configuration
     */
    private fun navigateToConfiguration() {
        try {
            findNavController().navigate(
                com.syniorae.R.id.action_home_to_configuration
            )
        } catch (e: Exception) {
            showMessage("Navigation vers configuration")
        }
    }

    /**
     * Configure le swipe refresh
     */
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            // Simuler un refresh
            CoroutineScope(Dispatchers.Main).launch {
                delay(1000)
                binding.swipeRefresh.isRefreshing = false
                updateCurrentDate()
                showMessage("Données mises à jour")
            }
        }
    }

    /**
     * Affiche un message
     */
    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        longPressJob?.cancel()
        super.onDestroyView()
        _binding = null
    }
}