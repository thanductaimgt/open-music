package com.mgt.openmusic

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.max


class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var adapter: SongAdapter

    private val adapterClickListener = { view: View, position: Int ->
        val song = adapter.songs[position]
        when (view.id) {
            R.id.downloadButton -> downloadSong(song)
            R.id.shareButton -> shareSong(song)
            R.id.thumbImgView -> {
                when (song.state) {
                    Song.STATE_PLAYING, Song.STATE_PREPARING -> {
                        pauseSong(position)
                    }
                    Song.STATE_PAUSED -> {
                        resumeSong(position)
                    }
                    Song.STATE_IDLE -> {
                        stopSong()
                        playSong(position)
                    }
                    Song.STATE_COMPLETE -> {
                        replaySong()
                    }
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

    private val seekBarProgressListener = { progress: Float ->
        viewModel.apply {
            if (focusedMedia.state == Song.STATE_PLAYING || focusedMedia.state == Song.STATE_PAUSED) {
                val offset = (progress / 100f * focusedMedia.duration * 1000).toInt()
                mediaPlayer.seekTo(offset)
            } else {
                focusedMedia.progress = progress / 100f
            }
        }
    }

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            viewModel.apply {
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
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            rotateLogoBg(dy / 100f)
            Utils.hideKeyboard(this@MainActivity, rootView)
            val lastPosition =
                (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
            if (lastPosition >= adapter.songs.size - 5) {
                viewModel.loadMoreSongs()
            }
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

        if (!viewModel.isStartAnimRun) {
            viewModel.isStartAnimRun = true
            runAnimation()
        } else {
            logoBg.alpha = if (viewModel.firstSearch) 0.6f else 0.2f
        }

        initView()

        observeEvents()
        observeSongs()
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
        adapter = SongAdapter(adapterClickListener, seekBarProgressListener)
        recyclerView.adapter = adapter
        recyclerView.addOnScrollListener(scrollListener)

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

    private fun observeEvents() {
        viewModel.liveEvent.observe(this, {
            when (it) {
                is PlayEvent -> getHolder(it.position)?.play()
                is CompleteEvent -> getHolder(it.position)?.complete()
                is SearchSuccess -> {
                    loadingAnimView.visibility = View.INVISIBLE
                }
                is SearchFail -> {
                    loadingAnimView.visibility = View.INVISIBLE
                    showEmptyView()
                }
                is TimerUpdate -> {
                    viewModel.apply {
                        focusedMedia.position?.let { position ->
                            getHolder(position)?.let { holder ->
                                if (focusedMedia.state == Song.STATE_PLAYING) {
                                    holder.seekBar().progress =
                                        mediaPlayer.currentPosition / focusedMedia.duration
                                }
                            } ?: logE(TAG, "holder null")
                        } ?: logE(TAG, "playingPosition null")
                    }
                }
            }
        })
    }

    private fun observeSongs() {
        viewModel.liveSongs.observe(this, {
            adapter.songs = it
            adapter.notifyDataSetChanged()
            if (it.isEmpty()) {
                showEmptyView()
            }
        })
    }

//    override fun onStart() {
//        super.onStart()
//        if (viewModel.focusedMedia.state == Song.STATE_PAUSED) {
//            resumeSong()
//        }
//    }
//
//    override fun onStop() {
//        super.onStop()
//        viewModel.apply {
//            if (focusedMedia.state == Song.STATE_PLAYING || focusedMedia.state == Song.STATE_PREPARING) {
//                pauseSong()
//            }
//        }
//    }

//    override fun onDestroy() {
//        super.onDestroy()
//    }

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

    private fun playSong(position: Int? = viewModel.focusedMedia.position, progress: Float = 0f) {
        logD(TAG, "playSong: $position")
        position?.let {
            getHolder(it)?.prepare()
            adapter.songs[it].let { song ->
                viewModel.apply {
                    focusedMedia.position = position
                    focusedMedia.song = song
                    focusedMedia.state = Song.STATE_PREPARING
                    focusedMedia.progress = progress
                    mediaPlayer.apply {
                        setDataSource(song.url)
                        prepareAsync()
                    }
                }
            }
        }
    }

    private fun resumeSong(position: Int? = viewModel.focusedMedia.position) {
        logD(TAG, "resumeSong: $position")
        position?.let {
            getHolder(it)?.resume()
            viewModel.apply {
                mediaPlayer.start()
                focusedMedia.state = Song.STATE_PLAYING
                startTimer()
            }
        }
    }

    private fun pauseSong(
        position: Int? = viewModel.focusedMedia.position
    ) {
        logD(TAG, "pauseSong: $position")
        viewModel.apply {
            position?.let {
                getHolder(it)?.pause()
                focusedMedia.progress =
                    mediaPlayer.currentPosition / (focusedMedia.duration * 1000f)
                mediaPlayer.pause()
                focusedMedia.state = Song.STATE_PAUSED
                stopTimer()
            }
        }
    }

    private fun stopSong(
        position: Int? = viewModel.focusedMedia.position,
    ) {
        logD(TAG, "stopSong: $position")
        position?.let {
            getHolder(it)?.stop()
            viewModel.apply {
                mediaPlayer.reset()
                focusedMedia.reset()
                stopTimer()
            }
        }
    }

    private fun replaySong(
        position: Int? = viewModel.focusedMedia.position,
    ) {
        logD(TAG, "replaySong: $position")
        position?.let {
            getHolder(it)?.apply {
                seekBar().progress = 0
                resume()
            }
            viewModel.apply {
                focusedMedia.state = Song.STATE_PLAYING
                mediaPlayer.start()
                startTimer()
            }
        }
    }

    private fun getHolder(position: Int): SongAdapter.SongViewHolder? {
        return recyclerView.findViewHolderForAdapterPosition(position) as SongAdapter.SongViewHolder?
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.searchButton -> {
                searchEditText.text.takeIf { !TextUtils.isEmpty(it) }?.let {
                    if (viewModel.firstSearch) {
                        viewModel.firstSearch = false
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
        hideEmptyView()
        loadingAnimView.visibility = View.VISIBLE
        viewModel.search(text)
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
}