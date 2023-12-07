package com.mwysocki.smartwarehouse.ui.screens

import android.os.CountDownTimer
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mwysocki.smartwarehouse.activities.LoginActivity
import com.mwysocki.smartwarehouse.viewmodels.ProductsViewModel
import com.mwysocki.smartwarehouse.viewmodels.ProfileViewModel
import com.mwysocki.smartwarehouse.viewmodels.ProfileViewModelFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.graphics.asImageBitmap
import com.mwysocki.smartwarehouse.viewmodels.SharedViewModel

@OptIn(ExperimentalCoilApi::class)
@Composable
fun ProfileScreen(sharedViewModel: SharedViewModel) {
    val context = LocalContext.current
    val userId = LoginActivity.UserPrefs.getLoggedInUserId(context) ?: return

    // Use the factory to create the ViewModel
    val profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory(userId)
    )
    val userProfile by profileViewModel.userProfile.collectAsState()

    val triggerRecomposition by sharedViewModel.triggerRecomposition.collectAsState()

    LaunchedEffect(triggerRecomposition) {
        // This block will run every time 'triggerRecomposition' changes
        // You can perform actions here if needed, but its main purpose is to trigger recomposition
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier.fillMaxSize()
    ) {
        userProfile?.let { user ->
            if (user.profilePictureUrl.isNullOrEmpty()) {
                // If the profile picture URL is null or empty, show a placeholder
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.size(256.dp)
                )
            } else {
                // Load the profile picture from the URL
                Image(
                    painter = rememberImagePainter(user.profilePictureUrl),
                    contentDescription = "Profile Picture",
                    modifier = Modifier.size(200.dp).clip(CircleShape) // Clip as a circle
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Show first name and last name
            Text(text = "${user.firstname} ${user.lastname}", style = MaterialTheme.typography.titleMedium)

            // Show the username and email
            Text(text = "Username: ${user.username}")

            sharedViewModel.qrCodeBitmap.collectAsState().value?.let { qrBitmap ->
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier
                        .size(200.dp)
                        .padding(16.dp)
                )
            }

            CountdownTimerButton(sharedViewModel, user.id)

            // You can add more fields here...


        }
    }
}

@Composable
fun CountdownTimerButton(sharedViewModel: SharedViewModel, userId: String) {
    val timeLeft by sharedViewModel.timeLeft.collectAsState()

    Button(
        onClick = {
            sharedViewModel.startTimer()
            sharedViewModel.generateQRCode(userId) },
        enabled = !sharedViewModel.isTimerRunning()
    ) {
        Text(if (sharedViewModel.isTimerRunning()) "Timer: ${timeLeft}s" else "Create QR ID")
    }
    LaunchedEffect(sharedViewModel.timeLeft.collectAsState()) {
        if (sharedViewModel.timeLeft.value == 0) {
            sharedViewModel.onCountdownFinished()
        }
    }
}