package com.mwysocki.smartwarehouse.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.mwysocki.smartwarehouse.activities.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class SettingsViewModel : ViewModel() {
    private val _showAddUserDialog = MutableStateFlow(false)
    val showAddUserDialog: StateFlow<Boolean> = _showAddUserDialog.asStateFlow()

    private val _showDeleteUserDialog = MutableStateFlow(false)
    val showDeleteUserDialog: StateFlow<Boolean> = _showDeleteUserDialog.asStateFlow()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    init {
        fetchUsers()
    }

    fun showAddUserDialog() {
        _showAddUserDialog.value = true
    }

    fun hideAddUserDialog() {
        _showAddUserDialog.value = false
    }

    fun showDeleteUserDialog() {
        _showDeleteUserDialog.value = true
    }

    fun hideDeleteUserDialog() {
        _showDeleteUserDialog.value = false
    }

    private fun fetchUsers() {
        FirebaseFirestore.getInstance().collection("Users")
            .get()
            .addOnSuccessListener { result ->
                _users.value = result.toObjects(User::class.java)
            }
            .addOnFailureListener { e ->
                Log.e("SettingsViewModel", "Error fetching users: ${e.localizedMessage}")
            }
    }

    fun deleteUser(userId: String, onResult: (Boolean) -> Unit) {
        // Create a reference to the Firestore instance
        val firestore = FirebaseFirestore.getInstance()

        // Begin a batch write to perform multiple write operations as a single transaction
        val batch = firestore.batch()

        // Step 1: Update any packages that are assigned to this user
        firestore.collection("Packages")
            .whereEqualTo("assignedTo", userId)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val packageRef = firestore.collection("Packages").document(document.id)
                    // Update the 'assignedTo' field to an empty string
                    batch.update(packageRef, "assignedTo", "")
                }

                // Step 2: Delete the user
                val userRef = firestore.collection("Users").document(userId)
                batch.delete(userRef)

                // Commit the batch
                batch.commit().addOnSuccessListener {
                    Log.d("SettingsViewModel", "User and related packages updated successfully: $userId")
                    fetchUsers() // Refresh the list after deletion
                    onResult(true)
                }.addOnFailureListener { e ->
                    Log.e("SettingsViewModel", "Error updating user and packages: ${e.localizedMessage}")
                    onResult(false)
                }
            }.addOnFailureListener { e ->
                Log.e("SettingsViewModel", "Error finding packages for user: ${e.localizedMessage}")
                onResult(false)
            }
    }

    fun addUser(username: String, password: String, email: String, firstname: String, lastname: String, isManager: Boolean, onUserAdded: (Boolean) -> Unit) {
        // Check for required fields
        if(username.isBlank() || password.isBlank() || firstname.isBlank() || lastname.isBlank()) {
            onUserAdded(false)
            return
        }

        // Create a new user with a Firebase-generated ID
        val userRef = FirebaseFirestore.getInstance().collection("Users").document()


        val newUser = User(
            id = userRef.id,
            username = username,
            password = password,
            firstLogin = true,
            email = email,
            firstname = firstname,
            lastname = lastname,
            profilePictureUrl = "",
            isManager = isManager
        )

        userRef.set(newUser)
            .addOnSuccessListener {
                Log.d("SettingsViewModel", "User added successfully: $username")
                onUserAdded(true)
            }
            .addOnFailureListener { e ->
                Log.e("SettingsViewModel", "Error adding user: ${e.localizedMessage}")
                onUserAdded(false)
            }
    }


}