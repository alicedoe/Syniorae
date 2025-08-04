package com.syniorae.presentation.fragments.calendar.configuration.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.syniorae.databinding.ItemIconAssociationBinding
import com.syniorae.domain.models.widgets.calendar.IconAssociation

/**
 * Adapter pour afficher les associations d'icônes
 */
class IconAssociationAdapter(
    private val onDeleteClick: (Int) -> Unit
) : ListAdapter<IconAssociation, IconAssociationAdapter.AssociationViewHolder>(AssociationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssociationViewHolder {
        val binding = ItemIconAssociationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AssociationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AssociationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AssociationViewHolder(
        private val binding: ItemIconAssociationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(association: IconAssociation) {
            binding.associationIcon.text = association.iconPath
            binding.associationKeywords.text = association.getKeywordsDisplay()

            // Bouton supprimer
            binding.deleteButton.setOnClickListener {
                onDeleteClick(bindingAdapterPosition)
            }

            // Exemple d'utilisation
            val exampleText = "Exemple : \"${association.keywords.firstOrNull() ?: "mot-clé"}\" → ${association.iconPath}"
            binding.associationExample.text = exampleText
        }
    }

    private class AssociationDiffCallback : DiffUtil.ItemCallback<IconAssociation>() {
        override fun areItemsTheSame(oldItem: IconAssociation, newItem: IconAssociation): Boolean {
            return oldItem.iconPath == newItem.iconPath && oldItem.keywords == newItem.keywords
        }

        override fun areContentsTheSame(oldItem: IconAssociation, newItem: IconAssociation): Boolean {
            return oldItem == newItem
        }
    }
}