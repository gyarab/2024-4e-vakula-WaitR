package com.example.waitr

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.google.firebase.database.FirebaseDatabase
//TODO
class BackgroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Aplikace byla ukončena
        updateUserStatus(false) // Nastav stav uživatele na "offline"
        stopSelf()
    }

    private fun updateUserStatus(isOnline: Boolean) {
        val userId = getCurrentUserId() // Získej ID aktuálního uživatele
        if (userId != null) {
            val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("online")
            userRef.setValue(isOnline)
        }
    }

    private fun getCurrentUserId(): String? {
        // Získej ID aktuálního uživatele (např. z SharedPreferences nebo Firebase Auth)
        return "currentUserId" // Nahraď skutečným ID uživatele
    }
}