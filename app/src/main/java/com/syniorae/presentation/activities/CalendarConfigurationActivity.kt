package com.syniorae.presentation.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.syniorae.R
import com.syniorae.databinding.ActivityCalendarConfigurationBinding
import com.syniorae.presentation.fragments.calendar.configuration.CalendarConfigurationViewModel
import com.syniorae.presentation.fragments.calendar.configuration.ConfigNavigationEvent
import com.syniorae.presentation.fragments.calendar.configuration.steps.*
import kotlinx.coroutines.launch

/**
 * Activité pour le tunnel de configuration du calendrier Google
 * Gère la navigation entre les 6 étapes
 */
class CalendarConfigurationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalendarConfigurationBinding
    private val configViewModel: CalendarConfigurationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCalendarConfigurationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        observeViewModel()

        // Commencer par l'étape 1
        if (savedInstanceState == null) {
            navigateToStep(1)
        }
    }

    private fun setupNavigation() {
        // Configuration de base
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            configViewModel.navigationEvent.collect { event ->
                when (event) {
                    is ConfigNavigationEvent.NavigateToStep -> {
                        navigateToStep(event.step)
                    }
                    is ConfigNavigationEvent.ConfigurationComplete -> {
                        finishConfiguration()
                    }
                    is ConfigNavigationEvent.ConfigurationCancelled -> {
                        cancelConfiguration()
                    }
                }
            }
        }
    }

    private fun navigateToStep(step: Int) {
        val fragment = when (step) {
            1 -> Step1GoogleAuthFragment()
            2 -> Step2CalendarSelectionFragment()
            3 -> Step3WeeksParameterFragment()
            4 -> Step4EventsParameterFragment()
            5 -> Step5SyncFrequencyFragment()
            6 -> Step6IconAssociationsFragment()
            else -> return
        }

        replaceFragment(fragment)
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun finishConfiguration() {
        // Configuration terminée avec succès
        setResult(RESULT_OK)
        finish()
    }

    private fun cancelConfiguration() {
        // Configuration annulée
        setResult(RESULT_CANCELED)
        finish()
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, CalendarConfigurationActivity::class.java)
        }
    }
}