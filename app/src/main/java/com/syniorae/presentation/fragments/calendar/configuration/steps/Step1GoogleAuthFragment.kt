package com.syniorae.presentation.fragments.calendar.configuration.steps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.syniorae.databinding.FragmentStep1GoogleAuthBinding
import com.syniorae.presentation.fragments.calendar.configuration.CalendarConfigurationViewModel
import kotlinx.coroutines.launch

/**
 * Étape 1 : Connexion Google et autorisations
 */
class Step1GoogleAuthFragment : Fragment() {

    private var _binding: FragmentStep1GoogleAuthBinding? = null
    private val binding get() = _binding!!

    private val configViewModel: CalendarConfigurationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStep1GoogleAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.stepTitle.text = "Étape 1/6 - Connexion Google"
        binding.stepDescription.text = "Connectez votre compte Google pour accéder à vos calendriers"

        // Bouton de connexion Google (simulation)
        binding.connectGoogleButton.setOnClickListener {
            simulateGoogleConnection()
        }

        // Bouton suivant
        binding.nextButton.setOnClickListener {
            configViewModel.nextStep()
        }

        // Bouton annuler
        binding.cancelButton.setOnClickListener {
            configViewModel.previousStep()
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
        if (state.isGoogleConnected) {
            binding.connectGoogleButton.visibility = View.GONE
            binding.connectedAccountInfo.visibility = View.VISIBLE
            binding.connectedAccountEmail.text = state.googleAccountEmail
            binding.nextButton.isEnabled = true
        } else {
            binding.connectGoogleButton.visibility = View.VISIBLE
            binding.connectedAccountInfo.visibility = View.GONE
            binding.nextButton.isEnabled = false
        }
    }

    /**
     * Simule la connexion Google
     */
    private fun simulateGoogleConnection() {
        // TODO: Remplacer par vraie authentification Google
        val fakeEmail = "utilisateur@gmail.com"
        configViewModel.setGoogleAccount(fakeEmail)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}