package com.syniorae.presentation.fragments.calendar.configuration.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.syniorae.databinding.ItemCalendarSelectionBinding
import com.syniorae.presentation.fragments.calendar.configuration.steps.CalendarItem

/**
 * Adapter pour afficher la liste des calendriers Google
 */
class CalendarListAdapter(
    private val onCalendarSelected: (CalendarItem) -> Unit
) : ListAdapter<CalendarItem, CalendarListAdapter.CalendarViewHolder>(CalendarDiffCallback()) {

    private var selectedCalendarId: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val binding = ItemCalendarSelectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CalendarViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CalendarViewHolder(
        private val binding: ItemCalendarSelectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(calendar: CalendarItem) {
            binding.calendarName.text = calendar.name
            binding.calendarDescription.text = calendar.description

            // Indicateur de calendrier partagé
            if (calendar.isShared) {
                binding.sharedIndicator.visibility = android.view.View.VISIBLE
                binding.sharedIndicator.text = "📤 Partagé"
            } else {
                binding.sharedIndicator.visibility = android.view.View.GONE
            }

            // État sélectionné
            val isSelected = calendar.id == selectedCalendarId
            binding.selectionIndicator.visibility = if (isSelected) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }

            // Style de la carte selon sélection
            binding.root.alpha = if (isSelected) 1.0f else 0.8f
            binding.root.elevation = if (isSelected) 8f else 4f

            // Clic sur l'item
            binding.root.setOnClickListener {
                val previousSelected = selectedCalendarId
                selectedCalendarId = calendar.id

                // Mettre à jour l'affichage
                currentList.indexOfFirst { it.id == previousSelected }.takeIf { it != -1 }?.let {
                    notifyItemChanged(it)
                }
                notifyItemChanged(adapterPosition)

                // Notifier la sélection
                onCalendarSelected(calendar)
            }
        }
    }

    private class CalendarDiffCallback : DiffUtil.ItemCallback<CalendarItem>() {
        override fun areItemsTheSame(oldItem: CalendarItem, newItem: CalendarItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CalendarItem, newItem: CalendarItem): Boolean {
            return oldItem == newItem
        }
    }
}