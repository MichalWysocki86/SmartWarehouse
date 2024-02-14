package com.mwysocki.smartwarehouse.ui.screens

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mwysocki.smartwarehouse.activities.LoginActivity
import com.mwysocki.smartwarehouse.activities.ResetPasswordActivity
import com.mwysocki.smartwarehouse.activities.User
import com.mwysocki.smartwarehouse.viewmodels.SettingsViewModel

@Composable
fun SettingsScreen(settingsViewModel: SettingsViewModel, context: Context) {
    var settingsList = listOf("Change password")
    val isManager = LoginActivity.UserPrefs.getLoggedInUserIsManager(context)
    Log.d("SettingsScreen", "Is user a manager? $isManager")

    if (isManager) {
        settingsList += listOf("Add New User", "Delete User")
    }
    LazyColumn {
        items(settingsList) { settingName ->
            SettingItem(settingName = settingName) {
                when (settingName) {
                    "Change password" -> {
                        val intent = Intent(context, ResetPasswordActivity::class.java)
                        context.startActivity(intent)
                    }
                    "Add New User" -> {
                        if (isManager) {
                            settingsViewModel.showAddUserDialog()
                        }
                        else
                        {
                            Toast.makeText(context, "You do not have permission for that. Ask manager for that operation", Toast.LENGTH_LONG).show()
                        }
                    }
                    "Delete User" -> {
                        if (isManager) {
                            settingsViewModel.showDeleteUserDialog()
                        }
                    }
                }
            }
        }
    }

    if (settingsViewModel.showAddUserDialog.collectAsState().value) {
        AddUserDialog(settingsViewModel = settingsViewModel, onDismiss = { settingsViewModel.hideAddUserDialog() })
    }
    if (settingsViewModel.showDeleteUserDialog.collectAsState().value) {
        val loggedInUserId = LoginActivity.UserPrefs.getLoggedInUserId(context) ?: ""
        DeleteUserDialog(settingsViewModel = settingsViewModel, onDismiss = { settingsViewModel.hideDeleteUserDialog() }, loggedInUserId = loggedInUserId)
    }

}

@Composable
fun SettingItem(settingName: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = settingName,
                style = MaterialTheme.typography.subtitle1
            )
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Go to setting"
            )
        }
    }
}

@Composable
fun AddUserDialog(settingsViewModel: SettingsViewModel, onDismiss: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var firstname by remember { mutableStateOf("") }
    var lastname by remember { mutableStateOf("") }
    var isManager by remember { mutableStateOf(false) }
    var isAddingUser by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New User") },
        text = {
            Column {
                TextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") }
                )
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                )
                TextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") }
                )
                TextField(
                    value = firstname,
                    onValueChange = { firstname = it },
                    label = { Text("First Name") }
                )
                TextField(
                    value = lastname,
                    onValueChange = { lastname = it },
                    label = { Text("Last Name") }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isManager,
                        onCheckedChange = { isManager = it }
                    )
                    Text("Is Manager")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isAddingUser = true
                    settingsViewModel.addUser(username, password, email, firstname, lastname, isManager) { success ->
                        isAddingUser = false
                        if(success) onDismiss()
                    }
                },
                enabled = !isAddingUser
            ) {
                if(isAddingUser) {
                    CircularProgressIndicator()
                } else {
                    Text("Add User")
                }
            }
        },
        dismissButton = {
            if(!isAddingUser) {
                Button(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun DeleteUserDialog(settingsViewModel: SettingsViewModel, onDismiss: () -> Unit, loggedInUserId: String) {
    val allUsers by settingsViewModel.users.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val filteredUsers = allUsers.filterNot { it.id == loggedInUserId }.filter {
        it.username.contains(searchQuery, ignoreCase = true) ||
                it.id.contains(searchQuery, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete User") },
        text = {
            Column {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search by username or ID") },
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(filteredUsers) { user ->
                        UserCard(user, onUserClicked = {
                            settingsViewModel.deleteUser(user.id) { success ->
                                if (success) {
                                    onDismiss()
                                } else {

                                }
                            }
                        }, settingsViewModel)
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun UserCard(user: User, onUserClicked: (User) -> Unit, settingsViewModel: SettingsViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Delete User") },
            text = { Text("Are you sure you want to delete this user?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        onUserClicked(user)
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable { showDialog = true }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = user.username, style = MaterialTheme.typography.subtitle1)
                Text(text = "ID: ${user.id}", style = MaterialTheme.typography.caption)
            }
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colors.error
            )
        }
    }
}