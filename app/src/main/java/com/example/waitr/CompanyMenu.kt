package com.example.waitr

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.icu.text.Transliterator.Position
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
    //promenne
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
    private lateinit var invitesDisplay: LinearLayout
    private lateinit var joinCompany: Button
    private lateinit var invitesDialog: Dialog
    private lateinit var linearLayoutContainer: LinearLayout
    private lateinit var noInvitesYet: TextView
    private lateinit var username: String
    private lateinit var email: String

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_company_menu)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        //definovani dialogu pro zobrazeni pozvanek
        invitesDialog = Dialog(this)
        invitesDialog.setContentView(R.layout.displaying_invites_in_company_menu_popup)
        //definovani dynamickeho layoutu v dialogu
        invitesDisplay = invitesDialog.findViewById(R.id.display_invites_layout)
        linearLayoutContainer = findViewById(R.id.linearLayoutContainer)

        noCompaniesTextView = TextView(this).apply {
            text = "No companies yet"
            textSize = 16f
            gravity = Gravity.CENTER
        }
        noInvitesYet = TextView(this).apply {
            text = "No invites yet"
            textSize = 16f
            gravity = Gravity.CENTER
        }
        // zprovozneni drawermenu...
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        navigationView.itemTextColor = null
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
                        val usernamedata = dataSnapshot.child("username").getValue(String::class.java)
                        val emaildata = dataSnapshot.child("email").getValue(String::class.java)

                        // nastaveni textu v TextView a nastaveni globalnich promenych
                        yourUsername.text = usernamedata
                        yourEmail.text = emaildata
                        if (emaildata != null) {
                            email = emaildata
                        }
                        if (usernamedata != null) {
                            username = usernamedata
                        }
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
                R.id.profile_button -> {
                    profileSettingsPopup()
                    true
                }
                else -> false
            }
        }

        //Načtení seznamu podniků z database
        loadCompanies()

        createCompanyPopup = findViewById(R.id.Create_Company_popup_button)
        createCompanyPopup.setOnClickListener {
            showCreateCompanyPopup()
        }
        joinCompany = findViewById(R.id.Join_Company_button)
        joinCompany.setOnClickListener {
            showJoinCompanyPopup()
        }

    }
    private fun showCreateCompanyPopup() {
        // Vytvoření dialogu
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.create_company_popup)

        // Nastavení velikosti dialogu
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            (resources.displayMetrics.heightPixels * 0.85).toInt()
        )

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
                            tag = CompanyTag(companyId, companyName, "owner")
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
                            "authorization" to "owner"
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
                                    "authorization" to "owner",
                                    "status" to "offline",
                                    "email" to email,
                                    "username" to username
                                )
                            ),
                            "settings" to mapOf(
                                "seatedNotification" to 5,
                                "eatingNotification" to 5,
                                "paidNotification" to 5
                            ),
                            "Analytics" to mapOf(
                                "tables" to mapOf(),
                                "items" to mapOf(),
                                "users" to mapOf(
                                    userId to mapOf(
                                        "numberOfServedTables" to 0,
                                        "activity" to 0
                                    )
                                )
                            )
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
    private fun showJoinCompanyPopup(){
        // Nastavení velikosti dialogu
        invitesDialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            (resources.displayMetrics.heightPixels * 0.85).toInt()
        )
        loadInvites()
        invitesDialog.show()
    }
    // Pomocná funkce pro převod dp na px
    fun Int.dpToPx(): Int {
        return (this * Resources.getSystem().displayMetrics.density).toInt()
    }
    private fun selectedCompanyFromInviteOptions(){
        // Vytvoření dialogu
        val dialog1 = Dialog(this)
        dialog1.setContentView(R.layout.selected_company_from_invite_options)

        // Nastavení velikosti dialogu
        dialog1.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.65).toInt(),
            (resources.displayMetrics.heightPixels * 0.4).toInt()
        )

        dialog1.window?.setBackgroundDrawableResource(android.R.color.white)

        val enterButton = dialog1.findViewById<Button>(R.id.enter_company_from_invite_button)
        val selectedCompanyNameView = dialog1.findViewById<TextView>(R.id.company_from_invite_to_display)
        selectedCompanyNameView.apply {
            text = selectedCompanyName
        }
        enterButton.setOnClickListener{
            val intent: Intent
            // zmena online statusu pri vstupu do podniku
            val onlineStatusMap = mapOf(
                "status" to "online"
            )
            selectedCompanyId?.let {
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
                    Log.e("NotSignedIn", "user is not signed in")
                }
                Log.e("NoCompanyId", "Failed to get the companyId")
            }

            dialog1.dismiss()
            intent = Intent(this, Company_manager::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            intent.putExtra("COMPANY_ID", selectedCompanyId) // Předání ID do nové aktivity
            startActivity(intent)
            finish()
        }
        dialog1.show()
    }
    private fun acceptInvite(companyId: String, companyName: String, textViewId: Int, position: String){
        if (companyName.isNotEmpty()) {
            selectedCompanyName = companyName
            selectedCompanyId = companyId // Nastavení ID do globální proměnné
            // Vytvoř nový Button
            val newButton = Button(this).apply {
                selectedCompanyName = companyName
                text = companyName
                //tag = companyId // Uložení ID podniku
                tag = CompanyTag(companyId, companyName, position)
            }
            newButton.setOnClickListener {
                val companyTag = it.tag as CompanyTag
                selectedCompanyTag = companyTag
                selectedCompanyId = companyTag.companyId // Aktualizace ID
                selectedCompanyName = companyTag.companyName
                selectedAuthorization = companyTag.authorization
                if (position == "owner"){
                    selectedCompanyOptions()
                }
                if (position == "employee" || position == "manager"){
                    selectedCompanyFromInviteOptions()
                }
            }
            // Přidání tlačítka do LinearLayout
            linearLayoutContainer.addView(newButton)
            val view = invitesDisplay.findViewWithTag<TextView>(textViewId)
            Log.e("naslo to invite", view.toString())
            invitesDisplay.removeView(view)

            // Uložení názvu společnosti a ID do Firebase Realtime Database u konkrétního uživatele
            val companyMap = mapOf(
                "companyName" to companyName,
                "authorization" to position
            )

            // Uložení společnosti k uživateli
            if (userId != null) {
                db.child("users").child(userId).child("companies").child(companyId)
                    .setValue(companyMap)
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "Company: '$companyName' joined!",
                            Toast.LENGTH_SHORT
                        ).show()
                        invitesDialog.dismiss()
                        checksIfNoCompanies()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Failed to create company: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                //pridani uzivatele ke spolecnosti
                val newUserMap = mapOf(
                    userId to mapOf(
                        "authorization" to position,
                        "status" to "offline",
                        "email" to email,
                        "username" to username
                    )
                )
                db.child("companies").child(companyId).child("users")
                    .updateChildren(newUserMap)
                    .addOnSuccessListener {
                        Log.d("DatabaseUpdate", "User added successfully")
                    }
                    .addOnFailureListener{ exception ->
                        Log.e("DatabaseUpdate", "Error adding user: ${exception.message}")
                    }
                // pridani uzivatele k analytics
                val userAnalytics = mapOf(
                    userId to mapOf(
                        "numberOfServedTables" to 0,
                        "activity" to 0
                    )
                )
                db.child("companies").child(companyId).child("Analytics").child("users")
                    .updateChildren(userAnalytics)
                    .addOnSuccessListener {
                        Log.d("DatabaseUpdate", "User added successfully to analytics")
                    }
                    .addOnFailureListener{ exception ->
                        Log.e("DatabaseUpdate", "Error adding user to analytics: ${exception.message}")
                    }
                // smazani poznamky po potvrzeni
                removeInvite(companyId)
            }
        }
    }
    // vymaze poznamku z database uzivatele
    private fun removeInvite(companyIdToRemove: String){
        userId?.let { userId ->
            val invitesRef = db.child("users").child(userId).child("invites")
            invitesRef.get()
                .addOnSuccessListener { dataSnapshot ->
                    if (dataSnapshot.exists()) {
                        for (invite in dataSnapshot.children) {
                            val inviteValue = invite.key
                            if (inviteValue == companyIdToRemove) {
                                invite.ref.removeValue()
                                    .addOnSuccessListener {
                                        Log.d("RemoveInvite", "Invite successfully removed.")
                                    }
                                    .addOnFailureListener { exception ->
                                        Log.e("RemoveInvite", "Failed to remove invite.", exception)
                                    }
                                break // Pokud chceš smazat jen jeden uzel, ukonči cyklus
                            }
                        }
                    } else {
                        Log.d("RemoveInvite", "No invites found.")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("RemoveInvite", "Failed to fetch invites.", exception)
                }
        }
    }
    private fun loadInvites(){
        if (invitesDisplay.contains(noInvitesYet)){
            invitesDisplay.removeView(noInvitesYet)
        }
        userId?.let {
            val invitesRef = db.child("users").child(it).child("invites")

            invitesRef.get()
                .addOnSuccessListener { dataSnapshot ->
                    if (dataSnapshot.exists()) {
                        for (inviteSnapshot in dataSnapshot.children) {
                            // Získání ID společnosti z uzlu invites
                            val companyID = inviteSnapshot.key
                            //ziskani pozice
                            val position = inviteSnapshot.child("position").getValue(String::class.java)
                            companyID?.let { id ->
                                // Odkaz na uzel společnosti v databázi
                                val companiesRef = db.child("companies").child(id)

                                companiesRef.child("name").get()
                                    .addOnSuccessListener { companySnapshot ->

                                        val companyName = companySnapshot.getValue(String::class.java)
                                        val textViewID = View.generateViewId()

                                        companyName?.let { name ->
                                            // Dynamické vytvoření TextView pro každou pozvánku
                                            val inviteToDisplay = TextView(this).apply {
                                                text = "New invite from company: $name\nClick to accept" // Nastavení textu
                                                layoutParams = LinearLayout.LayoutParams(280.dpToPx(), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                                                    setMargins(10.dpToPx(), 10.dpToPx(), 10.dpToPx(), 10.dpToPx()) // Nastavení marginů
                                                }
                                                textSize = 18f // Nastavení velikosti textu
                                                setBackgroundColor(Color.parseColor("#BDEDBF")) // Nastavení barvy pozadí
                                                tag = textViewID
                                            }
                                            // Nastavení listeneru na TextView
                                            inviteToDisplay.setOnClickListener {
                                                if (position != null) {
                                                    acceptInvite(companyID, companyName, textViewID, position)
                                                }
                                            }

                                            // Přidání TextView do vašeho layoutu
                                            invitesDisplay.addView(inviteToDisplay)

                                        }
                                    }.addOnFailureListener { error ->
                                        Log.e("DatabaseError", "Failed to load inviteCompany name: ${error.message}")
                                    }
                            }
                        }
                    } else {
                        Log.d("Invites", "No invites found.")
                        // text ze nejsou ivity
                        invitesDisplay.addView(noInvitesYet)
                    }
                }.addOnFailureListener { error ->
                    Log.e("DatabaseError", "Failed to load invites: ${error.message}")
                }
        }
    }
    private fun loadCompanies(){
        userId?.let {
            val userRef = db.child("users").child(it).child("companies") // Cesta k podnikovým datům uživatele
            userRef.get()
                .addOnSuccessListener { dataSnapshot ->
                    if (dataSnapshot.exists()) {
                        for (companySnapshot in dataSnapshot.children) {
                            val companyId = companySnapshot.key
                            val companyName = companySnapshot.child("companyName").getValue(String::class.java)
                            val authorization = companySnapshot.child("authorization").getValue(String::class.java)

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
                                    if (selectedAuthorization.equals("owner")){
                                        selectedCompanyOptions()
                                    } else if (selectedAuthorization.equals("employee") || selectedAuthorization.equals("manager")){
                                        selectedCompanyFromInviteOptions()
                                    }
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
            // zmena online statusu pri vstupu do podniku
            val onlineStatusMap = mapOf(
                "status" to "online"
            )
            selectedCompanyId?.let {
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
                    Log.e("NotSignedIn", "user is not signed in")
                }
                Log.e("NoCompanyId", "Failed to get the companyId")
            }

            dialog1.dismiss()
            intent = Intent(this, Company_manager::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            intent.putExtra("COMPANY_ID", selectedCompanyId) // Předání ID do nové aktivity
            startActivity(intent)
            finish()
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
//metoda pro smazani vsem spolecnosti
    private fun deleteCompany(companyId: String, CompanyTag: CompanyTag){
        val linearLayoutContainer = findViewById<LinearLayout>(R.id.linearLayoutContainer)
        db.child("users").get()
            .addOnSuccessListener { snapshot ->
                snapshot.children.forEach { userSnapshot ->
                    val userId = userSnapshot.key
                    if (userId != null) {
                        db.child("users").child(userId).child("companies").child(companyId).removeValue()
                            .addOnSuccessListener {
                                Log.d("DeleteCompany", "Company $companyId removed for user $userId")
                            }
                            .addOnFailureListener { exception ->
                            Log.e("DeleteCompany", "Failed to remove company for user $userId", exception)
                            }
                    }
                }
                // Odstrani spolecnosti z database podniku
                db.child("companies").child(companyId).removeValue()
                    .addOnSuccessListener {
                        val buttonToRemove = linearLayoutContainer.findViewWithTag<Button>(CompanyTag)
                        linearLayoutContainer.removeView(buttonToRemove)
                        checksIfNoCompanies()
                        Toast.makeText(this, "Company deleted successfully for all users!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                    Toast.makeText(this, "Failed to delete company: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to retrieve users: ${exception.message}", Toast.LENGTH_SHORT).show()
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

    private fun profileSettingsPopup(){
        // Vytvoření dialogu
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.profile_settings_popup)

        // Nastavení velikosti dialogu
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            (resources.displayMetrics.heightPixels * 0.85).toInt()
        )

        // Reference na prvky v popup layoutu
        val closeButton = dialog.findViewById<Button>(R.id.Create_Company_button)
        closeButton.setOnClickListener {
            dialog.dismiss()
        }
        val usernameTextView = dialog.findViewById<TextView>(R.id.profile_settings_username_textView)
        usernameTextView.text = username
        val emailTextView = dialog.findViewById<TextView>(R.id.profile_settings_email_textView)
        emailTextView.text = email
        val changeUsername = dialog.findViewById<Button>(R.id.change_profile_username_button)
        changeUsername.setOnClickListener {
            changeProfileParameters("username")
        }
        val changePassword = dialog.findViewById<Button>(R.id.change_profile_password_button)
        changePassword.setOnClickListener {
            changeProfileParameters("password")
        }

        dialog.show()
    }

    private fun changeProfileParameters(type: String){
        // Vytvoření dialogu
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.create_company_popup)

        // Nastavení velikosti dialogu
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            (resources.displayMetrics.heightPixels * 0.85).toInt()
        )

        when(type){
            "username" -> {

            }
            "password" -> {

            }
        }
    }
}