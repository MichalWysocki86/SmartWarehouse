package com.mwysocki.smartwarehouse.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.firebase.auth.FirebaseAuth
import com.mwysocki.smartwarehouse.activities.LoginActivity.LoginManager.setLoggedIn
import com.mwysocki.smartwarehouse.ui.screens.MainApp
import com.mwysocki.smartwarehouse.ui.screens.MainScreen
import com.mwysocki.smartwarehouse.viewmodels.PackagesViewModel

data class NavigationItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int? = null,
    val route: MainScreen
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val packagesViewModel: PackagesViewModel by viewModels()
        setContent {
            MainApp(
                packagesViewModel = packagesViewModel,
                logoutUser = ::logoutUser
            )
        }
    }

    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        setLoggedIn(this, false)
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        this.startActivity(intent)
    }
}
