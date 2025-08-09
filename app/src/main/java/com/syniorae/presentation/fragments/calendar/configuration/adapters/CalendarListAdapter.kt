package com.syniorae.presentation.fragments.calendar.configuration.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.syniorae.R
import com.syniorae.presentation.fragments.calendar.configuration.steps.CalendarItem

/**
 * Adapter pour la liste des calendriers disponibles
 */
class CalendarListAdapter(
    private val onCalendarSelected: (CalendarItem) -> Unit
) : ListAdapter<CalendarItem, CalendarListAdapter.CalendarViewHolder>(CalendarDiffCallback()) {

    private var selectedCalendarId: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_selection, parent, false)
        return CalendarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        val calendar = getItem(position)
        holder.bind(calendar, calendar.id == selectedCalendarId)
    }

    /**
     * Met à jour l'ID du calendrier sélectionné
     */
    fun setSelectedCalendarId(calendarId: String?) {
        val previousSelectedId = selectedCalendarId
        selectedCalendarId = calendarId

        // Notifier les changements pour mise à jour visuelle
        currentList.forEachIndexed { index, calendar ->
            if (calendar.id == previousSelectedId || calendar.id == calendarId) {
                notifyItemChanged(index)
            }
        }
    }

    /**
     * Récupère l'ID du calendrier actuellement sélectionné
     */
    fun getSelectedCalendarId(): String? = selectedCalendarId

    inner class CalendarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.calendarName)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.calendarDescription)
        private val sharedIndicator: ImageView = itemView.findViewById(R.id.sharedIndicator)
        private val selectedIndicator: ImageView = itemView.findViewById(R.id.selectedIndicator)
        private val colorIndicator: View = itemView.findViewById(R.id.colorIndicator)

        fun bind(calendar: CalendarItem, isSelected: Boolean) {
            nameTextView.text = calendar.name
            descriptionTextView.text = calendar.description

            // Afficher l'indicateur de partage
            sharedIndicator.visibility = if (calendar.isShared) View.VISIBLE else View.GONE

            // Afficher l'indicateur de sélection
            selectedIndicator.visibility = if (isSelected) View.VISIBLE else View.GONE

            // Couleur du calendrier (si supportée dans le layout)
            try {
                val color = android.graphics.Color.parseColor(calendar.backgroundColor)
                colorIndicator.setBackgroundColor(color)
            } catch (e: Exception) {
                // Couleur par défaut si parsing échoue
                colorIndicator.setBackgroundColor(android.graphics.Color.parseColor("#1976d2"))
            }

            // Mettre en évidence si sélectionné
            itemView.alpha = if (isSelected) 1.0f else 0.7f
            itemView.isSelected = isSelected

            // Gérer le clic
            itemView.setOnClickListener {
                onCalendarSelected(calendar)
            }
        }
    }
}

/**
 * DiffCallback pour optimiser les performances de la RecyclerView
 */
class CalendarDiffCallback : DiffUtil.ItemCallback<CalendarItem>() {
    override fun areItemsTheSame(oldItem: CalendarItem, newItem: CalendarItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: CalendarItem, newItem: CalendarItem): Boolean {
        return oldItem == newItem
    }
}