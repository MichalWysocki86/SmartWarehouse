package com.mwysocki.smartwarehouse.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.mwysocki.smartwarehouse.activities.User

class ResetPasswordViewModel : ViewModel() {
    private val _message = MutableLiveData<String>()
    fun verifyOldPassword(username: String, oldPassword: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        FirebaseFirestore.getInstance().collection("Users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    onError("User not found")
                    Log.d("ResetPasswordViewModel", "User not found for username: $username")
                } else {
                    val user = documents.first().toObject(User::class.java)
                    if (user.password == oldPassword) {
                        onSuccess()
                        Log.d("ResetPasswordViewModel", "Old password verified for username: $username")
                    } else {
                        onError("Old password is incorrect")
                        Log.d("ResetPasswordViewModel", "Old password is incorrect for username: $username")
                    }
                }
            }
            .addOnFailureListener { e ->
                onError(e.localizedMessage ?: "Unknown error occurred")
                Log.e("ResetPasswordViewModel", "Error verifying old password: ${e.localizedMessage}")
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

        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            if (!snapshot.exists()) {
                onError("User not found")
                Log.e("ResetPasswordViewModel", "User not found for ID: $userId")
                return@runTransaction
            }

            val user = snapshot.toObject(User::class.java)
            if (user != null && user.password != newPassword) {
                transaction.update(userRef, "password", newPassword)
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
}