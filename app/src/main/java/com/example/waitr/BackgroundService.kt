package com.example.waitr

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Button
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
//TODO
class BackgroundService : Service() {

    private val auth = FirebaseAuth.getInstance()
    private val currentUser = auth.currentUser
    private val userId = currentUser?.uid
    private lateinit var companyId: String
    private val db = FirebaseDatabase.getInstance("https://waitr-dee9a-default-rtdb.europe-west1.firebasedatabase.app/").reference // Using Realtime Database reference

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Aplikace byla ukončena
        updateUserStatus() // Nastav stav uživatele na "offline"
        stopSelf()
    }

    private fun updateUserStatus() {
        getCompanyId { id ->
            companyId = id
            val ref = userId?.let { db.child("companies").child(companyId).child("users").child(it).child("status") }
            ref?.setValue("offline")?.addOnSuccessListener {
                Log.e("success", "uspech")
            }
        }
    }

    private fun getCompanyId(callback: (String) -> Unit) {
        val companyList = mutableListOf<String>()
        val ref = userId?.let { db.child("users").child(it).child("companies") }
        ref?.get()?.addOnSuccessListener { dataSnapshot ->
            if (dataSnapshot.exists()) {
                for (child in dataSnapshot.children) {
                    companyList.add(child.key.toString())
                }
                val idRef = db.child("companies")
                companyList.forEach { company ->
                    val reference = userId?.let { idRef.child(company).child("users").child(it) }
                    reference?.get()?.addOnSuccessListener { dataSnapshot ->
                        if (dataSnapshot.exists()) {
                            val status = dataSnapshot.child("status").getValue(String::class.java)
                            if (status == "online") {
                                callback(company)
                                return@addOnSuccessListener
                            }
                        }
                    }
                }
            }
        }
    }
}