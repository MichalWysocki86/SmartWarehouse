package com.mwysocki.smartwarehouse.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.google.firebase.firestore.FirebaseFirestore
import com.mwysocki.smartwarehouse.ui.screens.ResetPasswordScreen
import com.mwysocki.smartwarehouse.viewmodels.ResetPasswordViewModel

class ResetPasswordActivity : ComponentActivity() {

    private val viewModel by viewModels<ResetPasswordViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        setContent {
            ResetPasswordScreen(onPasswordChangeRequested = { oldPassword, newPassword, confirmPassword ->
                if (newPassword == confirmPassword) {
                    val username = LoginActivity.UserPrefs.getLoggedInUsername(this)
                    val userId = LoginActivity.UserPrefs.getLoggedInUserId(this)

                    if (username != null && userId != null) {
                        viewModel.verifyOldPassword(username, oldPassword, onSuccess = {
                            viewModel.updatePassword(userId, newPassword, onSuccess = {

                                navigateToMainActivity()
                            }, onError = { errorMsg ->
                                //Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
                            })
                        }, onError = { errorMsg ->
                            //Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
                        })
                    }
                } else {

                }
            })
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        // Check if the activity is still valid before navigating
        if (!isFinishing) {
            startActivity(intent)
            finish()
            Log.d("ResetPasswordActivity", "Navigating to MainActivity")
            Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show()
        } else {
            Log.d("ResetPasswordActivity", "Activity finishing, not navigating")
        }
    }

}