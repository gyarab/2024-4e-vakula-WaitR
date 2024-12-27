package com.example.waitr

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.contains
import androidx.core.view.isEmpty
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
// trida pro ulozeni  tagu pro button
data class CompanyTag(val companyId: String, val companyName: String, val authorization: String)

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
    private var selectedCompanyId: String? = null
    private var selectedCompanyName: String? = null
    private var selectedAuthorization: String? = null
    private var selectedCompanyTag: CompanyTag? = null
    private lateinit var noCompaniesTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_company_menu)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        noCompaniesTextView = TextView(this).apply {
            text = "No companies yet"
            textSize = 16f
            gravity = Gravity.CENTER
        }
        // zprovozneni drawermenu...
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        drawerToggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val headerView = navigationView.getHeaderView(0)
        yourUsername = headerView.findViewById(R.id.yourUsername)
        yourEmail = headerView.findViewById(R.id.yourEmail)
// nacteni dat do headeru
        userId?.let {
            val userRef = db.child("users").child(it)
            userRef.get()
                .addOnSuccessListener { dataSnapshot ->
                    if (dataSnapshot.exists()) {
                        val username = dataSnapshot.child("username").getValue(String::class.java)
                        val email = dataSnapshot.child("email").getValue(String::class.java)

                        // nastaveni textu v TextView
                        yourUsername.text = username
                        yourEmail.text = email
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("RealtimeDB", "Error getting data: ", exception)
                }
        }
// funkcnost polozek v drawermenu
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                        R.id.logout_button -> {
                    // Odhlášení
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
// Načtení seznamu podniků z database...
        userId?.let {
            val userRef = db.child("users").child(it).child("companies") // Cesta k podnikovým datům uživatele
            userRef.get()
                .addOnSuccessListener { dataSnapshot ->
                    if (dataSnapshot.exists()) {
                        for (companySnapshot in dataSnapshot.children) {
                            val companyId = companySnapshot.key
                            val companyName = companySnapshot.child("companyName").getValue(String::class.java)
                            val authorization = companySnapshot.child("Authorization").getValue(String::class.java)

                            if (companyId != null && companyName != null && authorization != null) {
                                val newButton = Button(this).apply {
                                    text = companyName
                                    tag = CompanyTag(companyId, companyName, authorization)
                                }
                                newButton.setOnClickListener {
                                    val companyTag = it.tag as CompanyTag
                                    selectedCompanyTag = companyTag
                                    selectedCompanyId = companyTag.companyId // Aktualizace ID
                                    selectedCompanyName = companyTag.companyName
                                    selectedAuthorization = companyTag.authorization
                                    selectedCompanyOptions()
                                }

                                // Přidání tlačítka do LinearLayout
                                linearLayoutContainer.removeView(noCompaniesTextView)
                                linearLayoutContainer.addView(newButton)
                            }
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("RealtimeDB", "Error getting companies: ", exception)
                }
            checksIfNoCompanies()
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

        dialog.window?.setBackgroundDrawableResource(android.R.color.holo_blue_light)

        // Reference na prvky v popup layoutu
        val createButton = dialog.findViewById<Button>(R.id.Create_Company_button)
        val companyNameInput = dialog.findViewById<TextInputEditText>(R.id.Company_name)
        val linearLayoutContainer = findViewById<LinearLayout>(R.id.linearLayoutContainer)

        // Akce při kliknutí na tlačítko "Create"
        createButton.setOnClickListener {
            val companyName = companyNameInput.text.toString().trim()
            if (companyName.isNotEmpty()) {
                selectedCompanyName = companyName
                if (userId != null) {
                    // vytvoreni id pro spolecnost
                    val companyId = db.child("users").child(userId).child("companies").push().key

                    if (companyId != null) {
                        selectedCompanyId = companyId // Nastavení ID do globální proměnné
                        // Vytvoř nový Button
                        val newButton = Button(this).apply {
                            selectedCompanyName = companyName
                            text = companyName
                            //tag = companyId // Uložení ID podniku
                            tag = CompanyTag(companyId, companyName, "manager")
                        }
                        newButton.setOnClickListener {
                            val companyTag = it.tag as CompanyTag
                            selectedCompanyTag = companyTag
                            selectedCompanyId = companyTag.companyId // Aktualizace ID
                            selectedCompanyName = companyTag.companyName
                            selectedAuthorization = companyTag.authorization
                            selectedCompanyOptions()
                        }
                        // Přidání tlačítka do LinearLayout
                        linearLayoutContainer.addView(newButton)

                        // Uložení názvu společnosti a ID do Firebase Realtime Database u konkrétního uživatele
                        val companyMap = mapOf(
                            "companyName" to companyName,
                            "Authorization" to "manager"
                        )

                        // Uložení společnosti k uživateli
                        db.child("users").child(userId).child("companies").child(companyId)
                            .setValue(companyMap)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this,
                                    "Company '$companyName' created!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                checksIfNoCompanies()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    this,
                                    "Failed to create company: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                        // ulozeni spolecnosti do database
                        val companyMap2 = mapOf(
                            "name" to companyName,
                            "users" to mapOf(
                                userId to mapOf(
                                    "authorization" to "manager",
                                    "status" to "offline"
                                )
                            ),
                            "ModelView" to "" // Můžete zde přidat další informace, pokud je máte
                        )

                        db.child("companies").child(companyId)
                            .setValue(companyMap2)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this,
                                    "Company '$companyName' created!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                checksIfNoCompanies()
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
    private fun selectedCompanyOptions() {
        // Vytvoření dialogu
        val dialog1 = Dialog(this)
        dialog1.setContentView(R.layout.selected_company_options)

        // Nastavení velikosti dialogu
        dialog1.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.65).toInt(),
            (resources.displayMetrics.heightPixels * 0.4).toInt()
        )

        dialog1.window?.setBackgroundDrawableResource(android.R.color.white)

        val enterButton = dialog1.findViewById<Button>(R.id.enter_company_button)
        val deleteButton = dialog1.findViewById<Button>(R.id.delete_company_button)
        val selectedCompanyNameView = dialog1.findViewById<TextView>(R.id.selected_company_name)

        selectedCompanyNameView.apply {
            text = selectedCompanyName
        }

        enterButton.setOnClickListener{
            val intent: Intent
            if (selectedAuthorization == "manager"){
                intent = Intent(this, Company_manager::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                intent.putExtra("COMPANY_ID", selectedCompanyId) // Předání ID do nové aktivity
                startActivity(intent)
                finish()
            }
        }

        deleteButton.setOnClickListener{
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Delete Company")
            builder.setMessage("Are you sure you want to delete this company?")

            builder.setPositiveButton("Yes") { dialog, _ ->
                deleteCompany(selectedCompanyId!!, selectedCompanyTag!!)
                dialog.dismiss()
                dialog1.dismiss()
            }

            builder.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }

            val alertDialog = builder.create()
            alertDialog.show()
        }
        dialog1.show()
    }

    private fun deleteCompany(companyId: String, CompanyTag: CompanyTag){
        val linearLayoutContainer = findViewById<LinearLayout>(R.id.linearLayoutContainer)
        if (userId != null) {
            db.child("users").child(userId).child("companies").child(companyId).removeValue()
                .addOnSuccessListener {
                    val buttonToRemove = linearLayoutContainer.findViewWithTag<Button>(CompanyTag)
                    linearLayoutContainer.removeView(buttonToRemove)
                    checksIfNoCompanies()
                    Toast.makeText(this, "Company deleted successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to delete company: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
    private fun checksIfNoCompanies() {
        val userRef = userId?.let {
            db.child("users").child(it).child("companies")
        }
        val linearLayoutContainer = findViewById<LinearLayout>(R.id.linearLayoutContainer)
        userRef?.get()?.addOnSuccessListener { dataSnapshot ->
            if (!dataSnapshot.hasChildren()) {
                if (noCompaniesTextView.parent == null) {
                    linearLayoutContainer.removeAllViews()
                    linearLayoutContainer.addView(noCompaniesTextView)
                }
            } else {
                if (noCompaniesTextView.parent != null) {
                    linearLayoutContainer.removeView(noCompaniesTextView)
                }
            }
        }?.addOnFailureListener { exception ->
            Log.e("Firebase", "Error getting data: ", exception)
        }
    }

    // metoda pro funkcnost hamburgeru
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