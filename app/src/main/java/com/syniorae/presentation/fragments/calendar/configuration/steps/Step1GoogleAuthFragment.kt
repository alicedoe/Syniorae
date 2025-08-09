package com.syniorae.presentation.fragments.calendar.configuration.steps

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.syniorae.databinding.FragmentStep1GoogleAuthBinding
import com.syniorae.presentation.fragments.calendar.configuration.CalendarConfigurationViewModel
import kotlinx.coroutines.launch

/**
 * Étape 1 : Connexion Google et autorisations
 * Fragment corrigé pour gérer la vraie connexion Google avec popup
 */
class Step1GoogleAuthFragment : Fragment() {

    private var _binding: FragmentStep1GoogleAuthBinding? = null
    private val binding get() = _binding!!

    private val configViewModel: CalendarConfigurationViewModel by activityViewModels()

    // ActivityResultLauncher pour gérer l'intent Google Sign-In
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialiser le launcher AVANT onCreateView
        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleGoogleSignInResult(result.resultCode, result.data)
        }
    }

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
            startGoogleAuthentication()
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

    /**
     * MÉTHODE CORRIGÉE - Lance l'authentification Google avec popup
     */
    private fun startGoogleAuthentication() {
        configViewModel.setLoading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Récupérer le gestionnaire d'authentification
                val authManager = com.syniorae.core.di.DependencyInjection.getGoogleAuthManager()

                // Vérifier si déjà connecté
                if (authManager.isSignedIn()) {
                    val email = authManager.getSignedInAccountEmail()
                    if (!email.isNullOrBlank()) {
                        configViewModel.setGoogleAccount(email)
                        configViewModel.setLoading(false)
                        showSuccess("Déjà connecté à Google !")
                        return@launch
                    }
                }

                // Obtenir l'intent de connexion Google
                val result = authManager.signIn()

                when (result) {
                    is com.syniorae.data.remote.google.GoogleAuthResult.Success -> {
                        if (result.userEmail == "pending") {
                            // Lancer l'intent Google Sign-In - CECI VA OUVRIR LE POPUP
                            val signInIntent = authManager.getSignInIntent()
                            if (signInIntent != null) {
                                googleSignInLauncher.launch(signInIntent)
                            } else {
                                configViewModel.setLoading(false)
                                showError("Impossible de lancer l'authentification Google")
                            }
                        } else {
                            // Déjà connecté
                            configViewModel.setGoogleAccount(result.userEmail)
                            configViewModel.setLoading(false)
                            showSuccess("Connexion Google réussie !")
                        }
                    }
                    is com.syniorae.data.remote.google.GoogleAuthResult.Error -> {
                        configViewModel.setLoading(false)
                        showError("Erreur de connexion: ${result.message}")
                    }
                    is com.syniorae.data.remote.google.GoogleAuthResult.Cancelled -> {
                        configViewModel.setLoading(false)
                        showInfo("Connexion annulée")
                    }
                }

            } catch (e: Exception) {
                configViewModel.setLoading(false)
                showError("Erreur inattendue: ${e.message}")
            }
        }
    }

    /**
     * NOUVELLE MÉTHODE - Traite le résultat du Google Sign-In
     */
    private fun handleGoogleSignInResult(resultCode: Int, data: Intent?) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d("SYNIORAE_AUTH", "=== DEBUT handleGoogleSignInResult ===")
                Log.d("SYNIORAE_AUTH", "resultCode = $resultCode")

                // AJOUT : Même si c'est CANCELED, vérifions ce que Google nous dit
                if (data != null) {
                    Log.d("SYNIORAE_AUTH", "Intent data présent - Tentative d'analyse...")
                    val authManager = com.syniorae.core.di.DependencyInjection.getGoogleAuthManager()
                    val result = authManager.handleSignInResult(data)
                    Log.d("SYNIORAE_AUTH", "Résultat malgré CANCELED: $result")

                    when (result) {
                        is com.syniorae.data.remote.google.GoogleAuthResult.Success -> {
                            Log.d("SYNIORAE_AUTH", "SUCCESS malgré CANCELED: ${result.userEmail}")
                            configViewModel.setGoogleAccount(result.userEmail)
                            showSuccess("Connexion Google réussie !")
                            configViewModel.setLoading(false)
                            return@launch
                        }
                        is com.syniorae.data.remote.google.GoogleAuthResult.Error -> {
                            Log.d("SYNIORAE_AUTH", "ERROR: ${result.message}")
                            showError("Erreur de connexion: ${result.message}")
                        }
                        is com.syniorae.data.remote.google.GoogleAuthResult.Cancelled -> {
                            Log.d("SYNIORAE_AUTH", "CANCELLED confirmé")
                            showInfo("Connexion annulée par l'utilisateur")
                        }
                    }
                }

                if (resultCode == Activity.RESULT_OK) {
                    // Code normal...
                } else {
                    Log.d("SYNIORAE_AUTH", "ResultCode CANCELED mais on a essayé quand même")
                    showInfo("Connexion annulée - resultCode: $resultCode")
                }

                configViewModel.setLoading(false)

            } catch (e: Exception) {
                Log.e("SYNIORAE_AUTH", "EXCEPTION: ${e.message}", e)
                configViewModel.setLoading(false)
                showError("Erreur lors du traitement: ${e.message}")
            }
        }
    }

    private fun showPermissionsInfo() {
        binding.permissionsInfo.visibility = View.VISIBLE
        binding.permissionsInfo.text = "✓ Accès aux calendriers accordé\n✓ Lecture seule des événements"
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(resources.getColor(android.R.color.holo_green_dark, null))
            .show()
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(resources.getColor(android.R.color.holo_red_dark, null))
            .show()
    }

    private fun showInfo(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}