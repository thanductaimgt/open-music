package com.mgt.openmusic

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Resources.Theme
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.lifecycle.MutableLiveData
import com.airbnb.lottie.LottieAnimationView


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

    @ColorInt
    fun getAttrColor(theme: Theme, @AttrRes attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    private fun convertDipsToPixels(context: Context, dipValue: Float): Int {
        return (0.5f + dipValue * context.resources.displayMetrics.density).toInt()
    }

    fun dp(dipValue: Float): Int {
        return convertDipsToPixels(MyApp.context, dipValue)
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

fun Throwable.print() {
    if (BuildConfig.DEBUG) {
        printStackTrace()
    }
}

fun <T> MutableLiveData<T>.notifyObservers() {
    value = value
}

inline fun <T> T?.ifNotNull(block: (it: T) -> Unit): Otherwise {
    return if (this != null) {
        block(this as T)
        OtherwiseNotExecute
    } else {
        OtherwiseExecute
    }
}

interface Otherwise {
    fun otherwise(block: () -> Unit)
}

object OtherwiseExecute : Otherwise {
    override fun otherwise(block: () -> Unit) {
        block()
    }
}

object OtherwiseNotExecute : Otherwise {
    override fun otherwise(block: () -> Unit) = Unit
}

fun ValueAnimator.resumeOrStart(){
    if(isPaused) {
        resume()
    }else{
        start()
    }
}

fun LottieAnimationView.playForward(){
    speed = 1f
    playAnimation()
}

fun LottieAnimationView.playBackward(){
    speed = -1f
    playAnimation()
}