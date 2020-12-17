package com.mgt.openmusic

import android.content.ContentValues
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mgt.openmusic.rxjava.MyCompletableObserver
import com.mgt.openmusic.rxjava.MySingleObserver
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.CompletableEmitter
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashSet

class MainViewModel : ViewModel() {
    val liveEvent = SingleLiveEvent<Event>()
    val liveSongs = MutableLiveData<ArrayList<Song>>(ArrayList())
    val liveSearchHistories = MutableLiveData<MutableSet<SearchHistory>>(LinkedHashSet())
    val compositeDisposable = CompositeDisposable()
    val compositeSubscription = CompositeSubscription()
    val downloadDisposableMap = HashMap<Int, Disposable>()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://myfreemp3.vip/")
        .addConverterFactory(ScalarsConverterFactory.create())
        .build()
    private val service = retrofit.create(ApiService::class.java)
    private val params = hashMapOf(
        "page" to "0"
    )

    var isTopButtonShown = false

    val focusedMedia = SongMedia()

    var firstSearch = true

    lateinit var mediaPlayer: MediaPlayer
    private var timer: Timer? = null

    var isStartAnimRun = false

    var isLoadingMore = false
    var isSearching = false
    var lastResponseSongsSize = 0

    private val prepareListener = MediaPlayer.OnPreparedListener {
        if (focusedMedia.duration <= 0) {
            focusedMedia.duration = it.duration / 1000
            liveEvent.value = UpdateDurationEvent(focusedMedia.duration)
        }
        logD(TAG, "source prepared, offset: ${focusedMedia.progress}")
        it.seekTo((focusedMedia.progress * focusedMedia.duration * 1000).toInt())
        if (focusedMedia.playState == SongPlayView.PlayState.STATE_PREPARING) {
            it.start()
            startTimer()
            focusedMedia.song?.let { song ->
                liveEvent.value = PlayEvent(song)
            }
            focusedMedia.playState = SongPlayView.PlayState.STATE_PLAYING
        }
    }

    private val completeListener = MediaPlayer.OnCompletionListener {
        logD(TAG, "song completed")
        liveEvent.value = CompleteEvent
        focusedMedia.playState = SongPlayView.PlayState.STATE_COMPLETE
        stopTimer()
    }

    private val searchObserver = object : MySingleObserver<ArrayList<Song>>(compositeDisposable) {
        override fun onSuccess(result: ArrayList<Song>) {
            super.onSuccess(result)
            try {
                logD(TAG, "search results ${result.size} songs: $result")
                liveEvent.value = SearchSuccess(result)
                liveSongs.value = result
                lastResponseSongsSize = result.size
                isSearching = false
            } catch (t: Throwable) {
                onError(t)
            }
        }

        override fun onError(t: Throwable) {
            super.onError(t)
            liveEvent.value = SearchFail
            lastResponseSongsSize = 0
            isSearching = false
        }
    }

    private val loadMoreObserver = object : MySingleObserver<ArrayList<Song>>(compositeDisposable) {
        override fun onSuccess(result: ArrayList<Song>) {
            super.onSuccess(result)
            try {
                logD(TAG, "load more ${result.size} songs: $result")
                liveEvent.value = LoadMoreSuccess(result)
                liveSongs.apply {
                    value?.addAll(result)
                    notifyObservers()
                }
                lastResponseSongsSize = result.size
                isLoadingMore = false
            } catch (t: Throwable) {
                onError(t)
            }
        }

        override fun onError(t: Throwable) {
            super.onError(t)
            liveEvent.value = LoadMoreFail
            lastResponseSongsSize = 0
            isLoadingMore = false
        }
    }

    private fun parseSongs(response: Response<String>): ArrayList<Song> {
        val body = response.body().toString()
        return if (body.length > 26) {
            val json =
                "[${response.body().toString().let { it.substring(23, it.length - 3) }}"
            Gson().fromJson<List<Song>>(
                json,
                object : TypeToken<List<Song>>() {}.type
            ).toMutableList() as ArrayList
        } else {
            ArrayList()
        }
    }

    init {
        initMediaPlayer()
        loadSearchHistories()
    }

    private fun initMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setOnPreparedListener(prepareListener)
            setOnCompletionListener(completeListener)
        }
    }

    fun startTimer() {
        timer?.cancel()
        timer = Timer().also {
            it.schedule(object : TimerTask() {
                override fun run() {
                    liveEvent.postValue(TimerUpdate)
                }
            }, 0, 1000)
        }
    }

    fun stopTimer() {
        timer?.cancel()
        timer = null
    }

    fun searchSongs(keyword: String) {
        isSearching = true
        params["q"] = keyword
        params["page"] = "0"

        // save to permanent

        liveSearchHistories.value?.apply {
            val searchHistory = SearchHistory(keyword)
            remove(searchHistory)
            add(searchHistory)
            liveSearchHistories.notifyObservers()
        }

        Single.fromCallable {
            parseSongs(service.search(params).execute())
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(searchObserver)
    }

    fun downloadSong(song: Song, observer: MyCompletableObserver) {
        Completable.create { emitter ->
            downloadSongInternal(song, emitter)
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(observer)
    }

    private fun downloadSongInternal(song: Song, emitter: CompletableEmitter) {
        MyApp.context.apply {
            val relativeLocation: String = Environment.DIRECTORY_MUSIC

            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.AudioColumns.TITLE, song.title)
                put(MediaStore.Audio.AudioColumns.DISPLAY_NAME, song.title)
                song.album?.title?.let { put(MediaStore.Audio.AudioColumns.ALBUM, it) }
                put(MediaStore.Audio.AudioColumns.ARTIST, song.artist)
                put(MediaStore.Audio.AudioColumns.DURATION, song.duration * 1000)
                put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativeLocation)
            }

            var output: OutputStream? = null
            var input: InputStream? = null
            var uri: Uri? = null

            try {
                val contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                uri = contentResolver.insert(contentUri, contentValues)!!
                output = contentResolver.openOutputStream(uri)!!

                val response = service.downloadAudio(song.url).execute()
                input = response.body()!!.byteStream()

                val data = ByteArray(4096)
                var count = input.read(data)

                while (count != -1) {
                    output.write(data, 0, count)
                    count = input.read(data)
                }
                emitter.onComplete()
            } catch (t: Throwable) {
                if (uri != null) {
                    // Don't leave an orphan entry in the MediaStore
                    contentResolver.delete(uri, null, null)
                }
                emitter.tryOnError(t)
            } finally {
                input?.close()
                output?.close()
            }
        }
    }

    fun loadMoreSongs() {
        isLoadingMore = true
        params["page"] = (params["page"]!!.toInt() + 1).toString()
        logD(TAG, "loading more songs - page:${params["page"]}")

        Single.fromCallable {
            parseSongs((service.search(params).execute()))
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(loadMoreObserver)
    }

    fun shouldLoadMoreSongs(): Boolean {
        return !isLoadingMore && !isSearching && lastResponseSongsSize > 0
    }

    private fun loadSearchHistories() {
        liveSearchHistories.value?.addAll(
            arrayListOf(
                SearchHistory("thuy chi"),
                SearchHistory("blackpink"),
                SearchHistory("chipu"),
                SearchHistory("justin bieber"),
                SearchHistory("imagine dragon"),
                SearchHistory("twenty one pilots"),
                SearchHistory("rihanna"),
                SearchHistory("orianna"),
                SearchHistory("nguyen tran trung quoc"),
                SearchHistory("vu cat tuong"),
                SearchHistory("masew"),
                SearchHistory("dat g"),
            )
        )
        liveSearchHistories.notifyObservers()
    }

    fun removeSearchHistory(searchHistory: SearchHistory) {
        // remove search history permanent

        liveSearchHistories.value?.remove(searchHistory)
        liveSearchHistories.notifyObservers()
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer.release()
        compositeDisposable.clear()
        compositeSubscription.clear()
    }

    data class SongMedia(
        var song: Song? = null,
        var progress: Float = 0f, // 0->1
    ) {
        var playState = SongPlayView.PlayState.STATE_STOPPED

        // second
        var duration: Int = song?.duration ?: 0
            get() = song?.duration ?: 0
            set(value) {
                song?.duration = value
                field = value
            }

        fun reset() {
            playState = SongPlayView.PlayState.STATE_STOPPED
            song = null
            progress = 0f
        }
    }
}