package com.example.waitr

import android.os.Handler
import android.os.Looper
import android.view.View

class CustomClickListener(
    private val onClick: () -> Unit,
    private val onDoubleClick: () -> Unit
) : View.OnClickListener {

    private var lastClickTime = 0L
    private var clickCount = 0
    private val DOUBLE_CLICK_THRESHOLD = 500L
    private val handler = Handler(Looper.getMainLooper())

    override fun onClick(v: View?) {
        clickCount++
        val currentTime = System.currentTimeMillis()

        // Spustit zpoždění pro rozlišení kliknutí
        handler.postDelayed({
            if (clickCount == 1) {
                // Jedno kliknutí
                onClick()
            } else if (clickCount == 2) {
                // Dvojité kliknutí
                onDoubleClick()
            }
            // Resetovat počet kliknutí po vyhodnocení
            clickCount = 0
        }, DOUBLE_CLICK_THRESHOLD)

        // Aktualizace času posledního kliknutí (pro debug nebo další logiku)
        lastClickTime = currentTime
    }
}