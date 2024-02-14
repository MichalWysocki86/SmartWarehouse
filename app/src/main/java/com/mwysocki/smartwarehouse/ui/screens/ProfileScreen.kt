package com.mwysocki.smartwarehouse.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import coil.transform.CircleCropTransformation
import com.mwysocki.smartwarehouse.activities.LoginActivity
import com.mwysocki.smartwarehouse.viewmodels.ProfileViewModel
import com.mwysocki.smartwarehouse.viewmodels.ProfileViewModelFactory
import com.mwysocki.smartwarehouse.viewmodels.SharedViewModel

@OptIn(ExperimentalCoilApi::class)
@Composable
fun ProfileScreen(sharedViewModel: SharedViewModel) {
    val context = LocalContext.current
    val userId = LoginActivity.UserPrefs.getLoggedInUserId(context) ?: return
    val profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory(userId)
    )
    val userProfile by profileViewModel.userProfile.collectAsState()
    val triggerRecomposition by sharedViewModel.triggerRecomposition.collectAsState()
    LaunchedEffect(triggerRecomposition) {
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            profileViewModel.uploadImageToFirebaseStorage(it, context)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier.fillMaxSize()
    ) {
        userProfile?.let { user ->
            if (user.profilePictureUrl.isNullOrEmpty()) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .clickable { launcher.launch("image/*") }
                )
            } else {
                Image(
                    painter = rememberImagePainter(
                        data = user.profilePictureUrl,
                        builder = {
                            transformations(CircleCropTransformation())
                        }
                    ),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .clickable { launcher.launch("image/*") }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "${user.firstname} ${user.lastname}", style = MaterialTheme.typography.titleMedium)
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
        Text(if (sharedViewModel.isTimerRunning()) "${timeLeft}s" else "Create QR ID")
    }
    LaunchedEffect(sharedViewModel.timeLeft.collectAsState()) {
        if (sharedViewModel.timeLeft.value == 0) {
            sharedViewModel.onCountdownFinished()
        }
    }
}