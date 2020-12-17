package com.mgt.openmusic

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.song_play_view.view.*
import kotlin.math.round


class SongPlayView : FrameLayout, View.OnClickListener {
    var songName: String = DEFAULT_SONG_NAME
        set(value) {
            field = value
            bindTitle()
        }
    var artistName: String = DEFAULT_ARTIST_NAME
        set(value) {
            field = value
            bindTitle()
        }
    var thumbSrc: Drawable? = null
        set(value) {
            field = value
            useSrc = true
            bindThumb()
        }
    var thumbUrl: String? = null
        set(value) {
            field = value
            useSrc = false
            bindThumb()
        }

    // second
    var duration: Int = 0
        set(value) {
            field = value
            bindDuration()
        }

    var progress: Float = 0f
        set(value) {
            field = value
            bindProgress()
        }

    var playState: PlayState = PlayState.STATE_PREPARING
        private set(value) {
            field = value
            bindPlayState(true)
        }

    var eventListener: EventListener? = null
    private val seekBarListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            seekBar?.let { eventListener?.onSongProgressSelected(it.progress.toFloat() / SEEK_BAR_MAX_VALUE) }
        }
    }

    private var useSrc = true
    private lateinit var rotateAnim: ObjectAnimator

    constructor(
        context: Context,
        attrs: AttributeSet?
    ) : super(
        context,
        attrs
    ) {
        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.SongPlayView,
            0,
            0
        )
        songName = typedArray.getString(R.styleable.SongPlayView_songName) ?: DEFAULT_SONG_NAME
        artistName =
            typedArray.getString(R.styleable.SongPlayView_artistName) ?: DEFAULT_ARTIST_NAME
        thumbSrc =
            typedArray.getDrawable(R.styleable.SongPlayView_thumbSrc) ?: ContextCompat.getDrawable(
                context,
                R.mipmap.ic_launcher
            )
        thumbUrl = typedArray.getString(R.styleable.SongPlayView_thumbUrl)
        duration = typedArray.getInt(R.styleable.SongPlayView_duration, 0)
        progress = typedArray.getFloat(R.styleable.SongPlayView_progress, 0f)

        useSrc = thumbSrc != null

        typedArray.recycle()
    }

    init {
        View.inflate(context, R.layout.song_play_view, this)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
//        setBackgroundColor(Utils.getAttrColor(context.theme, R.attr.colorBackgroundBold))
//        outlineProvider = ViewOutlineProvider.BOUNDS
//        setPadding(0, Utils.dp(4f), 0, 0)

        bindTitle()
        bindThumb()
        bindDuration()
        bindProgress()
        bindPlayState(false)

        playThumbImv.setOnClickListener(this)
        setOnClickListener(this)

        playSeekBar.max = SEEK_BAR_MAX_VALUE
        playSeekBar.setOnSeekBarChangeListener(seekBarListener)

        initAnim()
    }

    private fun initAnim() {
        rotateAnim = ObjectAnimator().apply {
            target = playThumbImv
            repeatCount = ValueAnimator.INFINITE
            setFloatValues(0f, 360f)
            setPropertyName("rotation")
            duration = 10000
            interpolator = LinearInterpolator()
        }
    }

    private fun bindTitle() {
        playTitleTextView.text = String.format("%s - %s", songName, artistName)
    }

    private fun bindThumb() {
        Glide.with(context).let {
            if (useSrc) {
                it.load(thumbSrc)
            } else {
                it.load(thumbUrl)
            }
        }.placeholder(thumbSrc)
            .error(thumbSrc)
            .circleCrop()
            .into(playThumbImv)
    }

    private fun bindDuration() {
        playDurationTextView.text = Utils.getDurationFormat(duration)
    }

    private fun bindProgress() {
        playSeekBar.progress = round(progress * SEEK_BAR_MAX_VALUE).toInt()
    }

    private fun bindPlayState(animate: Boolean = false) {
        when (playState) {
            PlayState.STATE_PREPARING -> {
                playTitleTextView.ellipsize = TextUtils.TruncateAt.END
                playSeekBar.progress = 0
                playLoadingAnimView.visibility = View.VISIBLE
                if (!animate) {
                    playAnimView.speed = 1f
                    playAnimView.progress = 1f
                }
            }
            PlayState.STATE_PLAYING -> {
                rotateAnim.resumeOrStart()
                playTitleTextView.ellipsize = TextUtils.TruncateAt.MARQUEE
                playTitleTextView.isSelected = true
                playSeekBar.visibility = View.VISIBLE
                playLoadingAnimView.visibility = View.INVISIBLE
                if (!animate) {
                    playAnimView.speed = 1f
                    playAnimView.progress = 1f
                }
            }
            PlayState.STATE_PAUSED, PlayState.STATE_STOPPED -> {
                rotateAnim.pause()
                playTitleTextView.ellipsize = TextUtils.TruncateAt.END
                playSeekBar.visibility = View.VISIBLE
                playLoadingAnimView.visibility = View.INVISIBLE
                if (!animate) {
                    playAnimView.speed = 1f
                    playAnimView.progress = 0f
                }
            }
            PlayState.STATE_COMPLETE -> {
                rotateAnim.pause()
                playTitleTextView.ellipsize = TextUtils.TruncateAt.END
                playSeekBar.visibility = View.VISIBLE
                playSeekBar.progress = 1000
                playLoadingAnimView.visibility = View.INVISIBLE
                if (!animate) {
                    playAnimView.speed = 1f
                    playAnimView.progress = 0f
                }
            }
        }
    }

    fun prepare() {
        playAnimView.playForward()
        playState = PlayState.STATE_PREPARING
    }

    fun play() {
        playState = PlayState.STATE_PLAYING
    }

    fun resume() {
        playAnimView.playForward()
        playState = PlayState.STATE_PLAYING
    }

    fun pause() {
        playAnimView.playBackward()
        playState = PlayState.STATE_PAUSED
    }

    fun stop(){
        playAnimView.playBackward()
        playState = PlayState.STATE_STOPPED
    }

    fun complete() {
        playAnimView.playBackward()
        playState = PlayState.STATE_COMPLETE
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.playThumbImv -> eventListener?.onThumbClick()
            R.id.songPlayView -> eventListener?.onClick()
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        return Bundle().apply {
            putParcelable(KEY_SUPER_STATE, super.onSaveInstanceState())
            putParcelable(KEY_PLAY_STATE, playState)
            putBoolean(KEY_USE_SRC, useSrc)
        }
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is Bundle) // implicit null check
        {
            state.getParcelable<PlayState>(KEY_PLAY_STATE)?.let { playState = it }
            useSrc = state.getBoolean(KEY_USE_SRC)
            bindPlayState(false)
            super.onRestoreInstanceState(state.getParcelable(KEY_SUPER_STATE))
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    interface EventListener {
        fun onThumbClick()
        fun onClick()
        fun onSongProgressSelected(progress: Float)
    }

    enum class PlayState(val value: Int) : Parcelable {
        STATE_PREPARING(0),
        STATE_PLAYING(1),
        STATE_PAUSED(2),
        STATE_COMPLETE(3),
        STATE_STOPPED(4);

        constructor(parcel: Parcel) : this(parcel.readInt())

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(value)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<PlayState> {
            override fun createFromParcel(parcel: Parcel): PlayState {
                return fromInt(parcel.readInt())
            }

            override fun newArray(size: Int): Array<PlayState?> {
                return arrayOfNulls(size)
            }

            private fun fromInt(value: Int): PlayState = values().first { it.value == value }
        }
    }

    companion object {
        const val DEFAULT_SONG_NAME = "song name"
        const val DEFAULT_ARTIST_NAME = "artist name"
        const val SEEK_BAR_MAX_VALUE = 1000

        const val KEY_SUPER_STATE = "superState"
        const val KEY_PLAY_STATE = "playState"
        const val KEY_USE_SRC = "useSrc"
    }
}