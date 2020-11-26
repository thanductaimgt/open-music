package com.mgt.openmusic

interface Event

data class PlayEvent(val position: Int) : Event

data class UpdateDurationEvent(val position: Int) : Event

data class CompleteEvent(val position: Int) : Event

data class SearchSuccess(val songs: ArrayList<Song>) : Event

data class LoadMoreSuccess(val songs: ArrayList<Song>) : Event

object LoadMoreFail : Event

object SearchFail : Event

object TimerUpdate : Event