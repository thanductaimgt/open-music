package com.mgt.openmusic

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val url: String,
    var duration: Int, //sec
    val album: Album? = null,
    @Transient
    var state: Int = STATE_IDLE,
) {
    companion object {
        const val STATE_IDLE = 0
        const val STATE_PREPARING = 1
        const val STATE_PLAYING = 2
        const val STATE_PAUSED = 3
        const val STATE_COMPLETE = 4

        const val PAYLOAD_PROGRESS = 10
    }
}

data class Album(
    val title: String? = null,
    val thumb: Thumb? = null,
)

data class Thumb(
    val photo_68: String? = null,
)