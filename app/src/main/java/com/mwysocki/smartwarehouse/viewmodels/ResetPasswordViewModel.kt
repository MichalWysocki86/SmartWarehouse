package com.mwysocki.smartwarehouse.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.mwysocki.smartwarehouse.activities.User
import java.security.MessageDigest

class ResetPasswordViewModel : ViewModel() {
    private val _message = MutableLiveData<String>()
    fun verifyOldPassword(username: String, oldPassword: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        FirebaseFirestore.getInstance().collection("Users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    onError("User not found")
                } else {
                    val user = documents.first().toObject(User::class.java)
                    if (user != null) {
                        // Check if it's the first login (password not hashed) or subsequent login (password hashed)
                        if (user.firstLogin) {
                            // For first login, compare the passwords directly
                            if (user.password == oldPassword) {
                                onSuccess()
                            } else {
                                onError("Old password is incorrect")
                            }
                        } else {
                            // For subsequent logins, hash the old password before comparing
                            val hashedOldPassword = hashPassword(oldPassword)
                            if (user.password == hashedOldPassword) {
                                onSuccess()
                            } else {
                                onError("Old password is incorrect")
                            }
                        }
                    } else {
                        onError("Invalid user data")
                    }
                }
            }
            .addOnFailureListener { e ->
                onError(e.localizedMessage ?: "Unknown error occurred")
            }
    }

    fun updatePassword(userId: String, newPassword: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("Users").document(userId)

        if (userId.isBlank()) {
            onError("Invalid User ID")
            Log.e("ResetPasswordViewModel", "Invalid User ID: $userId")
            return
        }

        // Hash the new password before saving it
        val hashedPassword = hashPassword(newPassword)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            if (!snapshot.exists()) {
                onError("User not found")
                Log.e("ResetPasswordViewModel", "User not found for ID: $userId")
                return@runTransaction
            }

            val user = snapshot.toObject(User::class.java)
            if (user != null && user.password != hashedPassword) {
                transaction.update(userRef, "password", hashedPassword)
                transaction.update(userRef, "firstLogin", false)
                Log.d("ResetPasswordViewModel", "Password updated for userId: $userId")
            }
        }.addOnSuccessListener {
            _message.postValue("Password updated successfully")
            onSuccess()
        }.addOnFailureListener { e ->
            _message.postValue(e.localizedMessage ?: "Unknown error occurred")
            onError(e.localizedMessage ?: "Unknown error occurred")
        }
    }

    // Hashing method
    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}