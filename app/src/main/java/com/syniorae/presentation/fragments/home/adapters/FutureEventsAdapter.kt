package com.syniorae.presentation.fragments.home.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.syniorae.databinding.ItemFutureEventBinding
import com.syniorae.domain.models.widgets.calendar.CalendarEvent
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Adapter pour afficher les événements futurs groupés par date
 */
class FutureEventsAdapter : ListAdapter<FutureEventsAdapter.EventItem, FutureEventsAdapter.EventViewHolder>(EventItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemFutureEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * Convertit une liste d'événements en items avec en-têtes de date
     */
    fun submitEventsList(events: List<CalendarEvent>) {
        val groupedEvents = events
            .filter { it.isFuture() }
            .sortedBy { it.startDateTime }
            .groupBy { it.startDateTime.toLocalDate() }

        val items = mutableListOf<EventItem>()

        groupedEvents.forEach { (date, dayEvents) ->
            // Ajouter l'en-tête de date pour le premier événement
            dayEvents.forEachIndexed { index, event ->
                items.add(
                    EventItem(
                        event = event,
                        showDateHeader = index == 0,
                        isLastInDay = index == dayEvents.size - 1,
                        date = date
                    )
                )
            }
        }

        submitList(items)
    }

    class EventViewHolder(
        private val binding: ItemFutureEventBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.getDefault())

        fun bind(item: EventItem) {
            val event = item.event

            // En-tête de date
            if (item.showDateHeader) {
                binding.dateHeader.visibility = View.VISIBLE
                binding.dateHeader.text = item.date.format(dateFormatter)
                    .replaceFirstChar { it.uppercase() }
            } else {
                binding.dateHeader.visibility = View.GONE
            }

            // Détails de l'événement
            binding.eventTime.text = event.getDisplayTime()
            binding.eventTitle.text = event.title

            // Icône de l'événement (pour les futures versions avec associations)
            binding.eventIcon.visibility = View.GONE // Pour l'instant

            // Séparateur (masqué pour le dernier événement du jour)
            binding.separator.visibility = if (item.isLastInDay) View.GONE else View.VISIBLE
        }
    }

    /**
     * Item représentant un événement avec ses métadonnées d'affichage
     */
    data class EventItem(
        val event: CalendarEvent,
        val showDateHeader: Boolean,
        val isLastInDay: Boolean,
        val date: LocalDate
    )

    private class EventItemDiffCallback : DiffUtil.ItemCallback<EventItem>() {
        override fun areItemsTheSame(oldItem: EventItem, newItem: EventItem): Boolean {
            return oldItem.event.id == newItem.event.id &&
                    oldItem.showDateHeader == newItem.showDateHeader
        }

        override fun areContentsTheSame(oldItem: EventItem, newItem: EventItem): Boolean {
            return oldItem == newItem
        }
    }
}