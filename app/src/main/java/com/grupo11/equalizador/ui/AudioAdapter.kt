package com.grupo11.equalizador.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.grupo11.equalizador.R
import com.grupo11.equalizador.data.AudioTrack

class AudioAdapter(
    private val tracks: List<AudioTrack>,
    private val onClick: (AudioTrack) -> Unit
): RecyclerView.Adapter<AudioAdapter.Holder>() {

    inner class Holder(item: View): RecyclerView.ViewHolder(item) {
        private val tvTitle = item.findViewById<TextView>(R.id.textTitle)
        fun bind(track: AudioTrack) {
            tvTitle.text = track.title
            itemView.setOnClickListener { onClick(track) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_audio, parent, false)
        return Holder(v)
    }
    override fun getItemCount() = tracks.size
    override fun onBindViewHolder(holder: Holder, pos: Int) {
        holder.bind(tracks[pos])
    }

}
