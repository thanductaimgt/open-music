package com.mgt.openmusic

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.*
import kotlin.collections.ArrayList

class MainViewModel : ViewModel() {
    val liveEvent = SingleLiveEvent<Event>()
    val liveSongs = MutableLiveData<ArrayList<Song>>()

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

    private val prepareListener = MediaPlayer.OnPreparedListener {
        logD(TAG, "source prepared, offset: ${focusedMedia.progress}")
        it.seekTo((focusedMedia.progress * focusedMedia.duration * 1000).toInt())
        if (focusedMedia.state == Song.STATE_PREPARING) {
            it.start()
            startTimer()
            focusedMedia.position?.let { position ->
                liveEvent.value = PlayEvent(position)
            }
            focusedMedia.state = Song.STATE_PLAYING
        }
    }

    private val completeListener = MediaPlayer.OnCompletionListener {
        logD(TAG, "song completed: ${focusedMedia.position}")
        focusedMedia.position?.let { position ->
            liveEvent.value = CompleteEvent(position)
        }
        focusedMedia.state = Song.STATE_COMPLETE
        stopTimer()
    }

    private val searchCallback = object : Callback<String> {
        override fun onResponse(call: Call<String>, response: retrofit2.Response<String>) {
            try {
                val body = response.body().toString()
                val songs = if (body.length > 26) {
                    val json =
                        "[${response.body().toString().let { it.substring(23, it.length - 3) }}"
                    Gson().fromJson<List<Song>>(
                        json,
                        object : TypeToken<List<Song>>() {}.type
                    ).toMutableList() as ArrayList
                } else {
                    ArrayList()
                }
                liveEvent.value = SearchSuccess(songs)
                liveSongs.value = songs
            } catch (t: Throwable) {
                onFailure(call, t)
            }
        }

        override fun onFailure(call: Call<String>, t: Throwable) {
            t.print()
            liveEvent.value = SearchFail
        }
    }

    init {
        initMediaPlayer()
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

    fun search(text: String) {
        focusedMedia.reset()
        mediaPlayer.reset()
        params["q"] = text
        service.search(params).enqueue(searchCallback)
    }

    fun loadMoreSongs() {

    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer.release()
    }

    data class SongMedia(
        var song: Song? = null,
        var position: Int? = null,
        var progress: Float = 0f, // 0->1
    ) {
        var state: Int = song?.state ?: Song.STATE_IDLE
            get() = song?.state ?: Song.STATE_IDLE
            set(value) {
                song?.state = value
                field = value
            }

        val duration: Int
            get() = song?.duration ?: -1

        fun reset() {
            state = Song.STATE_IDLE
            song = null
            position = null
            progress = 0f
        }
    }
}