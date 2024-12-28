package com.example.waitr

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_company_manager)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.company_manager_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // hlavni id podle ktereho se do layoutu nactou sparna data z database
        val intentmain = intent
        val CompanyID = intentmain.getStringExtra("COMPANY_ID")

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
                            dataSnapshot.child("companies").child(it1).child("Authorization").getValue(String::class.java)
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
                                .setValue(onlineStatusMap)
                                .addOnSuccessListener {
                                    Log.d("StatusChange", "Changed status successfully")
                                }
                                .addOnFailureListener { exception ->
                                    Log.e("StatusChange", "Failed to change online status", exception)
                                }
                            Log.e("NotSignedIn", "user is not signed in")
                        }
                        Log.e("NoCompanyId", "Failed to get the companyId")
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

        //akce pri kliknuti na tlacitka invite
        inviteButton.setOnClickListener {
            val emailOfUser = userToInvite.text.toString().trim()

            findUserByEmail(emailOfUser) { uid ->
                if (uid != null) {
                    sendInvite(uid)
                } else {
                    Toast.makeText(
                        this,
                        "User not found",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun sendInvite(idUzivatele: String) {
        // podiva se jestli uz uzivatel pozvanku od spolecnosti nema
        val invitesRef = db.child("users").child(idUzivatele).child("invites")
        invitesRef.get()
            .addOnSuccessListener { dataSnapshot ->
                var maUzInvite = false
                if (dataSnapshot.exists()) {
                    for (invitesSnapshot in dataSnapshot.children){
                        if (invitesSnapshot.child("from").getValue(String::class.java) == CompanyID){
                            maUzInvite = true
                            break
                        }
                    }
                }
                if (!maUzInvite){
                    //TODO nefunguje not working
                    val invitesMap = mapOf(
                        "from" to CompanyID
                    )
                    db.child("users").child(idUzivatele).child("invites").updateChildren(invitesMap)
                        .addOnSuccessListener {
                            Toast.makeText(
                                this,
                                "Invite sent!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                this,
                                "Failed to sent invite",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                } else {
                    Toast.makeText(
                        this,
                        "The user already has an invitation!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("RealtimeDB", "Error getting invites ", exception)
            }
    }

    //metoda pro ziskani uid uzivatele pomoci emailu
    private fun findUserByEmail(emailInput: String, callback: (String?) -> Unit){
        db.child("users").get()
            .addOnSuccessListener { dataSnapshot ->
            var foundUid: String? = null

            // Iterace přes všechny uživatele
            for (userSnapshot in dataSnapshot.children) {
                val email = userSnapshot.child("email").getValue(String::class.java)
                if (email == emailInput) {
                    foundUid = userSnapshot.key // UID je klíčem ve struktuře
                    break
                }
            }
            // vraceni hodnoty pomoci funkce
            callback(foundUid)
        }.addOnFailureListener { exception ->
            Log.e("Firebase", "Chyba při čtení uživatelů: ${exception.message}")
        }

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
}