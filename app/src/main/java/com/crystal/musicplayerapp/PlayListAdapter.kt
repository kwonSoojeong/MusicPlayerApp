package com.crystal.musicplayerapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class PlayListAdapter(private val clickCallback:(MusicModel)->Unit): ListAdapter<MusicModel,PlayListAdapter.ViewHolder>(diffUtil) {
    companion object{
        val diffUtil = object:DiffUtil.ItemCallback<MusicModel>(){
            override fun areItemsTheSame(oldItem: MusicModel, newItem: MusicModel): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: MusicModel, newItem: MusicModel): Boolean {
                return oldItem == newItem
            }

        }
    }
    inner class ViewHolder(private val view: View): RecyclerView.ViewHolder(view) {
        fun bind(item: MusicModel){
            view.findViewById<TextView>(R.id.itemArtistTextView).text = item.artist
            view.findViewById<TextView>(R.id.itemTrackTitleView).text = item.track
            val imageView = view.findViewById<ImageView>(R.id.itemCoverImageView)
            Glide.with(imageView.context)
                .load(item.coverUrl)
                .into(imageView)

            if(item.isPlaying){
                itemView.setBackgroundColor(Color.GRAY)
            }else{
                itemView.setBackgroundColor(Color.TRANSPARENT)
            }

            itemView.setOnClickListener {
                clickCallback(item)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_music, parent,false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(currentList[position])
    }
}