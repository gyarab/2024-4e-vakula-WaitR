package com.example.waitr

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.WindowInsetsController
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.android.material.navigation.NavigationView.OnNavigationItemSelectedListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CompanyMenu : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private lateinit var yourUsername: TextView
    private lateinit var yourEmail: TextView
    private val auth = FirebaseAuth.getInstance()
    private val currentUser = auth.currentUser
    private val userId = currentUser?.uid
    val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_company_menu)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        drawerToggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val headerView = navigationView.getHeaderView(0)


        yourUsername = headerView.findViewById(R.id.yourUsername)
        yourEmail = headerView.findViewById(R.id.yourEmail)

        userId?.let {
            usersCollection.document(it).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val username = document.getString("username") // assuming username is saved under "username"
                        val email = document.getString("email") // assuming email is saved under "email"

                        // Nastav text do TextInputLayout
                        yourUsername.setText(username)
                        yourEmail.setText(email)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("Firestore", "Error getting documents: ", exception)
                }
        }

        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                        R.id.logout_button -> {
                    //Odhlášení uživatele z Firebase
                    FirebaseAuth.getInstance().signOut()

                    // Přesun na Login obrazovku
                    val intent = Intent(this, Login::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (drawerToggle.onOptionsItemSelected(item)) {
            true // Událost byla zpracována výsuvným menu
        } else {
            when (item.itemId) {
                R.id.logout_button -> {
                    // Akce pro logout
                    FirebaseAuth.getInstance().signOut()
                    Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        }
    }

    private fun enableEdgeToEdge() {
        // Případná úprava stylu pro okraje
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
    }
}