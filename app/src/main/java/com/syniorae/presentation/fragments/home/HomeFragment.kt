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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Fragment de la page d'accueil (Page 1)
 * Version temporaire sans ViewModel complet en attendant la mise en place de l'injection de dépendances
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

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
        updateUI()
    }

    /**
     * Configure l'interface utilisateur
     */
    private fun setupUI() {
        setupSettingsIcon()
    }

    /**
     * Configure l'icône paramètre avec appui long
     */
    private fun setupSettingsIcon() {
        binding.settingsIcon.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    onSettingsIconPressed()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    onSettingsIconReleased()
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Met à jour la date actuelle
     */
    private fun updateCurrentDate() {
        val now = LocalDateTime.now()
        val dayOfWeek = now.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()))
        val dayOfMonth = now.format(DateTimeFormatter.ofPattern("d"))
        val month = now.format(DateTimeFormatter.ofPattern("MMMM"))
        val year = now.format(DateTimeFormatter.ofPattern("yyyy"))

        binding.dayOfWeek.text = dayOfWeek.replaceFirstChar { it.uppercase() }
        binding.dayOfMonth.text = dayOfMonth
        binding.monthYear.text = "$month $year"
    }

    /**
     * Met à jour l'interface utilisateur
     */
    private fun updateUI() {
        // Pour l'instant, pas de widget calendrier activé
        val hasCalendarWidget = false
        val hasTodayEvents = false
        val hasFutureEvents = false

        // Colonne droite
        if (hasCalendarWidget) {
            if (hasTodayEvents) {
                // Afficher les événements du jour
                binding.rightColumnMessage.visibility = View.GONE
                binding.todayEventsList.visibility = View.VISIBLE
            } else {
                // Afficher le message "Aucun événement"
                binding.rightColumnMessage.visibility = View.VISIBLE
                binding.todayEventsList.visibility = View.GONE
                binding.rightColumnMessage.text = "Aucun événement de prévu"
            }
        } else {
            // Pas de widget calendrier activé
            binding.rightColumnMessage.visibility = View.GONE
            binding.todayEventsList.visibility = View.GONE
        }

        // Événements futurs
        if (hasFutureEvents) {
            binding.futureEventsSection.visibility = View.VISIBLE
        } else {
            binding.futureEventsSection.visibility = View.GONE
        }
    }

    /**
     * Gère le début de l'appui long sur l'icône paramètre
     */
    private fun onSettingsIconPressed() {
        if (isLongPressing) return

        isLongPressing = true

        // TODO: Implémenter l'animation de progression
        binding.settingsIcon.alpha = 0.5f

        // Simulation d'un appui long de 1 seconde
        binding.settingsIcon.postDelayed({
            if (isLongPressing) {
                navigateToConfiguration()
            }
            resetLongPress()
        }, 1000L)
    }

    /**
     * Gère le relâchement de l'appui long
     */
    private fun onSettingsIconReleased() {
        isLongPressing = false
        resetLongPress()
    }

    /**
     * Remet l'icône à son état normal
     */
    private fun resetLongPress() {
        binding.settingsIcon.alpha = 1f
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
            showError("Impossible d'accéder à la configuration")
        }
    }

    /**
     * Affiche un message d'erreur
     */
    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("OK") { }
            .show()
    }

    /**
     * Affiche un message d'information
     */
    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}