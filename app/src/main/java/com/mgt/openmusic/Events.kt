package com.mgt.openmusic

interface Event

class PlayEvent(val position: Int) : Event

class CompleteEvent(val position: Int) : Event

class SearchSuccess(val songs: ArrayList<Song>) : Event

object SearchFail : Event

object TimerUpdate : Event