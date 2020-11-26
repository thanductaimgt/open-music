package com.mgt.openmusic

import android.app.Activity
import android.view.Gravity
import android.widget.Toast
import kotlinx.android.synthetic.main.toast_view.view.*

object MyToast {
    fun show(activity: Activity, text:String){
        Toast(activity).apply {
            view = activity.layoutInflater.inflate(R.layout.toast_view, activity.findViewById(R.id.toastRoot), false).apply {
                toastTV.text = text
            }
            setGravity(Gravity.TOP, 0, 64)
            duration = Toast.LENGTH_SHORT
            show()
        }
    }
}