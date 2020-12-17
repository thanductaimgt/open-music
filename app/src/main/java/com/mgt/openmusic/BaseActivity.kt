package com.mgt.openmusic

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntDef
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.*

abstract class BaseActivity:AppCompatActivity(), View.OnClickListener {
    protected fun setLightStatusBar(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var flags = view.systemUiVisibility
            flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            view.systemUiVisibility = flags
            window.statusBarColor = Color.WHITE
        }
    }

    protected fun clearLightStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val window = window
            window.statusBarColor = Color.BLACK
        }
    }

    protected fun makeRoomForStatusBar(context: Context, targetView: View, @MakeRoomType makeRoomType: Int = MAKE_ROOM_TYPE_PADDING) {
        var statusBarHeight: Int
        val resourceId: Int = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusBarHeight = context.resources.getDimensionPixelSize(resourceId)

            makeRoomForStatusBarInternal(targetView, statusBarHeight, makeRoomType)
        } else {
            var isInvoked = false

            ViewCompat.setOnApplyWindowInsetsListener(targetView) { _, insets ->
                insets.also {
                    if (!isInvoked) {
                        isInvoked = true
                        statusBarHeight = insets.systemWindowInsetTop

                        makeRoomForStatusBarInternal(targetView, statusBarHeight, makeRoomType)
                    }
                }
            }
        }
//        val rectangle = Rect()
//        activity.window.decorView.getWindowVisibleDisplayFrame(rectangle)
//        val statusBarHeight: Int = rectangle.top
////        val contentViewTop: Int = activity.window.findViewById<View>(Window.ID_ANDROID_CONTENT).top
////        val titleBarHeight = contentViewTop - statusBarHeight
//        rootView.updatePadding(top = statusBarHeight)
    }

    private fun makeRoomForStatusBarInternal(targetView: View, statusBarHeight: Int, @MakeRoomType makeRoomType: Int = MAKE_ROOM_TYPE_PADDING) {
        if (makeRoomType == MAKE_ROOM_TYPE_PADDING) {
            targetView.updatePadding(top = targetView.paddingTop + statusBarHeight)
        } else {
            targetView.updateLayoutParams {
                this as ViewGroup.MarginLayoutParams
                updateMargins(top = targetView.marginTop + statusBarHeight)
            }
        }
    }

    @IntDef(MAKE_ROOM_TYPE_PADDING, MAKE_ROOM_TYPE_MARGIN)
    @Retention(AnnotationRetention.SOURCE)
    annotation class MakeRoomType

    companion object {
        const val MAKE_ROOM_TYPE_PADDING = 0
        const val MAKE_ROOM_TYPE_MARGIN = 1
    }
}