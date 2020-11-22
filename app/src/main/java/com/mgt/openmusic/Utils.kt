package com.mgt.openmusic

import android.content.Context
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager

object Utils {
    fun getDurationFormat(duration: Int): String {
        var time = duration.toLong()
        val res = StringBuilder()

        if (time > 3600) {
            val hours = time / 3600
            res.append("${if (hours < 10) "0" else ""}$hours:")
            time -= hours * 3600
        }

        val mins = time / 60
        res.append("${if (mins < 10) "0" else ""}$mins:")
        time -= mins * 60

        val secs = time
        res.append("${if (secs < 10) "0" else ""}$secs")
        return res.toString()
    }

    fun hideKeyboard(context: Context, view: View) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
        view.clearFocus()
    }

    fun showKeyboard(context: Context, view: View) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }
}

fun logD(tag: String, message: String) {
    if (BuildConfig.DEBUG) {
        Log.d(tag, message)
    }
}

fun logE(tag: String, message: String) {
    if (BuildConfig.DEBUG) {
        Log.e(tag, message)
    }
}

val Any.TAG: String
    get() = this::class.java.simpleName