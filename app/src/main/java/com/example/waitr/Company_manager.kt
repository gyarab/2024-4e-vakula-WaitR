package com.example.waitr

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class Company_manager : AppCompatActivity() {
    // promenne sem
    private val auth = FirebaseAuth.getInstance()
    private val currentUser = auth.currentUser
    private val userId = currentUser?.uid
    private val db = FirebaseDatabase.getInstance("https://waitr-dee9a-default-rtdb.europe-west1.firebasedatabase.app/").reference // Using Realtime Database reference
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private lateinit var yourUsername: TextView
    private lateinit var yourEmail: TextView
    private lateinit var yourAuthStatus: TextView
    private lateinit var bottomNavigationView : BottomNavigationView
    private lateinit var modelView: Model_view
    private lateinit var foodMenu: Food_menu
    private lateinit var analytics: Analytics
    private lateinit var CompanyID: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_company_manager)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.company_manager_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // hlavni id podle ktereho se do layoutu nactou spravna data z database
        val intentmain = intent
        CompanyID = intentmain.getStringExtra("COMPANY_ID").toString()

        // Vytvoření fragmentů a předání CompanyID
        CompanyID?.let {
            modelView = Model_view.newInstance(it)
            foodMenu = Food_menu.newInstance(it)
            analytics = Analytics.newInstance(it)

            setCurrentFragment(modelView)
            bottomNavigationView = findViewById(R.id.bottomNavigationView)
            bottomNavigationView.setOnNavigationItemSelectedListener {
                when(it.itemId){
                    R.id.ModelView -> setCurrentFragment(modelView)
                    R.id.Foodmenu -> setCurrentFragment(foodMenu)
                    R.id.Analytics -> setCurrentFragment(analytics)
                }
                true
            }
        }

        // zprovozneni drawermenu...
        drawerLayout = findViewById(R.id.company_manager_main)
        navigationView = findViewById(R.id.manager_company_drawer_menu)
        drawerToggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val headerView = navigationView.getHeaderView(0)
        yourUsername = headerView.findViewById(R.id.yourUsername_company_manager)
        yourEmail = headerView.findViewById(R.id.yourEmail_company_manager)
        yourAuthStatus = headerView.findViewById(R.id.auth_status_company_manager)
        // nacteni dat do headeru
        userId?.let {
            val userRef = db.child("users").child(it)
            userRef.get()
                .addOnSuccessListener { dataSnapshot ->
                    if (dataSnapshot.exists()) {
                        val username = dataSnapshot.child("username").getValue(String::class.java)
                        val email = dataSnapshot.child("email").getValue(String::class.java)
                        val authorization = CompanyID?.let { it1 ->
                            dataSnapshot.child("companies").child(it1).child("authorization").getValue(String::class.java)
                        }
                        // nastaveni textu v TextView
                        yourUsername.text = username
                        yourEmail.text = email
                        yourAuthStatus.text = authorization
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("RealtimeDB", "Error getting data: ", exception)
                }
        }
// TODO funkcnost polozek v drawermenu
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.manager_settings_button -> {
                    true
                }
                R.id.manager_show_members_button -> {
                    true
                }
                R.id.manager_add_members_button -> {
                    // funkce pridavani uzivatelu do spolecnosti pomoci pozvanky
                    showInviteUserPopUp()
                    true
                }
                R.id.manager_info_button -> {
                    true
                }
                R.id.manager_logout_of_company_button -> {
                    // zmena online statusu pri opusteni podniku
                    val onlineStatusMap = mapOf(
                        "status" to "offline"
                    )
                    CompanyID?.let {
                            it1 ->
                        if (userId != null) {
                            db.child("companies").child(it1).child("users").child(userId)
                                .updateChildren(onlineStatusMap)
                                .addOnSuccessListener {
                                    Log.d("StatusChange", "Changed status successfully")
                                }
                                .addOnFailureListener { exception ->
                                    Log.e("StatusChange", "Failed to change online status", exception)
                                }
                        }
                    }
                    // Přesun na Company menu obrazovku
                    val intent = Intent(this, CompanyMenu::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }

    }
    private fun showInviteUserPopUp(){
        // Vytvoření dialogu
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.company_manager_invite_popup)

        // Nastavení velikosti dialogu
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            (resources.displayMetrics.heightPixels * 0.85).toInt()
        )
        // Reference na prvky v popup layoutu
        val inviteButton = dialog.findViewById<Button>(R.id.invite_user_button)
        val userToInvite = dialog.findViewById<TextInputEditText>(R.id.user_to_invite)
        val spinner: Spinner = dialog.findViewById(R.id.positionSpinner)

        // Možnosti pro Spinner
        val options = listOf("Select position", "employee", "manager")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        //akce pri kliknuti na tlacitka invite
        inviteButton.setOnClickListener {
            val emailOfUser = userToInvite.text.toString().trim()
            val selectedPosition = spinner.selectedItem.toString()
            if (selectedPosition.equals("Select position", ignoreCase = true)) {
                Toast.makeText(this, "Please select the user's position", Toast.LENGTH_SHORT).show()
            } else {
                // Získání UID uživatele podle emailu
                db.child("users").get()
                    .addOnSuccessListener { dataSnapshot ->
                        var foundUid: String? = null

                        // Iterace přes všechny uživatele
                        for (userSnapshot in dataSnapshot.children) {
                            val email = userSnapshot.child("email").getValue(String::class.java)
                            if (email == emailOfUser) {
                                foundUid = userSnapshot.key // UID je klíčem ve struktuře
                                break
                            }
                        }

                        // Pokud UID není nalezeno, zobraz chybovou zprávu
                        if (foundUid == null) {
                            Toast.makeText(
                                this,
                                "User not found",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@addOnSuccessListener
                        }
                        //zkontroluj jestli uz neni ve spolecnosti
                        val usersInCompanyRef = db.child("companies").child(CompanyID).child("users")
                        usersInCompanyRef.get()
                            .addOnSuccessListener { usersInCompanySnapshot ->
                                var userAlreadyInCompany = false
                                for (userInCompany in usersInCompanySnapshot.children) {
                                    if (userInCompany.key == foundUid) {
                                        userAlreadyInCompany = true
                                        break
                                    }
                                }
                                if (userAlreadyInCompany) {
                                    Toast.makeText(
                                        this,
                                        "This user is already in company!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@addOnSuccessListener
                                }
                                // Pokračujeme dále s kontrolou pozvánky
                                val invitesRef = db.child("users").child(foundUid).child("invites")
                                invitesRef.get()
                                    .addOnSuccessListener { invitesSnapshot ->
                                        if (invitesSnapshot.hasChild(CompanyID)) {
                                            Toast.makeText(
                                                this,
                                                "The user already has an invitation!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            val position = if (selectedPosition == "manager") "manager" else "employee"
                                            val newInvite = mapOf(
                                                CompanyID to mapOf("position" to position)
                                            )
                                            db.child("users").child(foundUid).child("invites")
                                                .updateChildren(newInvite)
                                                .addOnSuccessListener {
                                                    Toast.makeText(
                                                        this,
                                                        "Invite sent!",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    dialog.dismiss()
                                                }
                                                .addOnFailureListener {
                                                    Toast.makeText(
                                                        this,
                                                        "Failed to send invite",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                        }
                                    }
                                    .addOnFailureListener {
                                        Log.e("Firebase", "Error getting invites", it)
                                    }
                            }
                            .addOnFailureListener {
                                Log.e("Firebase error", "Failed to check users in company")
                            }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("Firebase", "Error getting users", exception)
                        Toast.makeText(
                            this,
                            "Error fetching users",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }
        dialog.show()
    }
    // Metoda pro meneni fragmentu
    private fun setCurrentFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        supportFragmentManager.fragments.forEach { transaction.hide(it) }

        if (fragment.isAdded) {
            transaction.show(fragment)
        } else {
            transaction.add(R.id.companyframelayout, fragment)
        }
        transaction.commit()
    }
    // metoda na funkcnost hamburgeru
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Otevření navigačního menu
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    drawerLayout.openDrawer(GravityCompat.START)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    // pri ukonceni aktivity se provede tahle metoda
    override fun onDestroy() {
        super.onDestroy()
        // Nastavení statusu na "offline"
        val onlineStatusMap = mapOf(
            "status" to "offline"
        )

        CompanyID?.let { companyId ->
            userId?.let { userId ->
                db.child("companies").child(companyId).child("users").child(userId)
                    .updateChildren(onlineStatusMap)
                    .addOnSuccessListener {
                        Log.d("StatusChange", "Changed status successfully to offline")
                    }
                    .addOnFailureListener { exception ->
                        Log.e("StatusChange", "Failed to change online status to offline", exception)
                    }
            } ?: Log.e("StatusChange", "User ID is null")
        } ?: Log.e("StatusChange", "Company ID is null")
    }
}