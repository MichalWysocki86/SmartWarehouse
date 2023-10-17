package com.mwysocki.smartwarehouse.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.firebase.auth.FirebaseAuth
import com.mwysocki.smartwarehouse.ui.screens.LoginScreen


class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoginScreen()
        }
    }
}

fun loginUser(context: Context, email: String, password: String, onError: (String) -> Unit) {
    val auth = FirebaseAuth.getInstance()
    auth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val intent = Intent(context, MainActivity::class.java)
                // Optionally, you can pass user data to MainActivity
                intent.putExtra("userEmail", email)
                context.startActivity(intent)
            } else {
                onError(task.exception?.message ?: "Unknown error")
            }
        }
}

