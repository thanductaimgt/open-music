package com.mgt.openmusic

import android.app.Application
import android.content.Context

class MyApp:Application() {
    override fun onCreate() {
        super.onCreate()
        context = this
    }

    companion object{
        lateinit var context: Context
    }
}