package com.mwysocki.smartwarehouse.viewmodels

import android.content.Context
import android.net.Uri
import android.os.CountDownTimer
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.mwysocki.smartwarehouse.activities.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProfileViewModel(private val userId: String) : ViewModel() {
    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile.asStateFlow()

    private val _timeLeft = mutableLongStateOf(0)
    val timeLeft: State<Long> = _timeLeft

    private var timer: CountDownTimer? = null

    init {
        loadUserProfile(userId)
    }

    private fun loadUserProfile(userId: String) {
        FirebaseFirestore.getInstance().collection("Users")
            .document(userId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                _userProfile.value = documentSnapshot.toObject(User::class.java)
            }
            .addOnFailureListener { e ->
                Log.e("ProfileViewModel", "Error loading user profile", e)
            }
    }


    override fun onCleared() {
        super.onCleared()
        timer?.cancel()
    }

    fun uploadImageToFirebaseStorage(imageUri: Uri, context: Context) {
        val storageRef = FirebaseStorage.getInstance().reference.child("profilePictures/$userId.jpg")
        val uploadTask = storageRef.putFile(imageUri)

        uploadTask.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                updateProfilePictureUrl(downloadUri.toString())
            }
        }.addOnFailureListener {
            // Handle failure
        }
    }

    private fun updateProfilePictureUrl(downloadUrl: String) {
        FirebaseFirestore.getInstance().collection("Users")
            .document(userId)
            .update("profilePictureUrl", downloadUrl)
            .addOnSuccessListener {
                _userProfile.value = _userProfile.value?.copy(profilePictureUrl = downloadUrl)
            }
            .addOnFailureListener {
                // Handle failure
            }
    }
}

// Factory for ProfileViewModel to pass the userId
class ProfileViewModelFactory(private val userId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            return ProfileViewModel(userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}