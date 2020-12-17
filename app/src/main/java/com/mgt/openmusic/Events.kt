package com.mgt.openmusic

interface Event

data class PlayEvent(val song:Song) : Event

data class UpdateDurationEvent(val duration:Int) : Event

object CompleteEvent : Event

data class SearchSuccess(val songs: ArrayList<Song>) : Event

data class LoadMoreSuccess(val songs: ArrayList<Song>) : Event

object LoadMoreFail : Event

object SearchFail : Event

object TimerUpdate : Event