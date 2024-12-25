package com.example.waitr

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.LinearLayout
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
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
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
    private val db = FirebaseDatabase.getInstance("https://waitr-dee9a-default-rtdb.europe-west1.firebasedatabase.app/").reference // Using Realtime Database reference
    private lateinit var createCompanyPopup: Button


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
            val userRef = db.child("users").child(it) // Realtime Database path to user data
            userRef.get()
                .addOnSuccessListener { dataSnapshot ->
                    if (dataSnapshot.exists()) {
                        val username = dataSnapshot.child("username").getValue(String::class.java)
                        val email = dataSnapshot.child("email").getValue(String::class.java)

                        // Set text in TextViews
                        yourUsername.text = username
                        yourEmail.text = email
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("RealtimeDB", "Error getting data: ", exception)
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
        val linearLayoutContainer = findViewById<LinearLayout>(R.id.linearLayoutContainer)
// Načtení seznamu podniků z database
        userId?.let {
            val userRef = db.child("users").child(it).child("companies") // Cesta k podnikovým datům uživatele
            userRef.get()
                .addOnSuccessListener { dataSnapshot ->
                    if (dataSnapshot.exists()) {
                        for (companySnapshot in dataSnapshot.children) {
                            val companyId = companySnapshot.key
                            val companyName = companySnapshot.child("companyName").getValue(String::class.java)

                            if (companyId != null && companyName != null) {
                                val newButton = Button(this).apply {
                                    text = companyName
                                    tag = companyId // Uložení ID podniku
                                }

                                newButton.setOnClickListener {
                                    val companyIdasString = it.tag as String
                                    val intent = Intent(this, Company::class.java)
                                    intent.putExtra("COMPANY_ID", companyIdasString)
                                    startActivity(intent)
                                }

                                // Přidání tlačítka do LinearLayout
                                linearLayoutContainer.addView(newButton)
                            }
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("RealtimeDB", "Error getting companies: ", exception)
                }
        }

        createCompanyPopup = findViewById(R.id.Create_Company_popup_button)
        createCompanyPopup.setOnClickListener {
            showCreateCompanyPopup()
        }

    }
    private fun showCreateCompanyPopup() {
        // Vytvoření dialogu
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.create_company_popup)

        // Nastavení velikosti dialogu
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            (resources.displayMetrics.heightPixels * 0.6).toInt()
        )

        // Zarovnání na střed
        dialog.window?.setBackgroundDrawableResource(android.R.color.holo_blue_light)

        // Reference na prvky v popup layoutu
        val createButton = dialog.findViewById<Button>(R.id.Create_Company_button)
        val companyNameInput = dialog.findViewById<TextInputEditText>(R.id.Company_name)
        val linearLayoutContainer = findViewById<LinearLayout>(R.id.linearLayoutContainer)

        // Akce při kliknutí na tlačítko "Create"
        createButton.setOnClickListener {
            val companyName = companyNameInput.text.toString().trim()
            if (companyName.isNotEmpty()) {
                if (userId != null) {
                    // vytvoreni id pro spolecnost
                    val companyId = db.child("users").child(userId).child("companies").push().key

                    if (companyId != null) {
                        // Vytvoř nový Button
                        val newButton = Button(this).apply {
                            text = companyName
                            tag = companyId // Uložení ID podniku (například z databáze)
                        }

                        newButton.setOnClickListener {
                            val companyIdasString = it.tag as String // Získání ID podniku
                            val intent = Intent(this, Company::class.java)
                            intent.putExtra(
                                "COMPANY_ID",
                                companyIdasString
                            ) // Předání ID do nové aktivity
                            startActivity(intent)
                        }
                        // Přidání tlačítka do LinearLayout
                        linearLayoutContainer.addView(newButton)

                        // Uložení názvu společnosti a ID do Firebase Realtime Database u konkrétního uživatele
                        val companyMap = mapOf(
                            "companyName" to companyName
                        )

                        // Uložení společnosti do uživatele
                        db.child("users").child(userId).child("companies").child(companyId)
                            .setValue(companyMap)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this,
                                    "Company '$companyName' created!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    this,
                                    "Failed to create company: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                        dialog.dismiss()
                    }
                }
            } else {
                Toast.makeText(this, "Please enter a company name", Toast.LENGTH_SHORT).show()
            }
        }
        // Zobrazení dialogu
        dialog.show()
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