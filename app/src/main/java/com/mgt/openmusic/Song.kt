package com.mgt.openmusic

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val url: String,
    var duration: Int, //sec
    val album: Album? = null,
)

data class Album(
    val title: String? = null,
    val thumb: Thumb? = null,
)

data class Thumb(
    val photo_68: String? = null,
)