package com.example.waitr

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class Registration : AppCompatActivity() {
    private lateinit var editTextEmail: TextInputEditText
    private lateinit var editTextPassword: TextInputEditText
    private lateinit var editTextUsername: TextInputEditText
    private lateinit var registerButton: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var nacitaciKolecko: ProgressBar
    private lateinit var prihlasSe: TextView

    public override fun onStart() {
        super.onStart()
        FirebaseAuth.getInstance().signOut()

        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val intent = Intent(applicationContext, CompanyMenu::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registration)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        editTextEmail = findViewById(R.id.email)
        editTextPassword = findViewById(R.id.password)
        editTextUsername = findViewById(R.id.username)
        registerButton = findViewById(R.id.register_button)
        auth = Firebase.auth
        nacitaciKolecko = findViewById(R.id.nacitani)
        prihlasSe = findViewById(R.id.loginnow)
        prihlasSe.setOnClickListener(){
            val intent = Intent(applicationContext, Login::class.java)
            startActivity(intent)
            finish()
        }

        registerButton.setOnClickListener {
            val email = editTextEmail.text.toString()
            val password = editTextPassword.text.toString()
            val username = editTextUsername.text.toString()
            nacitaciKolecko.visibility = View.VISIBLE

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(this, "Enter Email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(this, "Enter Password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(username)) {
                Toast.makeText(this, "Enter Username", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    nacitaciKolecko.visibility = View.GONE
                    if (task.isSuccessful) {
                        // Získání UID vytvořeného uživatele
                        val userId = auth.currentUser?.uid
                        if (userId != null) {
                            // Přidání uživatele do Firestore
                            val userMap = hashMapOf(
                                "username" to username,
                                "email" to email
                            )

                            val db = Firebase.firestore
                            db.collection("users").document(userId)
                                .set(userMap)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        baseContext,
                                        "Account created and saved",
                                        Toast.LENGTH_SHORT,
                                    ).show()

                                    // Přesměrování na přihlašovací obrazovku
                                    val intent = Intent(applicationContext, Login::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        baseContext,
                                        "Failed to save user: ${e.message}",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                        }
                    } else {
                        // Pokud registrace selže
                        Toast.makeText(
                            baseContext,
                            "Authentication failed",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
        }
    }
}