package com.example.waitr

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
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

class Login : AppCompatActivity() {
    private lateinit var editTextEmail: TextInputEditText
    private lateinit var editTextPassword: TextInputEditText
    private lateinit var loginButton: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var nacitaciKolecko: ProgressBar
    private lateinit var prihlasSe: TextView

    public override fun onStart() {
        super.onStart()
        FirebaseAuth.getInstance().signOut()
        // jestli uživatel je přihlášen, pošle ho to na jinou aktivitu
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
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        editTextEmail = findViewById(R.id.email)
        editTextPassword = findViewById(R.id.password)
        loginButton = findViewById(R.id.login_button)
        auth = Firebase.auth
        nacitaciKolecko = findViewById(R.id.nacitani)
        prihlasSe = findViewById(R.id.registrujse)
        prihlasSe.setOnClickListener(){
            val intent = Intent(applicationContext, Registration::class.java)
            startActivity(intent)
            finish()
        }
        //funkce tlačítka login
        loginButton.setOnClickListener {
            val email = editTextEmail.getText().toString()
            val password = editTextPassword.getText().toString()
            nacitaciKolecko.visibility = View.VISIBLE
            //ošetření nechtěných inputů
            if (TextUtils.isEmpty(email)){
                Toast.makeText(this, "Enter Email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(password)){
                Toast.makeText(this, "Enter Password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
        //přihlášení přes knihovní funkci FirebaseAuth
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    nacitaciKolecko.visibility = View.GONE
                    if (task.isSuccessful) {
                        Toast.makeText(
                            baseContext,
                            "Login Successful",
                            Toast.LENGTH_SHORT,
                        ).show()
                        val intent = Intent(applicationContext, CompanyMenu::class.java)
                        startActivity(intent)
                        finish()
                    } else {
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