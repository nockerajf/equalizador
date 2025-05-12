package com.grupo11.equalizador.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.grupo11.equalizador.R
import com.grupo11.equalizador.data.AudioTrack

class AudioAdapter(
    private val audioTracks: List<AudioTrack>,
    private val onItemClick: (AudioTrack) -> Unit
) : RecyclerView.Adapter<AudioAdapter.AudioViewHolder>() {

    private var selectedPosition = RecyclerView.NO_POSITION // Variável para rastrear a posição selecionada

    inner class AudioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val titleTextView: TextView = itemView.findViewById(R.id.textTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_audio, parent, false)
        return AudioViewHolder(view)
    }
    override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {
        val track = audioTracks[position]
        holder.titleTextView.text = track.title

        if (position == selectedPosition) {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.selected_item_background))
        } else {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.default_item_background))
        }

        holder.itemView.setOnClickListener {
            val previouslySelectedPosition = selectedPosition
            selectedPosition = holder.adapterPosition

            if (previouslySelectedPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(previouslySelectedPosition) // Desmarca o item anterior
            }
            notifyItemChanged(selectedPosition) // Marca o novo item

            onItemClick(track) // Chama o lambda de clique original
        }
    }

    override fun getItemCount() = audioTracks.size
}