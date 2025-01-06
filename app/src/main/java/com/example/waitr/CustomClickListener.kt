package com.example.waitr

import android.view.View

class CustomClickListener(
    private val onClick: () -> Unit,
    private val onDoubleClick: () -> Unit
) : View.OnClickListener {

    private var lastClickTime = 0L
    private val DOUBLE_CLICK_THRESHOLD = 300

    override fun onClick(v: View?) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime < DOUBLE_CLICK_THRESHOLD) {
            onDoubleClick()
        } else {
            onClick()
        }
        lastClickTime = currentTime
    }
}