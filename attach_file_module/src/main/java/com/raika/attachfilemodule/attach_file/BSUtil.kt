package com.raika.attachfilemodule.attach_file

import android.view.View

object BSUtil {
    fun controlShadow(slideOffset: Float, vShadow: View) {
        if (slideOffset == 0f) {
            vShadow.visibility = View.GONE
        } else {
            vShadow.visibility = View.VISIBLE
        }
    }
}