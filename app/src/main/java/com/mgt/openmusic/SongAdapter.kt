package com.mgt.openmusic

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.item_song.view.*


class SongAdapter(
    private val clickListener: ClickListener
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {
    var songs = ArrayList<Song>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        return SongViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_song, parent, false)
        )
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return songs.size
    }

    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        fun bind(position: Int) {
            val song = songs[position]
            itemView.apply {
                titleTextView.text = song.title
                artistTextView.text = song.artist

                bindDuration(song)

                Glide.with(context).let {
                    song.album?.thumb?.photo_68?.let { url ->
                        it.load(url)
                            .placeholder(R.mipmap.ic_launcher)
                            .error(R.mipmap.ic_launcher)
                    } ?: it.load(R.mipmap.ic_launcher)
                }.transition(DrawableTransitionOptions.withCrossFade(300))
                    .apply(
                        RequestOptions()
                            .transform(CenterCrop())
                            .transform(RoundedCorners(45))
                    )
                    .into(thumbImgView)

                downloadButton.setOnClickListener(this@SongViewHolder)
                openButton.setOnClickListener(this@SongViewHolder)
                cancelButton.setOnClickListener(this@SongViewHolder)
                shareButton.setOnClickListener(this@SongViewHolder)
                setOnClickListener(this@SongViewHolder)
            }
        }

        override fun onClick(v: View?) {
            when (v?.id) {
                R.id.downloadButton -> clickListener.onDownload(adapterPosition)
                R.id.shareButton -> clickListener.onShare(adapterPosition)
                R.id.rootItemView -> clickListener.onPlay(adapterPosition)
                R.id.openButton -> clickListener.onOpen(adapterPosition)
                R.id.cancelButton -> clickListener.onCancel(adapterPosition)
            }
        }

        fun bindDuration(song: Song) {
            itemView.apply {
                if (song.duration > 0) {
                    durationTextView.text = Utils.getDurationFormat(song.duration)
                    durationTextView.setTextColor(
                        Utils.getAttrColor(
                            context.theme,
                            R.attr.colorOnPrimary
                        )
                    )
                } else {
                    durationTextView.text = context.getString(R.string.unknown)
                    durationTextView.setTextColor(ContextCompat.getColor(context, R.color.error))
                }
            }
        }

        fun startDownload() {
            itemView.apply {
                cancelButton.visibility = View.VISIBLE
                downloadButton.visibility = View.INVISIBLE
                openButton.visibility = View.INVISIBLE
                downloadProgressBar.visibility = View.VISIBLE
                downloadProgressBar.progress = 0
                downloadButton.setImageResource(R.drawable.cancel)
            }
        }

        fun finishDownload(isSuccess: Boolean) {
            itemView.apply {
                downloadProgressBar.visibility = View.GONE
                cancelButton.visibility = View.INVISIBLE
                downloadButton.setImageResource(
                    if (isSuccess) {
                        downloadButton.visibility = View.INVISIBLE
                        openButton.visibility = View.VISIBLE
                        R.drawable.downloaded
                    } else {
                        downloadButton.visibility = View.VISIBLE
                        openButton.visibility = View.INVISIBLE
                        R.drawable.download
                    }
                )
            }
        }
    }

    interface ClickListener {
        fun onDownload(position: Int)
        fun onOpen(position: Int)
        fun onCancel(position: Int)
        fun onShare(position: Int)
        fun onPlay(position: Int)
    }
}