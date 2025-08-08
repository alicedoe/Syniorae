package com.syniorae.presentation.activities

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
 * Gère la navigation entre les 6 étapes avec indicateurs de progression
 */
class CalendarConfigurationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalendarConfigurationBinding
    private val configViewModel: CalendarConfigurationViewModel by viewModels()

    private var currentStep = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCalendarConfigurationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBackPressedCallback()
        observeViewModel()

        // Commencer par l'étape 1
        if (savedInstanceState == null) {
            navigateToStep(1)
        }
    }

    /**
     * Configure le callback pour le bouton retour moderne
     */
    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Gérer le retour selon l'étape actuelle
                if (currentStep > 1) {
                    configViewModel.previousStep()
                } else {
                    // Première étape → Annuler la configuration
                    cancelConfiguration()
                }
            }
        })
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

        lifecycleScope.launch {
            configViewModel.currentStep.collect { step ->
                currentStep = step
                updateProgressIndicators(step)
            }
        }
    }

    private fun navigateToStep(step: Int) {
        val fragment = when (step) {
            1 -> Step1GoogleAuthFragment() // ✅ Bon nom de classe
            2 -> Step2CalendarSelectionFragment()
            3 -> Step3WeeksParameterFragment()
            4 -> Step4EventsParameterFragment()
            5 -> Step5SyncFrequencyFragment()
            6 -> Step6IconAssociationsFragment()
            else -> return
        }

        replaceFragment(fragment)
        updateProgressIndicators(step)
    }

    private fun replaceFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()

        // Animation selon le sens de navigation
        if (currentStep < configViewModel.currentStep.value) {
            // Navigation vers l'avant
            transaction.setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
        } else {
            // Navigation vers l'arrière
            transaction.setCustomAnimations(
                R.anim.slide_in_left,
                R.anim.slide_out_right,
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
        }

        transaction.replace(R.id.fragment_container, fragment)
        transaction.commit()
    }

    /**
     * Met à jour les indicateurs de progression avec animation
     */
    private fun updateProgressIndicators(step: Int) {
        val indicators = listOf(
            binding.step1Indicator,
            binding.step2Indicator,
            binding.step3Indicator,
            binding.step4Indicator,
            binding.step5Indicator,
            binding.step6Indicator
        )

        val primaryColor = 0xFF2196F3.toInt() // Même bleu que primary_senior
        val grayColor = ContextCompat.getColor(this, android.R.color.darker_gray)

        indicators.forEachIndexed { index, indicator ->
            val targetColor = if (index < step) primaryColor else grayColor

            // Animation simple pour le changement de couleur
            val colorAnimator = ObjectAnimator.ofArgb(
                indicator,
                "backgroundColor",
                indicator.solidColor,
                targetColor
            )
            colorAnimator.duration = 200
            colorAnimator.start()
        }
    }

    private fun finishConfiguration() {
        // Configuration terminée avec succès
        setResult(RESULT_OK)

        // Utiliser finishAfterTransition() pour une animation moderne
        finishAfterTransition()
    }

    private fun cancelConfiguration() {
        // Configuration annulée
        setResult(RESULT_CANCELED)

        // Utiliser finishAfterTransition() pour une animation moderne
        finishAfterTransition()
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, CalendarConfigurationActivity::class.java)
        }
    }
}