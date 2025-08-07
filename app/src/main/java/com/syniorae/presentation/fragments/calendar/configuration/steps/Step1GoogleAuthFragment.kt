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
 * Version complète avec gestion d'erreurs et feedback utilisateur
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

        // Bouton de connexion Google
        binding.connectGoogleButton.setOnClickListener {
            startGoogleAuthentication()
        }

        // Bouton suivant
        binding.nextButton.setOnClickListener {
            configViewModel.nextStep()
        }

        // Bouton annuler
        binding.cancelButton.setOnClickListener {
            // Retour vers la configuration ou annulation complète
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
            showPermissionsGranted()
        } else {
            // Pas encore connecté
            binding.connectGoogleButton.visibility = View.VISIBLE
            binding.connectedAccountInfo.visibility = View.GONE
            binding.nextButton.isEnabled = false
        }
    }

    private fun updateLoadingState(isLoading: Boolean) {
        binding.connectGoogleButton.isEnabled = !isLoading
        binding.nextButton.isEnabled = !isLoading && configViewModel.configState.value.isGoogleConnected

        if (isLoading) {
            binding.connectGoogleButton.text = "Connexion en cours..."
        } else {
            binding.connectGoogleButton.text = "Se connecter avec Google"
        }
    }

    private fun startGoogleAuthentication() {
        configViewModel.setLoading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val authManager = com.syniorae.core.di.DependencyInjection.getGoogleAuthManager(requireContext())

                // Tenter la connexion Google
                when (val result = authManager.signIn()) {
                    is com.syniorae.data.remote.google.GoogleAuthResult.Success -> {
                        // Connexion réussie, maintenant demander les permissions calendrier
                        if (authManager.requestCalendarPermissions()) {
                            // Tout est OK
                            configViewModel.setGoogleAccount(result.userEmail)
                            showSuccess("Connexion réussie !")
                        } else {
                            // Permissions refusées
                            showError("Les permissions d'accès au calendrier sont requises pour continuer.")
                            authManager.signOut() // Déconnecter si permissions refusées
                        }
                    }
                    is com.syniorae.data.remote.google.GoogleAuthResult.Error -> {
                        showError("Erreur de connexion : ${result.message}")
                    }
                    is com.syniorae.data.remote.google.GoogleAuthResult.Cancelled -> {
                        showError("Connexion annulée. Veuillez réessayer.")
                    }
                }
            } catch (e: Exception) {
                showError("Erreur inattendue : ${e.message}")
            } finally {
                configViewModel.setLoading(false)
            }
        }
    }

    private fun showPermissionsGranted() {
        // TODO: Afficher la liste des permissions accordées
        // Pour l'instant, juste un message de confirmation
    }

    private fun showSuccess(message: String) {
        context?.let { ctx ->
            android.widget.Toast.makeText(ctx, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun showError(message: String) {
        context?.let { ctx ->
            android.widget.Toast.makeText(ctx, message, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}