package com.mwysocki.smartwarehouse.viewmodels

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
import com.mwysocki.smartwarehouse.activities.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProfileViewModel(userId: String) : ViewModel() {
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