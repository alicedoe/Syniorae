package com.syniorae.presentation.fragments.home.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.syniorae.databinding.ItemTodayEventBinding
import com.syniorae.domain.models.widgets.calendar.CalendarEvent

/**
 * Adapter pour afficher les événements du jour dans la colonne droite
 */
class TodayEventsAdapter : ListAdapter<CalendarEvent, TodayEventsAdapter.EventViewHolder>(EventDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemTodayEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class EventViewHolder(
        private val binding: ItemTodayEventBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: CalendarEvent) {
            binding.eventTime.text = event.getDisplayTime()
            binding.eventTitle.text = event.title

            // Indicateur si l'événement est en cours
            binding.eventIndicator.visibility = if (event.isCurrentlyRunning) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
        }
    }

    private class EventDiffCallback : DiffUtil.ItemCallback<CalendarEvent>() {
        override fun areItemsTheSame(oldItem: CalendarEvent, newItem: CalendarEvent): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CalendarEvent, newItem: CalendarEvent): Boolean {
            return oldItem == newItem
        }
    }
}