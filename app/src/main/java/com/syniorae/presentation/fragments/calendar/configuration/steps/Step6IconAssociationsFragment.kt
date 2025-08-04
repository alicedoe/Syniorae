package com.syniorae.presentation.fragments.calendar.configuration.steps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.syniorae.databinding.FragmentStep6IconAssociationsBinding
import com.syniorae.presentation.fragments.calendar.configuration.CalendarConfigurationViewModel
import com.syniorae.presentation.fragments.calendar.configuration.adapters.IconAssociationAdapter
import kotlinx.coroutines.launch

/**
 * Étape 6 : Associations d'icônes
 */
class Step6IconAssociationsFragment : Fragment() {

    private var _binding: FragmentStep6IconAssociationsBinding? = null
    private val binding get() = _binding!!

    private val configViewModel: CalendarConfigurationViewModel by activityViewModels()
    private lateinit var iconAdapter: IconAssociationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStep6IconAssociationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupRecyclerView()
        observeViewModel()
        addDefaultAssociations()
    }

    private fun setupUI() {
        binding.stepTitle.text = "Étape 6/6 - Associations d'icônes"
        binding.stepDescription.text = "Associez des mots-clés à des icônes pour vos événements (optionnel)"

        // Bouton d'ajout d'association
        binding.addAssociationButton.setOnClickListener {
            showAddAssociationDialog()
        }

        // Bouton terminer (remplace "Suivant" pour la dernière étape)
        binding.finishButton.setOnClickListener {
            configViewModel.nextStep() // Termine la configuration
        }

        binding.previousButton.setOnClickListener {
            configViewModel.previousStep()
        }

        // Bouton ignorer (pour passer cette étape)
        binding.skipButton.setOnClickListener {
            configViewModel.nextStep()
        }
    }

    private fun setupRecyclerView() {
        iconAdapter = IconAssociationAdapter { index ->
            // Supprimer une association
            configViewModel.removeIconAssociation(index)
        }

        binding.associationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = iconAdapter
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
        iconAdapter.submitList(state.iconAssociations)

        if (state.iconAssociations.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.associationsRecyclerView.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.associationsRecyclerView.visibility = View.VISIBLE
        }

        binding.associationsCount.text = "${state.iconAssociations.size} association(s) créée(s)"
    }

    /**
     * Ajoute quelques associations par défaut
     */
    private fun addDefaultAssociations() {
        val defaultAssociations = listOf(
            Pair(listOf("médecin", "docteur", "rdv médical"), "🏥"),
            Pair(listOf("travail", "réunion", "bureau"), "💼"),
            Pair(listOf("famille", "anniversaire", "fête"), "🎉")
        )

        defaultAssociations.forEach { (keywords, icon) ->
            configViewModel.addIconAssociation(keywords, icon)
        }
    }

    /**
     * Affiche le dialogue d'ajout d'association
     */
    private fun showAddAssociationDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(com.syniorae.R.layout.dialog_add_icon_association, null)

        val keywordsInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(
            com.syniorae.R.id.keywordsInput
        )
        val iconInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(
            com.syniorae.R.id.iconInput
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Ajouter une association")
            .setView(dialogView)
            .setPositiveButton("Ajouter") { _, _ ->
                val keywordsText = keywordsInput.text.toString()
                val iconText = iconInput.text.toString()

                if (keywordsText.isNotBlank() && iconText.isNotBlank()) {
                    val keywords = keywordsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    configViewModel.addIconAssociation(keywords, iconText)
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}