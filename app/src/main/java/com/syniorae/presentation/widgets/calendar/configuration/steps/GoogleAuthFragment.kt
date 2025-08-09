package com.syniorae.presentation.widgets.calendar.configuration.steps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.syniorae.data.remote.google.GoogleAuthState
import com.syniorae.databinding.FragmentStep1GoogleAuthBinding
import com.syniorae.presentation.fragments.calendar.configuration.CalendarConfigurationViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Étape 1 : Connexion Google et autorisations
 * Fragment corrigé pour gérer la vraie connexion Google
 */
class Step1GoogleAuthFragment : Fragment() {

    private var _binding: FragmentStep1GoogleAuthBinding? = null
    private val binding get() = _binding!!

    private val _authState = MutableStateFlow(GoogleAuthState())
    val authState: StateFlow<GoogleAuthState> = _authState.asStateFlow()

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
        // Configuration des textes
        binding.stepTitle.text = "Étape 1/6 - Connexion Google"
        binding.stepDescription.text = "Connectez votre compte Google pour accéder à vos calendriers."

        // Bouton de connexion Google
        binding.connectGoogleButton.setOnClickListener {
            handleGoogleConnection()
        }

        // Bouton suivant (initialement désactivé)
        binding.nextButton.apply {
            isEnabled = false
            setOnClickListener {
                configViewModel.nextStep()
            }
        }

        // Bouton annuler
        binding.cancelButton.setOnClickListener {
            requireActivity().finish()
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
        if (state.isGoogleConnected) {
            // Connexion réussie
            binding.connectGoogleButton.visibility = View.GONE
            binding.connectedAccountInfo.visibility = View.VISIBLE
            binding.connectedAccountEmail.text = state.googleAccountEmail
            binding.nextButton.isEnabled = true

            // Afficher les permissions accordées
            showPermissionsInfo()
        } else {
            // Pas encore connecté
            binding.connectGoogleButton.visibility = View.VISIBLE
            binding.connectedAccountInfo.visibility = View.GONE
            binding.nextButton.isEnabled = false
        }
    }

    private fun updateLoadingState(isLoading: Boolean) {
        binding.connectGoogleButton.isEnabled = !isLoading
        binding.cancelButton.isEnabled = !isLoading

        if (isLoading) {
            binding.connectGoogleButton.text = "Connexion en cours..."
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.connectGoogleButton.text = "Se connecter avec Google"
            binding.progressBar.visibility = View.GONE
        }
    }
    suspend fun requestCalendarPermissions(): Boolean {
        return hasCalendarPermissions()
    }

    fun hasCalendarPermissions(): Boolean {
        return _authState.value.hasCalendarPermission
    }

    private fun showPermissionsInfo() {
        binding.permissionsInfo.visibility = View.VISIBLE
        binding.permissionsInfo.text = "✓ Accès aux calendriers accordé\n✓ Lecture seule des événements"
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(resources.getColor(android.R.color.holo_green_light, null))
            .show()
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(resources.getColor(android.R.color.holo_red_light, null))
            .show()
    }

    private fun showInfo(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun handleGoogleConnection() {
        configViewModel.setLoading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Récupérer le gestionnaire d'authentification
                val authManager = com.syniorae.core.di.DependencyInjection.getGoogleAuthManager()

                // Tentative de connexion
                val result = authManager.signIn()

                when (result) {
                    is com.syniorae.data.remote.google.GoogleAuthResult.Success -> {
                        // Demander les permissions pour les calendriers
                        if (authManager.requestCalendarPermissions()) {
                            configViewModel.setGoogleAccount(result.userEmail)
                            showSuccess("Connexion Google réussie !")
                        } else {
                            authManager.signOut()
                            showError("Les permissions d'accès au calendrier sont requises.")
                        }
                    }
                    is com.syniorae.data.remote.google.GoogleAuthResult.Error -> {
                        showError("Erreur de connexion : ${result.message}")
                    }
                    is com.syniorae.data.remote.google.GoogleAuthResult.Cancelled -> {
                        showInfo("Connexion annulée.")
                    }
                }
            } catch (e: Exception) {
                showError("Erreur inattendue : ${e.message}")
            } finally {
                configViewModel.setLoading(false)
            }
        }
    }
}