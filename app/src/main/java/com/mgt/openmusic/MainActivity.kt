package com.mgt.openmusic

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.max


class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var adapter: SongAdapter
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://myfreemp3.vip/")
        .addConverterFactory(ScalarsConverterFactory.create())
        .build()
    private val service = retrofit.create(ApiService::class.java)
    private val params = hashMapOf(
        "page" to "0"
    )

    private val adapterClickListener = { view: View, position: Int ->
        val song = adapter.songs[position]
        when (view.id) {
            R.id.downloadButton -> downloadSong(song)
            R.id.shareButton -> shareSong(song)
            R.id.thumbImgView -> {
                when (playingMedia.state) {
                    PlayingMedia.STATE_PLAYING, PlayingMedia.STATE_PREPARING -> {
                        if (song == playingMedia.playingSong) {
                            pauseSong(position)
                        } else {
                            stopSong()
                            playSong(position)
                        }
                    }
                    PlayingMedia.STATE_PAUSED -> {
                        if (song == playingMedia.playingSong) {
                            resumeSong(position)
                        } else {
                            stopSong()
                            playSong(position)
                        }
                    }
                    PlayingMedia.STATE_STOPPED -> playSong(position)
                }
            }
        }
    }

    private var isShow = false
    private val showAnim = ObjectAnimator().apply {
        duration = 200
        setFloatValues(0f, 1f)
        setPropertyName("alpha")
        doOnStart {
            if (isShow) {
                (target as View).visibility = View.VISIBLE
            }
        }
        doOnEnd {
            if (!isShow) {
                (target as View).visibility = View.INVISIBLE
            }
        }
    }

    private var isTopButtonShown = false

    private val playingMedia = PlayingMedia()

    private var firstSearch = true
    private val searchCallback = object : Callback<String> {
        override fun onResponse(call: Call<String>, response: retrofit2.Response<String>) {
            try {
                val json = "[${response.body().toString().let { it.substring(23, it.length - 3) }}"
                val songs = Gson().fromJson<List<Song>>(
                    json,
                    object : TypeToken<List<Song>>() {}.type
                )
                adapter.songs = songs.toMutableList() as ArrayList
                adapter.notifyDataSetChanged()
                if (songs.isEmpty()) {
                    showEmptyView()
                }
                loadingAnimView.visibility = View.INVISIBLE
            } catch (t: Throwable) {
                onFailure(call, t)
            }
        }

        override fun onFailure(call: Call<String>, t: Throwable) {
            t.printStackTrace()
            loadingAnimView.visibility = View.INVISIBLE
            showEmptyView()
        }
    }

    private var mediaPlayer: MediaPlayer? = null
    private var timer: Timer? = null
    private val prepareListener = MediaPlayer.OnPreparedListener {
        logD(TAG, "source prepared, offset: ${playingMedia.progress}, duration: ${it.duration}")
        playingMedia.duration = it.duration
        it.seekTo((playingMedia.progress * playingMedia.duration).toInt())
        if (playingMedia.state == PlayingMedia.STATE_PREPARING) {
            it.start()
            startTimer()
            playingMedia.state = PlayingMedia.STATE_PLAYING
        }
    }

    private val seekBarProgressListener = { progress: Float ->
        if (playingMedia.state == PlayingMedia.STATE_PLAYING || playingMedia.state == PlayingMedia.STATE_PAUSED) {
            val offset = (progress / 100f * playingMedia.duration).toInt()
            mediaPlayer?.seekTo(offset)
        } else {
            playingMedia.progress = progress / 100f
        }
    }

    private fun showEmptyView() {
        emptyIcon.visibility = View.VISIBLE
        emptyDescTextView.visibility = View.VISIBLE
    }

    private fun hideEmptyView() {
        emptyIcon.visibility = View.INVISIBLE
        emptyDescTextView.visibility = View.INVISIBLE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        setContentView(R.layout.activity_main)

        when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_NO -> setLightStatusBar(rootView)
            Configuration.UI_MODE_NIGHT_YES -> clearLightStatusBar()
        }

        runAnimation()

        initView()
    }

    private fun runAnimation() {
        ValueAnimator().apply {
            duration = 1000
            setFloatValues(0f, 1f)
            addUpdateListener {
                appBar.alpha = it.animatedFraction
                contentView.alpha = it.animatedFraction
                logoBg.alpha = max(1 - it.animatedFraction, 0.6f)
            }
            start()
        }
    }

    private fun initView() {
        adapter = SongAdapter(adapterClickListener, playingMedia, seekBarProgressListener)
        recyclerView.adapter = adapter
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE && recyclerView.computeVerticalScrollOffset() > 200) {
                    if (!isTopButtonShown) {
                        isTopButtonShown = true
                        show(topButton)
                    }
                } else {
                    if (isTopButtonShown) {
                        isTopButtonShown = false
                        hide(topButton)
                    }
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                rotateLogoBg(dy / 100f)
                Utils.hideKeyboard(this@MainActivity, rootView)
            }
        })

        searchButton.setOnClickListener(this)
        topButton.setOnClickListener(this)
        historyButton.setOnClickListener(this)

        searchEditText.setOnFocusChangeListener { _, b ->
            if (b) {
                searchEditText.post {
                    Utils.showKeyboard(this, searchEditText)
                }
            } else {
                Utils.hideKeyboard(this, rootView)
            }
        }
        searchEditText.requestFocus()
    }

    override fun onStart() {
        super.onStart()
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setOnPreparedListener(prepareListener)
        }
        if (playingMedia.state == PlayingMedia.STATE_PAUSED) {
            playSong(progress = playingMedia.progress)
        }
    }

    override fun onStop() {
        super.onStop()
        if (playingMedia.state == PlayingMedia.STATE_PLAYING || playingMedia.state == PlayingMedia.STATE_PREPARING) {
            pauseSong()
        }
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun rotateLogoBg(degree: Float) {
        logoBg.rotation += degree
    }

    private fun hide(view: View) {
        isShow = false
        showAnim.target = view
        showAnim.reverse()
    }

    private fun show(view: View) {
        isShow = true
        showAnim.target = view
        showAnim.start()
    }

    private fun downloadSong(song: Song) {
        Toast.makeText(this, "Not impl yet", Toast.LENGTH_SHORT).show()
    }

    private fun shareSong(song: Song) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, song.url)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    private fun playSong(position: Int? = playingMedia.playingPosition, progress: Float = 0f) {
        logD(TAG, "playSong: $position")
        position?.let {
            getHolder(it)?.play()
            adapter.songs[it].let { song ->
                playingMedia.playingPosition = position
                playingMedia.playingSong = song
                playingMedia.state = PlayingMedia.STATE_PREPARING
                playingMedia.progress = progress
                mediaPlayer?.apply {
                    setDataSource(song.url)
                    prepareAsync()
                }
            }
        }
    }

    private fun resumeSong(position: Int? = playingMedia.playingPosition) {
        logD(TAG, "resumeSong: $position")
        position?.let { getHolder(it)?.play() }
        mediaPlayer?.start()
        playingMedia.state = PlayingMedia.STATE_PLAYING
        startTimer()
    }

    private fun startTimer() {
        timer?.cancel()
        timer = Timer().also {
            it.schedule(object : TimerTask() {
                override fun run() {
                    runOnUiThread {
                        playingMedia.playingPosition?.let { position ->
                            (recyclerView.findViewHolderForAdapterPosition(position) as SongAdapter.SongViewHolder?)?.let { holder ->
                                if (playingMedia.state == PlayingMedia.STATE_PLAYING) {
                                    mediaPlayer?.let { player ->
                                        holder.seekBar().progress =
                                            1000 * player.currentPosition / playingMedia.duration
                                    } ?: logE(TAG, "mediaPlayer null")
                                }
                            } ?: logE(TAG, "holder null")
                        } ?: logE(TAG, "playingPosition null")
                    }
                }
            }, 0, 1000)
        }
    }

    private fun stopTimer() {
        timer?.cancel()
        timer = null
    }

    private fun pauseSong(
        position: Int? = playingMedia.playingPosition
    ) {
        logD(TAG, "pauseSong: $position")
        playingMedia.progress =
            (mediaPlayer?.currentPosition ?: 0).toFloat() / playingMedia.duration
        position?.let {
            getHolder(it)?.pause()
        }
        mediaPlayer?.pause()
        playingMedia.state = PlayingMedia.STATE_PAUSED
        stopTimer()
    }

    private fun stopSong(
        position: Int? = playingMedia.playingPosition,
    ) {
        logD(TAG, "stopSong: $position")
        position?.let {
            getHolder(it)?.stop()
        }
        mediaPlayer?.reset()
        playingMedia.reset()
        stopTimer()
    }

    private fun getHolder(position: Int): SongAdapter.SongViewHolder? {
        return recyclerView.findViewHolderForAdapterPosition(position) as SongAdapter.SongViewHolder?
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.searchButton -> {
                searchEditText.text.takeIf { !TextUtils.isEmpty(it) }?.let {
                    if (firstSearch) {
                        firstSearch = false
                        ObjectAnimator().apply {
                            duration = 400
                            setPropertyName("alpha")
                            setFloatValues(0.6f, 0.2f)
                            target = logoBg
                        }.start()
                    }
                    search(it.toString())
                }
            }
            R.id.topButton -> recyclerView.smoothScrollToPosition(0)
            R.id.historyButton -> {
            }
        }
    }

    private fun search(text: String) {
        adapter.songs.clear()
        adapter.notifyDataSetChanged()
        playingMedia.reset()
        mediaPlayer?.reset()
        hideEmptyView()
        loadingAnimView.visibility = View.VISIBLE
        params["q"] = text
        service.search(params).enqueue(searchCallback)
    }

    private fun setLightStatusBar(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var flags = view.systemUiVisibility
            flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            view.systemUiVisibility = flags
            window.statusBarColor = Color.WHITE
        }
    }

    private fun clearLightStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val window = window
            window.statusBarColor = Color.BLACK
        }
    }

    data class PlayingMedia(
        var playingSong: Song? = null,
        var playingPosition: Int? = null,
        var progress: Float = 0f, // 0->1
        var duration: Int = -1,
        var state: Int = STATE_STOPPED
    ) {
        fun reset() {
            playingSong = null
            playingPosition = null
            duration = -1
            progress = 0f
            state = STATE_STOPPED
        }

        companion object {
            const val STATE_PLAYING = 0
            const val STATE_PAUSED = 1
            const val STATE_STOPPED = 2
            const val STATE_PREPARING = 3
        }
    }
}