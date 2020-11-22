package com.mgt.openmusic

data class Song(
    val title: String,
    val artist: String,
    val url: String,
    val duration: Int,
    val album: Album?=null
)

data class Album(
    val thumb: Thumb?=null
)

data class Thumb(
    val photo_68: String?=null
)