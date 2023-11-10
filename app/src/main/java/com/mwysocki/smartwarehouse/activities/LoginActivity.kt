package com.mwysocki.smartwarehouse.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mwysocki.smartwarehouse.ui.screens.LoginScreen
import com.google.firebase.firestore.FirebaseFirestore

data class User(
    val username: String = "",
    val password: String = "" // add hashing??
)

class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if the user is already logged in
        val isLoggedIn = LoginManager.getLoginStatus(this)
        if (isLoggedIn) {
            navigateToMainActivity()
        } else {
            setContent {
                LoginScreen()
            }
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    // Inner class for Login related functions
    object LoginManager {
        fun loginUser(context: Context, username: String, password: String, onError: (String) -> Unit) {
            val db = FirebaseFirestore.getInstance()

            db.collection("Users")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener { result ->
                    if (result.isEmpty) {
                        onError("Username not found")
                    } else {
                        val user = result.documents[0].toObject(User::class.java)
                        if (user != null && user.password == password) {
                            setLoggedIn(context, true)
                            UserPrefs.setLoggedInUsername(context, username)
                            if (context is LoginActivity) {
                                context.navigateToMainActivity()
                            }
                        } else {
                            onError("Invalid password")
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    onError(exception.message ?: "Unknown error")
                }
        }

        fun setLoggedIn(context: Context, loggedIn: Boolean) {
            val sharedPref = context.getSharedPreferences("appPrefs", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putBoolean("isLoggedIn", loggedIn)
                apply()
            }
        }

        fun getLoginStatus(context: Context): Boolean {
            val sharedPref = context.getSharedPreferences("appPrefs", Context.MODE_PRIVATE)
            return sharedPref.getBoolean("isLoggedIn", false)
        }
    }

    object UserPrefs {
        fun getLoggedInUsername(context: Context): String? {
            val sharedPref = context.getSharedPreferences("appPrefs", Context.MODE_PRIVATE)
            return sharedPref.getString("loggedInUsername", null)
        }

        fun setLoggedInUsername(context: Context, username: String) {
            val sharedPref = context.getSharedPreferences("appPrefs", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putString("loggedInUsername", username)
                apply()
            }
        }
    }
}