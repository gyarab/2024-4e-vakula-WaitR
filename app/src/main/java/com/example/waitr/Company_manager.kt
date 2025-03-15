package com.example.waitr

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class Company_manager : AppCompatActivity() {
    // promenne sem
    private val auth = FirebaseAuth.getInstance()
    private val currentUser = auth.currentUser
    private val userId = currentUser?.uid
    private val db = FirebaseDatabase.getInstance("https://waitr-dee9a-default-rtdb.europe-west1.firebasedatabase.app/").reference // Using Realtime Database reference
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var notificationMenuItem: MenuItem
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private lateinit var yourUsername: TextView
    private lateinit var yourEmail: TextView
    private lateinit var yourAuthStatus: TextView
    private lateinit var bottomNavigationView : BottomNavigationView
    private lateinit var modelView: Model_view
    private lateinit var foodMenu: Food_menu
    private lateinit var analytics: Analytics
    private lateinit var CompanyID: String
    private var valueEventListener: ValueEventListener? = null
    private var onlineMembers: ArrayList<String> = ArrayList()
    private var offlineMembers: ArrayList<String> = ArrayList()
    private lateinit var currentMembersDialog: Dialog
    private lateinit var displayOnlineUsers: LinearLayout
    private lateinit var displayOfflineUsers: LinearLayout
    private lateinit var constrainedLayoutForCurrentUsers: ConstraintLayout
    private lateinit var tableNotificationDialog: Dialog
    private lateinit var notificationsLayout: LinearLayout
    private lateinit var companySettingsDialog: Dialog
    private lateinit var settingsLayout: LinearLayout
    private val notificationsList = mutableListOf<Notification>()
    private var settings = Settings()
    private lateinit var UserName: String
    private lateinit var Email: String
    private lateinit var Authorization:String
    private var seatedTableNotificationPeriod = 5
    private var eatingTableNotificationPeriod = 5
    private var paidTableNotificationPeriod = 5
    private lateinit var companyListener: ValueEventListener
    private val userCompaniesRef = userId?.let { db.child("users").child(it).child("companies") }
    private val handler = Handler(Looper.getMainLooper())
    private val allUsersList = mutableListOf<String>()
    private val checkNotificationsRunnable = object : Runnable {
        override fun run() {
            checkAndSendNotifications() // Zavolá tvou funkci
            handler.postDelayed(this, 5000) // Naplánuje další spuštění za 5 sekund
        }
    }

    // Spustí opakovanou úlohu
    fun startCheckingNotifications() {
        handler.post(checkNotificationsRunnable)
    }

    // Zastaví opakovanou úlohu
    fun stopCheckingNotifications() {
        handler.removeCallbacks(checkNotificationsRunnable)
    }


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
        CompanyID.let {
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
        //definovani dynamickych UI prvku
        currentMembersDialog = Dialog(this)
        currentMembersDialog.setContentView(R.layout.current_members_popup)
        currentMembersDialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            (resources.displayMetrics.heightPixels * 0.85).toInt()
        )
        constrainedLayoutForCurrentUsers = currentMembersDialog.findViewById(R.id.constraint_layout_for_current_users)
        displayOnlineUsers = constrainedLayoutForCurrentUsers.findViewById(R.id.display_current_online_users)
        displayOfflineUsers = constrainedLayoutForCurrentUsers.findViewById(R.id.display_current_offline_users)

        // zprovozneni drawermenu...
        drawerLayout = findViewById(R.id.company_manager_main)
        navigationView = findViewById(R.id.manager_company_drawer_menu)
        navigationView.itemIconTintList = null
        navigationView.itemTextColor = null
        getAuthorization { auth ->
            if (auth != null && auth == "employee") {
                navigationView.menu.findItem(R.id.manager_add_members_button).isVisible = false
            }
        }
        notificationMenuItem = navigationView.menu.findItem(R.id.manager_notifications_button)
        drawerToggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val headerView = navigationView.getHeaderView(0)
        yourUsername = headerView.findViewById(R.id.yourUsername_company_manager)
        yourEmail = headerView.findViewById(R.id.yourEmail_company_manager)
        yourAuthStatus = headerView.findViewById(R.id.auth_status_company_manager)
        tableNotificationDialog = Dialog(this)
        tableNotificationDialog.setContentView(R.layout.tabel_notifications_popup)
        notificationsLayout = tableNotificationDialog.findViewById(R.id.table_notifications_layout)
        companySettingsDialog = Dialog(this)
        companySettingsDialog.setContentView(R.layout.company_settings_popup)
        settingsLayout = companySettingsDialog.findViewById(R.id.company_settings_layout)

        // nacteni dat do headeru
        userId?.let {
            val userRef = db.child("users").child(it)
            userRef.get()
                .addOnSuccessListener { dataSnapshot ->
                    if (dataSnapshot.exists()) {
                         UserName = dataSnapshot.child("username").getValue(String::class.java).toString()
                         Email = dataSnapshot.child("email").getValue(String::class.java).toString()
                         Authorization = CompanyID.let { it1 ->
                             dataSnapshot.child("companies").child(it1).child("authorization").getValue(String::class.java)
                                 .toString()
                        }
                        // nastaveni textu v TextView
                        yourUsername.text = UserName
                        yourEmail.text = Email
                        yourAuthStatus.text = Authorization
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("RealtimeDB", "Error getting data: ", exception)
                }
        }
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.manager_notifications_button -> {
                    tableNotificationPopup()
                    true
                }
                R.id.manager_settings_button -> {
                    companySettingsPopup()
                    true
                }
                R.id.manager_show_members_button -> {
                    currentMembersPopup()
                    true
                }
                R.id.manager_add_members_button -> {
                    // funkce pridavani uzivatelu do spolecnosti pomoci pozvanky
                    showInviteUserPopUp()
                    true
                }
                R.id.manager_info_button -> {
                    //TODO
                    true
                }
                R.id.manager_logout_of_company_button -> {
                    // zmena online statusu pri opusteni podniku
                    val onlineStatusMap = mapOf(
                        "status" to "offline"
                    )
                    CompanyID.let { it1 ->
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
        // zavolani metody pro nastaveni listeneru
        createNotificationChannel(this)
        setupRealtimeListener()
        startListeningForNotifications()
        startCheckingNotifications()
        startCompanyListener()
        listenForSettingsChanges()
    }

    //nacte pozici uzivatele pro authorizaci ve spolecnosti
    private fun getAuthorization(callback: (String?) -> Unit) {
        val authRef = CompanyID.let { companyId ->
            userId?.let { uid ->
                db.child("companies").child(companyId).child("users").child(uid).child("authorization")
            } ?: run {
                Log.e("error", "chyba pri ziskani id uzivatele")
                null
            }
        }

        authRef?.get()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val dataSnapshot = task.result
                Authorization = dataSnapshot.getValue(String::class.java).toString()
                Log.d("Authorization", "Hodnota authorization: ${Authorization ?: "Nenalezeno"}")
                callback(Authorization) // Zavolá callback s hodnotou authorization
            } else {
                Log.e("error", "Chyba pri ziskavani dat", task.exception)
                callback(null) // Zavolá callback s null v případě chyby
            }
        }
    }

    private fun showBadge(show: Boolean) {
        if (show) {
            notificationMenuItem.icon = ContextCompat.getDrawable(this, R.drawable.ic_notification_with_badge)
            notificationMenuItem.title = "Notifications - new"
        } else {
            notificationMenuItem.icon = ContextCompat.getDrawable(this, R.drawable.baseline_circle_notifications_24)
            notificationMenuItem.title = "Notifications"
        }
        navigationView.invalidate()
    }

    //Trida pro zobrazeni nastaveni pro spolecnost
    private fun companySettingsPopup(){
        companySettingsDialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            (resources.displayMetrics.heightPixels * 0.85).toInt()
        )
        val closeButton = companySettingsDialog.findViewById<Button>(R.id.close_company_settings_button)
        closeButton.setOnClickListener {
            companySettingsDialog.dismiss()
        }
        fetchDataForSettings {
            drawSettings()
        }
        companySettingsDialog.show()
    }

    private fun drawSettings(){
        val companyName = settings.companyName
        val seatedNotificationTimePeriod = settings.seatedNotSendPeriod
        val eatingNotificationTimePeriod = settings.eatingNotSendPeriod
        val paidNotificationTimePeriod = settings.paidNotSendPeriod

        settingsLayout.removeAllViews()

        val companySetLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }
        val companyTextView = TextView(this).apply {
            text = "Company:"
            textSize = 30f
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val companyNameLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
            orientation = LinearLayout.VERTICAL
            val backgroundDrawable = GradientDrawable().apply {
                setColor(Color.WHITE) // Barva pozadí
                cornerRadius = 30f // Zaoblení rohů v pixelech
            }
            background = backgroundDrawable
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(16, 16, 16, 16)
        }
        val companyNameTextView = TextView(this).apply {
            text = companyName
            textSize = 25f
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val changeNameButton = MaterialButton(this).apply {
            text = "Change name"
            textSize = 20f
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 0, 0, 8)
            }
        }
        changeNameButton.setOnClickListener {
            changeCompanyName()
        }
        companyNameLayout.addView(companyNameTextView)
        companyNameLayout.addView(changeNameButton)
        companySetLayout.addView(companyTextView)
        companySetLayout.addView(companyNameLayout)
        settingsLayout.addView(companySetLayout)

        val usersSetLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }
        val usersTextView = TextView(this).apply {
            text = "Users:"
            textSize = 30f
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        usersSetLayout.addView(usersTextView)
        settings.users.forEach { user ->
            val manageUserLayout = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 8, 0, 8)
                }
                orientation = LinearLayout.VERTICAL
                val backgroundDrawable = GradientDrawable().apply {
                    setColor(Color.WHITE) // Barva pozadí
                    cornerRadius = 30f // Zaoblení rohů v pixelech
                }
                background = backgroundDrawable
                gravity = Gravity.CENTER
                setPadding(16, 16, 16, 16)
            }
            val userNameTextView = TextView(this).apply {
                text = user.name
                textSize = 25f
                setPadding(16, 16, 16, 16)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            val userPositionTextView = TextView(this).apply {
                text = user.authorization
                textSize = 20f
                setPadding(16, 16, 16, 16)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(16, 0, 0, 8)
                }
            }
            val manageUserSettingsTag = manageUserSettingsTag(user.id, user.name, user.authorization)
            val manageButton = MaterialButton(this).apply {
                text = "manage user"
                textSize = 20f
                setPadding(16, 16, 16, 16)
                tag = manageUserSettingsTag
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(16, 0, 0, 8)
                }
            }
            manageButton.setOnClickListener {
                val tag = manageButton.tag as manageUserSettingsTag
               manageUserSettings(tag.name, tag.id)
            }
            manageUserLayout.addView(userNameTextView)
            manageUserLayout.addView(userPositionTextView)
            if (Authorization.equals("owner")){
                manageUserLayout.addView(manageButton)
            } else if (user.authorization.equals("employee")){
                manageUserLayout.addView(manageButton)
            }
            usersSetLayout.addView(manageUserLayout)
        }
        settingsLayout.addView(usersSetLayout)

        val notificationsSetLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }
        val notificationsTextView = TextView(this).apply {
            text = "Notifications:"
            textSize = 30f
            setPadding(0, 0, 0, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT  ,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        notificationsSetLayout.addView(notificationsTextView)
        val notificationLayout1 = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
            orientation = LinearLayout.VERTICAL
            val backgroundDrawable = GradientDrawable().apply {
                setColor(Color.WHITE) // Barva pozadí
                cornerRadius = 30f // Zaoblení rohů v pixelech
            }
            background = backgroundDrawable
            gravity = Gravity.CENTER
            setPadding(16, 16, 16, 16)
        }
        val seatedNotTextView = TextView(this).apply {
            text = "Time period for seated table Notifications: ${seatedNotificationTimePeriod}"
            textSize = 22f
            setPadding(8, 8, 8, 8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val changeSeatedNotPeriodButton = MaterialButton(this).apply {
            text = "Change"
            textSize = 20f
            setPadding(8, 8, 8, 8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8, 0, 0, 8)
            }
        }
        changeSeatedNotPeriodButton.setOnClickListener {
            changeNotificationPeriod("seated")
        }
        notificationLayout1.addView(seatedNotTextView)
        notificationLayout1.addView(changeSeatedNotPeriodButton)
        notificationsSetLayout.addView(notificationLayout1)

        val notificationLayout2 = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
            orientation = LinearLayout.VERTICAL
            val backgroundDrawable = GradientDrawable().apply {
                setColor(Color.WHITE) // Barva pozadí
                cornerRadius = 30f // Zaoblení rohů v pixelech
            }
            background = backgroundDrawable
            gravity = Gravity.CENTER
            setPadding(16, 16, 16, 16)
        }
        val eatingNotTextView = TextView(this).apply {
            text = "Time period for eating table Notifications: ${eatingNotificationTimePeriod}"
            textSize = 22f
            setPadding(8, 8, 8, 8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val changeEatingNotPeriodButton = MaterialButton(this).apply {
            text = "Change"
            textSize = 20f
            setPadding(8, 8, 8, 8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8, 0, 0, 8)
            }
        }
        changeEatingNotPeriodButton.setOnClickListener {
            changeNotificationPeriod("eating")
        }
        notificationLayout2.addView(eatingNotTextView)
        notificationLayout2.addView(changeEatingNotPeriodButton)
        notificationsSetLayout.addView(notificationLayout2)

        val notificationLayout3 = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
            orientation = LinearLayout.VERTICAL
            val backgroundDrawable = GradientDrawable().apply {
                setColor(Color.WHITE) // Barva pozadí
                cornerRadius = 30f // Zaoblení rohů v pixelech
            }
            background = backgroundDrawable
            gravity = Gravity.CENTER
            setPadding(16, 16, 16, 16)
        }
        val paidNotTextView = TextView(this).apply {
            text = "Time period for paid table Notifications: ${paidNotificationTimePeriod}"
            textSize = 22f
            setPadding(8, 8, 8, 8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val changePaidNotPeriodButton = MaterialButton(this).apply {
            text = "Change"
            textSize = 20f
            setPadding(8, 8, 8, 8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8, 0, 0, 8)
            }
        }
        changePaidNotPeriodButton.setOnClickListener {
            changeNotificationPeriod("paid")
        }
        notificationLayout3.addView(paidNotTextView)
        notificationLayout3.addView(changePaidNotPeriodButton)
        notificationsSetLayout.addView(notificationLayout3)

        settingsLayout.addView(notificationsSetLayout)

        // pro uzivatele employee pouze tlacitko pro odchod ze spolenosti
        if (Authorization.equals("employee")){
            settingsLayout.removeAllViews()
        }

        val leaveButtonLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8, 8, 8, 8)
            }
            orientation = LinearLayout.VERTICAL
            val backgroundDrawable = GradientDrawable().apply {
                setColor(Color.WHITE) // Barva pozadí
                cornerRadius = 30f // Zaoblení rohů v pixelech
            }
            background = backgroundDrawable
            gravity = Gravity.CENTER
            setPadding(16, 16, 16, 16)
        }
        val leaveCompanyButton = MaterialButton(this).apply {
            text = "Leave this company"
            textSize = 20f
            setPadding(15, 10, 10, 15)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8, 0, 0, 8)
            }
        }
        leaveCompanyButton.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Leave company?")
            builder.setMessage("Are you sure you want to leave this company?")

            builder.setPositiveButton("Yes") { dialog1, _ ->
                val companyRef = userId?.let { it1 ->
                    db.child("companies").child(CompanyID).child("users").child(it1)
                    }
                companyRef?.removeValue()?.addOnSuccessListener {
                    val userRef = userId?.let { it1 -> db.child("users").child(it1).child("companies").child(CompanyID) }
                    userRef?.removeValue()?.addOnSuccessListener {
                        // Přesun na Company menu obrazovku
                        val intent = Intent(this, CompanyMenu::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }?.addOnFailureListener {
                        Log.e("failure2", "failed to remove company from user")
                    }
                }?.addOnFailureListener {
                    Log.e("failure1", "failed to remove user from company")
                }
            }

            builder.setNegativeButton("No") { dialog1, _ ->
                dialog1.dismiss()
            }
            val alertDialog = builder.create()
            alertDialog.show()
        }
        leaveButtonLayout.addView(leaveCompanyButton)
        if (!Authorization.equals("owner")){
            settingsLayout.addView(leaveButtonLayout)
        }
    }

    private fun changeNotificationPeriod(type: String){
        // Vytvoření dialogu
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.change_parameters_for_menu_elements)

        // Nastavení velikosti dialogu
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.8).toInt(),
            (resources.displayMetrics.heightPixels * 0.7).toInt()
        )
        // Reference na prvky v popup layoutu
        val textView = dialog.findViewById<TextView>(R.id.parametr_view)
        when(type){
            "seated" -> textView.text = "seated table notification period"
            "eating" -> textView.text = "eating table notification period"
            "paid" -> textView.text = "paid table notification period"
        }
        val parametrToChange = dialog.findViewById<TextInputEditText>(R.id.parameter_to_change)
        parametrToChange.hint = "New period"
        val closeButton = dialog.findViewById<Button>(R.id.close_change_parameters_button)
        closeButton.setOnClickListener {
            dialog.dismiss()
        }
        val changePeriodButton = dialog.findViewById<Button>(R.id.change_group_name_button)
        changePeriodButton.text = "Change period"
        changePeriodButton.setOnClickListener {

            val newPeriod = parametrToChange.text.toString().toIntOrNull()

            if (newPeriod == null || newPeriod < 5 || newPeriod > 45) {
                Toast.makeText(this, "Please enter a whole number between 5 and 45 minutes", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val databaseRef = db.child("companies").child(CompanyID).child("settings")

            when (type) {
                "seated" -> databaseRef.child("seatedNotification").setValue(newPeriod)
                "eating" -> databaseRef.child("eatingNotification").setValue(newPeriod)
                "paid" -> databaseRef.child("paidNotification").setValue(newPeriod)
            }
            dialog.dismiss()
            fetchDataForSettings {
                drawSettings()
            }
        }
        dialog.show()
    }

    private fun manageUserSettings(userName: String, userId: String){
        // Vytvoření dialogu
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.manage_user_settings_layout)

        // Nastavení velikosti dialogu
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.8).toInt(),
            (resources.displayMetrics.heightPixels * 0.7).toInt()
        )
        val textView = dialog.findViewById<TextView>(R.id.textView12)
        textView.text = userName
        val promoteButton = dialog.findViewById<TextView>(R.id.promote_button)
        promoteButton.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Promote user")
            builder.setMessage("Are you sure you want to promote this user to manager?")

            builder.setPositiveButton("Yes") { dialog1, _ ->
                val companyRef = db.child("companies").child(CompanyID).child("users").child(userId).child("authorization")
                companyRef.setValue("manager").addOnSuccessListener {
                    Log.d("Firebase", "user position updated successfully")
                    val positionRef = db.child("users").child(userId).child("companies").child(CompanyID).child("authorization")
                    positionRef.setValue("manager").addOnSuccessListener {
                        dialog1.dismiss()
                        dialog.dismiss()
                        fetchDataForSettings { drawSettings() }
                        Log.d("Firebase", "user position updated successfully")
                    }.addOnFailureListener {
                        Log.e("Firebase", "Failed to update user position: ${it.message}")
                    }
                }.addOnFailureListener {
                    Log.e("Firebase", "Failed to update user position: ${it.message}")
                }
            }

            builder.setNegativeButton("No") { dialog1, _ ->
                dialog1.dismiss()
                dialog.dismiss()
            }

            val alertDialog = builder.create()
            alertDialog.show()
        }
        val kickButton = dialog.findViewById<TextView>(R.id.kick_button)
        kickButton.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Kick user")
            builder.setMessage("Are you sure you want to kick this user from the company?")

            builder.setPositiveButton("Yes") { dialog1, _ ->
                val companyRef = db.child("companies").child(CompanyID).child("users").child(userId)
                companyRef.removeValue().addOnSuccessListener {
                    Log.d("Firebase", "user kicked successfully")
                    val positionRef = db.child("users").child(userId).child("companies").child(CompanyID)
                    positionRef.removeValue().addOnSuccessListener {
                        dialog1.dismiss()
                        dialog.dismiss()
                        fetchDataForSettings { drawSettings() }
                        Log.d("Firebase", "user kicked successfully")
                    }.addOnFailureListener {
                        Log.e("Firebase", "Failed to kick the user: ${it.message}")
                    }
                }.addOnFailureListener {
                    Log.e("Firebase", "Failed to kick the user: ${it.message}")
                }
            }

            builder.setNegativeButton("No") { dialog1, _ ->
                dialog1.dismiss()
                dialog.dismiss()
            }

            val alertDialog = builder.create()
            alertDialog.show()
        }
        val closeButton = dialog.findViewById<TextView>(R.id.close_user_settings_button)
        closeButton.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun changeCompanyName(){
        // Vytvoření dialogu
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.change_parameters_for_menu_elements)

        // Nastavení velikosti dialogu
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.8).toInt(),
            (resources.displayMetrics.heightPixels * 0.7).toInt()
        )
        // Reference na prvky v popup layoutu
        val textView = dialog.findViewById<TextView>(R.id.parametr_view)
        textView.text = "Company name"
        val parametrToChange = dialog.findViewById<TextInputEditText>(R.id.parameter_to_change)
        parametrToChange.hint = "New company name"
        val changeTextButton = dialog.findViewById<Button>(R.id.change_group_name_button)
        changeTextButton.text = "Change name"
        val closeButton = dialog.findViewById<Button>(R.id.close_change_parameters_button)
        closeButton.setOnClickListener {
            dialog.dismiss()
        }
        changeTextButton.setOnClickListener {
            val newName = parametrToChange.text.toString().trim()
            if (newName.isEmpty()){
                Toast.makeText(dialog.context, "You must enter the name first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val companyRef = db.child("companies").child(CompanyID).child("name")
            companyRef.setValue(newName).addOnSuccessListener {
                Log.d("Firebase", "Company name updated successfully")

                //projdeme všechny uživatele a přepíšeme název společnosti u nich
                db.child("users").get().addOnSuccessListener { usersSnapshot ->
                    for (userSnapshot in usersSnapshot.children) {
                        val userId = userSnapshot.key ?: continue
                        val companiesNode = userSnapshot.child("companies")

                        for (companySnapshot in companiesNode.children) {
                            val storedCompanyId = companySnapshot.key
                            if (storedCompanyId == CompanyID) {
                                db.child("users").child(userId)
                                    .child("companies").child(CompanyID).child("companyName")
                                    .setValue(newName)
                                    .addOnSuccessListener {
                                        fetchDataForSettings {
                                            drawSettings()
                                        }
                                        Log.d("Firebase", "Company name updated for user: $userId")
                                    }
                                    .addOnFailureListener {
                                        Log.e("Firebase", "Failed to update company name for user: $userId")
                                    }
                            }
                        }
                    }
                }.addOnFailureListener {
                    Log.e("Firebase", "Failed to fetch users: ${it.message}")
                }

            }.addOnFailureListener {
                Log.e("Firebase", "Failed to update company name: ${it.message}")
            }

            dialog.dismiss()
        }
        dialog.show()
    }
    //metoda pro nacteni dat do objektu settings
    private fun fetchDataForSettings(onComplete: () -> Unit){
        val companyRef = db.child("companies").child(CompanyID)

        companyRef.child("name").get().addOnSuccessListener { nameSnapshot ->
            val companyName = nameSnapshot.getValue(String::class.java) ?: ""

            companyRef.child("users").get().addOnSuccessListener { usersSnapshot ->
                val usersList = mutableListOf<User>()

                for (userSnapshot in usersSnapshot.children) {
                    val userId = userSnapshot.key ?: continue
                    val username = userSnapshot.child("username").getValue(String::class.java) ?: ""
                    val authorization = userSnapshot.child("authorization").getValue(String::class.java) ?: ""

                    usersList.add(User(id = userId, name = username, authorization = authorization))
                }
                companyRef.child("settings").get().addOnSuccessListener { settingsSnapshot ->
                    val seatedNot = settingsSnapshot.child("seatedNotification").getValue(Int::class.java) ?: 5
                    val eatingNot = settingsSnapshot.child("eatingNotification").getValue(Int::class.java) ?: 5
                    val paidNot = settingsSnapshot.child("paidNotification").getValue(Int::class.java) ?: 5

                    // Aktualizace globální proměnné settings
                    settings = Settings(companyName, usersList, seatedNot, eatingNot, paidNot)

                    // Zavolání callbacku po úspěšném načtení dat
                    onComplete()
                }.addOnFailureListener {
                    Log.e("Firebase", "Chyba při načítání settings: ${it.message}")
                }
            }.addOnFailureListener {
                Log.e("Firebase", "Chyba při načítání uživatelů: ${it.message}")
            }
        }.addOnFailureListener {
            Log.e("Firebase", "Chyba při načítání názvu společnosti: ${it.message}")
        }
    }

    private fun tableNotificationPopup(){
        tableNotificationDialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            (resources.displayMetrics.heightPixels * 0.85).toInt()
        )
        val closeButton = tableNotificationDialog.findViewById<Button>(R.id.close_table_notifications_button)
        closeButton.setOnClickListener {
            tableNotificationDialog.dismiss()
        }
        drawNotifications()
        //showBadge(false)
        tableNotificationDialog.show()
    }

    private fun drawNotifications(){
        notificationsLayout.removeAllViews()
        notificationsList.forEach { notification ->
            if (notification.send){
                when(notification.type){
                    "seated" -> createNotification(notification,"Check table ${notification.tableName} if customers have picked their order!")
                    "eating" -> createNotification(notification,"Check table ${notification.tableName} if customers want something else!")
                    "paid" -> createNotification(notification, "Table ${notification.tableName} needs to be cleaned!")
                }
            }
        }
    }
    //TODO
    private fun createNotification(notification: Notification, message: String){
        val linearLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(32, 0, 5, 0)
            }
            orientation = LinearLayout.HORIZONTAL
            val backgroundDrawable = GradientDrawable().apply {
                setColor(Color.WHITE) // Barva pozadí
                cornerRadius = 30f // Zaoblení rohů v pixelech
            }
            background = backgroundDrawable
            gravity = Gravity.CENTER
            setPadding(16, 16, 16, 16)
        }
        val confirmButton = ImageButton(this).apply {
            setImageResource(R.drawable.baseline_done_24)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 16, 16, 16)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setBackgroundColor(Color.GREEN)
        }
        confirmButton.setOnClickListener {
            val notificationRef = db.child("companies").child(CompanyID).child("Notifications").child(notification.id)
            notificationRef.removeValue().addOnSuccessListener {
                // Vytvoření nové notifikace s časem posunutým o periodu
                val period: Int
                when(notification.type){
                    "seated" -> period = seatedTableNotificationPeriod
                    "eating" -> period = eatingTableNotificationPeriod
                    "paid" -> period = paidTableNotificationPeriod
                    else -> period = 5
                }
                val newNotificationRef = db.child("companies").child(CompanyID).child("Notifications").push()
                val newNotification = notification.copy(
                    id = newNotificationRef.key!!,
                    timeToSend = System.currentTimeMillis() + (period * 60 * 1000), // Přidá 5 minut
                    send = false
                )

                newNotificationRef.setValue(newNotification).addOnSuccessListener {
                    drawNotifications()
                }
            }.addOnFailureListener {
                Log.e("Firebase", "Chyba při mazání notifikace: ${it.message}")
            }
            // update analytics
            updateUserDataInAnalytics()
        }
        val itemView = TextView(this).apply {
            text = message
            textSize = 18f
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8,0,0,0)
            }
        }
        linearLayout.addView(confirmButton)
        linearLayout.addView(itemView)
        notificationsLayout.addView(linearLayout)
    }

    private fun updateUserDataInAnalytics(){
        val userRef = userId?.let {
                db.child("companies").child(CompanyID).child("Analytics").child("users").child(it).child("activity")
            }
        userRef?.get()?.addOnSuccessListener { snapshot ->
            val currentValue = snapshot.getValue(Int::class.java) ?: 0
            val newValue = currentValue + 1

            userRef.setValue(newValue)
        }?.addOnFailureListener { error ->
            Log.e("Firebase", "Chyba při načítání numberOfServedTimes: ${error.message}")
        }
    }

    private fun createNotificationChannel(context: Context) {
        val name = "WaitR Channel"
        val descriptionText = "Notifikace pro WaitR"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("waitr_channel_id", name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
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
        val closeButton = dialog.findViewById<Button>(R.id.close_invite_users_button)
        closeButton.setOnClickListener {
            dialog.dismiss()
        }

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

    //Metoda pro poslani notifikace
    private fun sendNotification(context: Context,tableName: String, type: String) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val notification = NotificationCompat.Builder(context, "waitr_channel_id")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(tableName)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)  // Zmizí po kliknutí

        when (type){
            "seated" -> notification.setContentText("Check table ${tableName} if customers have picked their order!")
            "eating" -> notification.setContentText("Check table ${tableName} if customers want something else!")
            "paid" -> notification.setContentText("Table ${tableName} needs to be cleaned!")
            else -> {}
        }

        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification.build())
    }

    //Listener pro notifikace
    private fun startListeningForNotifications() {
        val ref = db.child("companies").child(CompanyID).child("Notifications")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                notificationsList.clear()
                for (child in snapshot.children) {
                    val notification = child.getValue(Notification::class.java)
                    notification?.let { notificationsList.add(it) }
                }
                checkAndSendNotifications()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error loading notifications: ${error.message}")
            }
        })
    }

    //Zkontroluje jestli je cas poslat notifikaci
    fun checkAndSendNotifications() {
        val currentTime = System.currentTimeMillis()
        var hasUnsentNotifications = false

        notificationsList.forEach { notification ->
            if (notification.timeToSend <= currentTime && !notification.send) {
                sendNotification(this, notification.tableName, notification.type)
                // Aktualizace hodnoty "send" na true v Firebase
                val notificationRef = db.child("companies")
                    .child(CompanyID)
                    .child("Notifications")
                    .child(notification.id)
                notificationRef.child("send").setValue(true)
                hasUnsentNotifications = true
            }
            if (notification.send) hasUnsentNotifications = true
        }
        showBadge(hasUnsentNotifications)
    }

    //vymaze notifikaci
    private fun removeNotificationFromFirebase(id: String) {
        db.child("companies").child(CompanyID).child("Notifications").child(id).removeValue()
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
        val usersRef = db.child("companies").child(CompanyID).child("users")
        super.onDestroy()
        // Odstraní posluchač při ukončení aktivity
        valueEventListener?.let { usersRef.removeEventListener(it) }
        // Nastavení statusu na "offline"
        val onlineStatusMap = mapOf(
            "status" to "offline"
        )

        CompanyID.let { companyId ->
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
        stopCheckingNotifications()
    }
    //získání useru z menu a rozdělení na online a offline
    private fun fetchUsers(){
        allUsersList.clear()
        onlineMembers.clear()
        offlineMembers.clear()
        val usersRef = db.child("companies").child(CompanyID).child("users")
        usersRef.get()
            .addOnSuccessListener { usersDataSnapshot ->
                for(user in usersDataSnapshot.children){
                    user.key?.let { allUsersList.add(it) }
                    val userName = user.child("username").getValue(String::class.java)
                    val userStatus = user.child("status").getValue(String::class.java)
                    if (userStatus == "online" && userName != null){
                        onlineMembers.add(userName)
                    }
                    if (userStatus == "offline" && userName != null){
                        offlineMembers.add(userName)
                    }
                }
                updateAnalytics()
                loadCurrentUsersToLayout()
            }
    }
    // upravi v NavigationDrawer menu polozku Members
    private fun currentMembersPopup(){
        loadCurrentUsersToLayout()
        val closePopup = currentMembersDialog.findViewById<Button>(R.id.close_current_users_popup)
        closePopup.setOnClickListener {
            currentMembersDialog.dismiss()
        }
        currentMembersDialog.show()
    }
    //dynamicke nacteni uzivatelu do layoutu
    private fun loadCurrentUsersToLayout(){
        displayOnlineUsers.removeAllViews()
        displayOfflineUsers.removeAllViews()
        Log.e("je list prazdny", onlineMembers.toString() + " "+ offlineMembers.toString() )

        for (user in onlineMembers){
            val userToDisplay = TextView(this).apply {
                text = user
                textSize = 20f
                setPadding(16, 16, 16, 16)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(5,5,5,5)
                }
                val backgroundDrawable = GradientDrawable().apply {
                    setColor(Color.WHITE)
                    cornerRadius = 30f
                }
                background = backgroundDrawable
            }
            displayOnlineUsers.addView(userToDisplay)
        }
        for (user in offlineMembers){
            val userToDisplay = TextView(this).apply {
                text = user
                textSize = 20f
                setPadding(16, 16, 16, 16)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(5,5,5,5)
                }
                val backgroundDrawable = GradientDrawable().apply {
                    setColor(Color.WHITE)
                    cornerRadius = 30f
                }
                background = backgroundDrawable
            }
            displayOfflineUsers.addView(userToDisplay)
        }
    }
    //updatne analytics
    private fun updateAnalytics(){
        val analyticsUsersList = mutableListOf<String>()

        val tablesRef = CompanyID.let { db.child("companies").child(it).child("Analytics").child("users") }
        tablesRef.get().addOnSuccessListener { snapshot ->
            analyticsUsersList.clear() // Vyčištění listu

            for (itemSnapshot in snapshot.children) {
                val itemId = itemSnapshot.key // Každý klíč je ID položky
                if (itemId != null) {
                    analyticsUsersList.add(itemId)
                }
            }
            Log.d("Analytics", "Načtené ID položek: $analyticsUsersList")

            val finalAnalyticsUsersList = mutableListOf<String>()

            analyticsUserTraversal(analyticsUsersList, finalAnalyticsUsersList)

            analyticsUsersList.forEach { table ->
                tablesRef.child(table).removeValue()
            }
            finalAnalyticsUsersList.forEach { table ->
                val tableMap = mapOf(
                    table to mapOf(
                        "numberOfServedTables" to 0,
                        "activity" to 0
                    )
                )
                tablesRef.updateChildren(tableMap)
            }

        }.addOnFailureListener { error ->
            Log.e("Firebase", "Chyba při načítání Analytics: ${error.message}")
        }
    }

    private fun analyticsUserTraversal(list: MutableList<String>, finalList: MutableList<String>){
            allUsersList.forEach { user ->
                if (list.contains(user)){
                    list.remove(user)
                } else {
                    finalList.add(user)
                }
            }
    }
    //nastaveni realtime listeneru
    private fun setupRealtimeListener() {
        val usersRef = db.child("companies").child(CompanyID).child("users")
        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                fetchUsers()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Failed to listen for changes: ${error.message}")
            }
        }
        usersRef.addValueEventListener(valueEventListener!!)
    }
    //listener jestli uzivatel nebyl vyhozen ze spolecnosti
    private fun startCompanyListener() {
        companyListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.hasChild(CompanyID)) {
                    // Uživatel už není členem této společnosti
                    Toast.makeText(this@Company_manager, "You have been removed from the company.", Toast.LENGTH_LONG).show()

                    // Přesměrování na CompanyMenu
                    val intent = Intent(this@Company_manager, CompanyMenu::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Failed to listen for company changes: ${error.message}")
            }
        }

        userCompaniesRef?.addValueEventListener(companyListener)
    }
    //listener pro zmeny v settings
    private fun listenForSettingsChanges() {
        val databaseRef = db.child("companies").child(CompanyID).child("settings")

        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                seatedTableNotificationPeriod = snapshot.child("seatedNotification").getValue(Int::class.java) ?: 5
                eatingTableNotificationPeriod = snapshot.child("eatingNotification").getValue(Int::class.java) ?: 5
                paidTableNotificationPeriod = snapshot.child("paidNotification").getValue(Int::class.java) ?: 5

                // Debug log (pokud chceš vidět změny v Logcat)
                Log.d("SettingsListener", "Updated settings: Seated=$seatedTableNotificationPeriod, Eating=$eatingTableNotificationPeriod, Paid=$paidTableNotificationPeriod")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SettingsListener", "Failed to read settings", error.toException())
            }
        })
    }
}