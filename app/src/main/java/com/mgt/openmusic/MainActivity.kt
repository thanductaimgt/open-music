package com.mgt.openmusic

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.mgt.openmusic.rxjava.MyCompletableObserver
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.song_play_view.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round


class MainActivity : BaseActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var songAdapter: SongAdapter
    private lateinit var searchHistoryAdapter: SearchHistoryAdapter
    private lateinit var layoutManager: LinearLayoutManager
    private val bottomSheetDialog = DownloadBottomSheet()

    private val songAdapterClickListener = object : SongAdapter.ClickListener {
        override fun onDownload(position: Int) {
            val song = songAdapter.songs[position]
            viewModel.downloadSong(
                song,
                DownloadObserver(position)
            )
            playDownloadingAnimation()
        }

        override fun onOpen(position: Int) {

        }

        override fun onCancel(position: Int) {
            cancelDownload(position)
        }

        override fun onShare(position: Int) {
            val song = songAdapter.songs[position]
            shareSong(song)
        }

        override fun onPlay(position: Int) {
            stopSong()
            val song = songAdapter.songs[position]
            prepareSong(song)
        }
    }

    private val searchHistoryAdapterClickListener = object : SearchHistoryAdapter.ClickListener {
        override fun onDelete(position: Int) {
            val searchHistory = searchHistoryAdapter.searchHistories[position]
            viewModel.removeSearchHistory(searchHistory)
        }

        override fun onSelect(position: Int) {
            val searchHistory = searchHistoryAdapter.searchHistories[position]
            searchEditText.setText(searchHistory.text)
            search()
            hideSearchHistoryView()
        }
    }

    private var isShowAnimRunning = false
    private val showAnim = ObjectAnimator().apply {
        duration = 200
        setFloatValues(0f, 1f)
        setPropertyName("alpha")
        doOnStart {
            if (isShowAnimRunning) {
                (target as View).visibility = View.VISIBLE
            }
        }
        doOnEnd {
            if (!isShowAnimRunning) {
                (target as View).visibility = View.INVISIBLE
            }
        }
    }

    private val songPlayEventListener = object : SongPlayView.EventListener {
        override fun onThumbClick() {
            when (songPlayView.playState) {
                SongPlayView.PlayState.STATE_PREPARING -> {
                    stopSong()
                }
                SongPlayView.PlayState.STATE_PLAYING -> {
                    pauseSong()
                }
                SongPlayView.PlayState.STATE_PAUSED -> {
                    resumeSong()
                }
                SongPlayView.PlayState.STATE_COMPLETE -> {
                    replaySong()
                }
                SongPlayView.PlayState.STATE_STOPPED -> {
                    viewModel.focusedMedia.song?.let { prepareSong(it) }
                }
            }
        }

        override fun onClick() {

        }

        override fun onSongProgressSelected(progress: Float) {
            viewModel.apply {
                if (focusedMedia.playState == SongPlayView.PlayState.STATE_PLAYING || focusedMedia.playState == SongPlayView.PlayState.STATE_PAUSED) {
                    val offset = round(progress * focusedMedia.duration).toInt() * 1000
                    mediaPlayer.seekTo(offset)
                } else {
                    focusedMedia.progress = progress
                }
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
            hideSearchHistoryView()
            val lastPosition = layoutManager.findLastVisibleItemPosition()
            if (lastPosition >= songAdapter.songs.size - 5 && viewModel.shouldLoadMoreSongs()) {
                loadMoreAnimView.visibility = View.VISIBLE
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
        makeRoomForStatusBar(this, rootView, MAKE_ROOM_TYPE_PADDING)

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
        observeData()
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
        layoutManager = songRecyclerView.layoutManager as LinearLayoutManager
        songAdapter = SongAdapter(songAdapterClickListener)
        songRecyclerView.adapter = songAdapter
        songRecyclerView.addOnScrollListener(scrollListener)

        searchHistoryAdapter = SearchHistoryAdapter(searchHistoryAdapterClickListener)
        searchHistoryRv.adapter = searchHistoryAdapter

        searchButton.setOnClickListener(this)
        topButton.setOnClickListener(this)
        historyButton.setOnClickListener(this)

        searchEditText.apply {
            setOnFocusChangeListener { _, b ->
                if (b) {
                    searchEditText.post {
                        Utils.showKeyboard(this@MainActivity, searchEditText)
                        showSearchHistoryView()
                    }
                } else {
                    Utils.hideKeyboard(this@MainActivity, rootView)
                    hideSearchHistoryView()
                }
            }
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    search()
                    true
                } else {
                    false
                }
            }
            requestFocus()
        }
        searchEditText.setOnClickListener(this)
        downloadListAnimView.setOnClickListener(this)

        songPlayView.eventListener = songPlayEventListener
        bindSongPlayView()
        bindTopButton()

        appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            titleLayout.alpha = 1 - min(1f, -verticalOffset.toFloat() / toolBar.minimumHeight)
        })
    }

    private fun bindTopButton() {
        if (viewModel.isTopButtonShown) show(topButton) else hide(topButton)
    }

    private fun bindSongPlayView() {
        viewModel.focusedMedia.song.ifNotNull { song ->
            songPlayView.apply {
                songName = song.title
                artistName = song.artist
                thumbUrl = song.album?.thumb?.photo_68
                duration = song.duration
            }

            songPlayView.visibility = View.VISIBLE

            moveToTop(topButton, 64)
            moveToTop(loadMoreAnimView, 81)
            moveToTop(downloadListAnimView, 64)
        }.otherwise {
            songPlayView.visibility = View.GONE

            moveToTop(topButton, 8)
            moveToTop(loadMoreAnimView, 25)
            moveToTop(downloadListAnimView, 8)
        }
    }

    private fun moveToTop(view: View, dp: Int) {
        view.layoutParams =
            (view.layoutParams as ViewGroup.MarginLayoutParams).apply {
                setMargins(0, 0, 0, Utils.dp(dp.toFloat()))
            }
    }

    private fun observeEvents() {
        viewModel.liveEvent.observe(this, {
            logD(TAG, "Receive event: $it")
            when (it) {
                is PlayEvent -> songPlayView.play()
                is UpdateDurationEvent -> songPlayView.duration = it.duration
                is CompleteEvent -> songPlayView.complete()
                is SearchSuccess -> {
                    loadingAnimView.visibility = View.INVISIBLE
                    if (it.songs.isEmpty()) {
                        showEmptyView()
                    }
                }
                is SearchFail -> {
                    loadingAnimView.visibility = View.INVISIBLE
                    showEmptyView()
                }
                is LoadMoreSuccess, LoadMoreFail -> loadMoreAnimView.visibility = View.INVISIBLE
                is TimerUpdate -> {
                    viewModel.apply {
                        if (focusedMedia.playState == SongPlayView.PlayState.STATE_PLAYING) {
                            songPlayView.progress =
                                mediaPlayer.currentPosition / (focusedMedia.duration * 1000f)
                            focusedMedia.progress = songPlayView.progress
                        }
                    }
                }
            }
        })
    }

    private fun observeData() {
        viewModel.liveSongs.observe(this, {
            songAdapter.songs = it
            songAdapter.notifyDataSetChanged()
        })

        viewModel.liveSearchHistories.observe(this, {
            searchHistoryAdapter.searchHistories.apply {
                clear()
                addAll(it)
                reverse()
            }
            searchHistoryAdapter.notifyDataSetChanged()
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

    private fun rotateLogoBg(degree: Float) {
        logoBg.rotation += degree
    }

    private fun hide(view: View) {
        isShowAnimRunning = false
        showAnim.target = view
        showAnim.reverse()
    }

    private fun show(view: View) {
        isShowAnimRunning = true
        showAnim.target = view
        showAnim.start()
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

    private fun cancelDownload(position: Int) {
        viewModel.downloadDisposableMap[position]?.dispose()
        viewModel.downloadDisposableMap.remove(position)
        getHolder(position)?.finishDownload(false)
    }

    private fun prepareSong(song: Song) {
        logD(TAG, "prepareSong: $song")
        viewModel.apply {
            focusedMedia.song = song
            focusedMedia.progress = 0f
            focusedMedia.playState = SongPlayView.PlayState.STATE_PREPARING
            mediaPlayer.apply {
                setDataSource(song.url)
                prepareAsync()
            }
        }
        bindSongPlayView()
        songPlayView.prepare()
    }

    private fun resumeSong() {
        logD(TAG, "resumeSong")
        songPlayView.resume()
        viewModel.apply {
            mediaPlayer.start()
            focusedMedia.playState = SongPlayView.PlayState.STATE_PLAYING
            startTimer()
        }
    }

    private fun pauseSong() {
        logD(TAG, "pauseSong")
        viewModel.apply {
            songPlayView.pause()
            focusedMedia.progress =
                mediaPlayer.currentPosition / (focusedMedia.duration * 1000f)
            mediaPlayer.pause()
            focusedMedia.playState = SongPlayView.PlayState.STATE_PAUSED
            stopTimer()
        }
    }

    private fun stopSong() {
        logD(TAG, "stopSong")
        viewModel.apply {
            songPlayView.stop()
            mediaPlayer.reset()
//            focusedMedia.reset()
            stopTimer()
        }
    }

    private fun replaySong() {
        logD(TAG, "replaySong")
        songPlayView.apply {
            progress = 0f
            resume()
        }
        viewModel.apply {
            focusedMedia.playState = SongPlayView.PlayState.STATE_PLAYING
            mediaPlayer.start()
            startTimer()
        }
    }

    private fun getHolder(position: Int): SongAdapter.SongViewHolder? {
        return songRecyclerView.findViewHolderForAdapterPosition(position) as SongAdapter.SongViewHolder?
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.searchButton -> {
                search()
            }
            R.id.topButton -> {
                if (layoutManager.findLastVisibleItemPosition() < 50) {
                    songRecyclerView.smoothScrollToPosition(0)
                } else {
                    songRecyclerView.scrollToPosition(0)
                    hide(topButton)
                }
                appBar.setExpanded(true)
            }
            R.id.historyButton -> {
            }
            R.id.searchEditText -> showSearchHistoryView()
            R.id.downloadListAnimView -> bottomSheetDialog.show(
                supportFragmentManager,
                DownloadBottomSheet::class.java.simpleName
            )
        }
    }

    private fun showSearchHistoryView() {
        searchHistoryRv.apply {
            if (visibility != View.VISIBLE) {
                visibility = View.VISIBLE
                scrollToPosition(0)
            }
        }
    }

    private fun hideSearchHistoryView() {
        searchHistoryRv.visibility = View.INVISIBLE
    }

    private fun playDownloadingAnimation() {
        downloadListAnimView.playAnimation()
    }

    private fun stopDownloadingAnimation() {
        downloadListAnimView.cancelAnimation()
        downloadListAnimView.progress = 0f
    }

    private fun search() {
        searchEditText.text.takeIf { !TextUtils.isEmpty(it) }?.toString()?.let { keyword ->
            logD(TAG, "search keyword: $keyword")
            if (viewModel.firstSearch) {
                viewModel.firstSearch = false
                ObjectAnimator().apply {
                    duration = 400
                    setPropertyName("alpha")
                    setFloatValues(0.6f, 0.2f)
                    target = logoBg
                }.start()
            }

            hideSearchHistoryView()
            hideEmptyView()
            loadingAnimView.visibility = View.VISIBLE

            viewModel.liveSongs.apply {
                value?.clear()
                notifyObservers()
            }
            viewModel.searchSongs(keyword)
        }
    }

    // TODO: 17-Dec-20 fix memory leak and of using inner class
    inner class DownloadObserver(private val position: Int) :
        MyCompletableObserver(viewModel.compositeDisposable) {
        override fun onSubscribe(d: Disposable) {
            super.onSubscribe(d)
            logD(TAG, "onSubscribe: position: $position")
            viewModel.downloadDisposableMap[position] = d
            getHolder(position)?.startDownload()
        }

        override fun onComplete() {
            super.onComplete()
            logD(TAG, "onComplete: position: $position")
            viewModel.downloadDisposableMap.remove(position)
            getHolder(position)?.finishDownload(true)
            stopDownloadingAnimation()
        }

        override fun onError(t: Throwable) {
            super.onError(t)
            logD(TAG, "onError: position: $position")
            viewModel.downloadDisposableMap.remove(position)
            getHolder(position)?.finishDownload(false)
            Toast.makeText(this@MainActivity, "A download fail", Toast.LENGTH_SHORT).show()
        }
    }
}