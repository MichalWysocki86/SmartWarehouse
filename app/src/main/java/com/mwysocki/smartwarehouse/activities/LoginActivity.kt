package com.mwysocki.smartwarehouse.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mwysocki.smartwarehouse.ui.screens.LoginScreen
import com.google.firebase.firestore.FirebaseFirestore
import java.security.MessageDigest

data class User(
    var id: String = "",
    val username: String = "",
    var password: String = "",
    var firstLogin: Boolean = true,
    var profilePictureUrl: String? = null,
    var email: String = "",
    var firstname: String = "",
    var lastname: String = "",
    var isManager: Boolean = true
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

    private fun navigateToResetPasswordActivity() {
        val intent = Intent(this, ResetPasswordActivity::class.java)
        startActivity(intent)
        finish()
    }

    // Inner class for Login related functions
    object LoginManager {
        fun loginUser(context: Context, username: String, enteredPassword: String, onError: (String) -> Unit) {
            val db = FirebaseFirestore.getInstance()

            db.collection("Users")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener { result ->
                    if (result.isEmpty) {
                        onError("Username not found")
                    } else {
                        val user = result.documents[0].toObject(User::class.java)
                        if (user != null) {
                            Log.d("LoginManager", "Retrieved user: ${user.username}, IsManager: ${user.isManager}")
                            // Check if the user is logging in for the first time and password is not hashed
                            if (user.firstLogin) {
                                // Compare entered password directly with stored password
                                if (user.password == enteredPassword) {
                                    proceedWithLogin(context, user, username)
                                } else {
                                    onError("Invalid password")
                                }
                            } else {
                                // Hash the entered password and compare it with the stored hashed password
                                val hashedEnteredPassword = hashPassword(enteredPassword)
                                if (user.password == hashedEnteredPassword) {
                                    proceedWithLogin(context, user, username)
                                } else {
                                    onError("Invalid password")
                                }
                            }
                        } else {
                            onError("Invalid user data")
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    onError(exception.message ?: "Unknown error")
                }
        }

        private fun proceedWithLogin(context: Context, user: User, username: String) {
            setLoggedIn(context, true)
            UserPrefs.setLoggedInUsername(context, username)
            UserPrefs.setLoggedInUserId(context, user.id)
            UserPrefs.setLoggedInUserIsManager(context, user.isManager)

            if (user.firstLogin && context is LoginActivity) {
                context.navigateToResetPasswordActivity() // Redirect to ResetPasswordActivity
            } else if (context is LoginActivity) {
                context.navigateToMainActivity()
            }
        }


        // Reuse the same hashPassword function from your ResetPasswordViewModel
        private fun hashPassword(password: String): String {
            val bytes = password.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            return digest.fold("", { str, it -> str + "%02x".format(it) })
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
            // The context object is correctly used to call getSharedPreferences
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

        fun setLoggedInUserId(context: Context, userId: String) {
            val sharedPref = context.getSharedPreferences("appPrefs", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putString("loggedInUserId", userId)
                apply()
            }
        }

        fun getLoggedInUserId(context: Context): String? {
            val sharedPref = context.getSharedPreferences("appPrefs", Context.MODE_PRIVATE)
            return sharedPref.getString("loggedInUserId", null)
        }

        fun setLoggedInUserIsManager(context: Context, isManager: Boolean) {
            val sharedPref = context.getSharedPreferences("appPrefs", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putBoolean("loggedInUserIsManager", isManager)
                apply()
            }
        }

        fun getLoggedInUserIsManager(context: Context): Boolean {
            val sharedPref = context.getSharedPreferences("appPrefs", Context.MODE_PRIVATE)
            return sharedPref.getBoolean("loggedInUserIsManager", false)
        }
    }
}