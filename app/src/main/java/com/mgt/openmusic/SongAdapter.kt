package com.mgt.openmusic

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.item_song.view.*


class SongAdapter(
    private val clickListener: (view: View, position: Int) -> Any?,
    private val seekBarProgressListener: (progress: Float) -> Any?,
    private val focusedMedia: MainViewModel.SongMedia,
) :
    RecyclerView.Adapter<SongAdapter.SongViewHolder>() {
    var songs = ArrayList<Song>()

    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(position: Int) {
            val song = songs[position]
            itemView.apply {
                titleTextView.text = song.title
                artistTextView.text = song.artist

                bindDuration(song)
                if (focusedMedia.song == song) {
                    seekBar.progress = (focusedMedia.progress * 1000).toInt()
                }

                val requestOptions = RequestOptions().transforms(CenterCrop(), RoundedCorners(45))
                Glide.with(context).let {
                    song.album?.thumb?.photo_68?.let { url ->
                        it.load(url)
                            .placeholder(R.mipmap.ic_launcher)
                            .error(R.mipmap.ic_launcher)
                    } ?: it.load(R.mipmap.ic_launcher)
                }.transition(DrawableTransitionOptions.withCrossFade(300))
                    .apply(requestOptions)
                    .into(thumbImgView)

                downloadButton.setOnClickListener { clickListener(downloadButton, position) }
                openButton.setOnClickListener { clickListener(openButton, position) }
                cancelButton.setOnClickListener { clickListener(cancelButton, position) }
                shareButton.setOnClickListener { clickListener(shareButton, position) }
                thumbImgView.setOnClickListener { clickListener(thumbImgView, position) }
                seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                        logD(TAG, "onStartTrackingTouch, progress: ${seekBar?.progress}")
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                        logD(TAG, "onStopTrackingTouch, progress: ${seekBar?.progress}")
                        // seekbar max progress = 1000 -> reduce loss when convert to player offset
                        seekBar?.let { seekBarProgressListener(it.progress / 10f) }
                    }
                })
            }

            bindPlayState(song.state, true)
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

        private fun bindPlayState(
            state: Int,
            bindPlayAnimView: Boolean = false
        ) {
            itemView.apply {
                when (state) {
                    Song.STATE_PREPARING -> {
                        titleTextView.ellipsize = TextUtils.TruncateAt.END
                        seekBar.visibility = View.GONE
                        seekBar.progress = 0
                        loadingAnimView.visibility = View.VISIBLE
                        if (bindPlayAnimView) {
                            playAnimView.speed = 1f
                            playAnimView.progress = 1f
                        }
                    }
                    Song.STATE_PLAYING -> {
                        titleTextView.ellipsize = TextUtils.TruncateAt.MARQUEE
                        titleTextView.isSelected = true
                        seekBar.visibility = View.VISIBLE
                        loadingAnimView.visibility = View.INVISIBLE
                        if (bindPlayAnimView) {
                            playAnimView.speed = 1f
                            playAnimView.progress = 1f
                        }
                    }
                    Song.STATE_PAUSED -> {
                        titleTextView.ellipsize = TextUtils.TruncateAt.MARQUEE
                        titleTextView.isSelected = true
                        seekBar.visibility = View.VISIBLE
                        loadingAnimView.visibility = View.INVISIBLE
                        if (bindPlayAnimView) {
                            playAnimView.speed = 1f
                            playAnimView.progress = 0f
                        }
                    }
                    Song.STATE_IDLE -> {
                        titleTextView.ellipsize = TextUtils.TruncateAt.END
                        seekBar.visibility = View.GONE
                        seekBar.progress = 0
                        loadingAnimView.visibility = View.INVISIBLE
                        if (bindPlayAnimView) {
                            playAnimView.speed = 1f
                            playAnimView.progress = 0f
                        }
                    }
                    Song.STATE_COMPLETE -> {
                        titleTextView.ellipsize = TextUtils.TruncateAt.END
                        seekBar.visibility = View.VISIBLE
                        seekBar.progress = 1000
                        loadingAnimView.visibility = View.INVISIBLE
                        if (bindPlayAnimView) {
                            playAnimView.speed = 1f
                            playAnimView.progress = 0f
                        }
                    }
                }
            }
        }

        fun prepare() {
            itemView.apply {
                playAnimView.speed = 1f
                playAnimView.playAnimation()
                bindPlayState(Song.STATE_PREPARING)
            }
        }

        fun play() {
            itemView.apply {
                bindPlayState(Song.STATE_PLAYING)
            }
        }

        fun resume() {
            itemView.apply {
                playAnimView.speed = 1f
                playAnimView.playAnimation()
                bindPlayState(Song.STATE_PLAYING)
            }
        }

        fun pause() {
            itemView.apply {
                playAnimView.speed = -1f
                playAnimView.playAnimation()
                bindPlayState(Song.STATE_PAUSED)
            }
        }

        fun stop() {
            itemView.apply {
                playAnimView.speed = -1f
                playAnimView.playAnimation()
                bindPlayState(Song.STATE_IDLE)
            }
        }

        fun complete() {
            itemView.apply {
                playAnimView.speed = -1f
                playAnimView.playAnimation()
                bindPlayState(Song.STATE_COMPLETE)
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

        fun updateProgress(progress: Int) {
            itemView.apply {
                downloadProgressBar.progress = progress
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

        fun seekBar(): SeekBar {
            return itemView.seekBar
        }
    }

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
}