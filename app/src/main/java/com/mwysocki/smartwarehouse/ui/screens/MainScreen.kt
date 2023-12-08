package com.mwysocki.smartwarehouse.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mwysocki.smartwarehouse.R
import com.mwysocki.smartwarehouse.activities.LoginActivity
import com.mwysocki.smartwarehouse.activities.NavigationItem
import com.mwysocki.smartwarehouse.ui.theme.SmartWarehouseTheme
import com.mwysocki.smartwarehouse.viewmodels.MainViewModel
import com.mwysocki.smartwarehouse.viewmodels.PackagesViewModel
import com.mwysocki.smartwarehouse.viewmodels.ProfileViewModel
import com.mwysocki.smartwarehouse.viewmodels.SharedViewModel
import kotlinx.coroutines.launch

enum class MainScreen(@StringRes val title: Int) {
    Home(R.string.home),
    Profile(R.string.profile),
    Products(R.string.products),
    Packages(R.string.packages),
    Logout(R.string.logout)
}

@Composable
fun MainApp(
    navController: NavHostController = rememberNavController(),
    mainViewModel: MainViewModel = viewModel(),
    logoutUser: () -> Unit
) {
    val state by mainViewModel.uiState.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    // Get the name of the current screen
    val currentScreen = MainScreen.valueOf(
        backStackEntry?.destination?.route ?: MainScreen.Home.name
    )
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val packagesViewModel: PackagesViewModel = viewModel()
    val sharedViewModel: SharedViewModel = viewModel()

    val context = LocalContext.current
    NavigationDrawer(
        drawerState = drawerState,
        navController = navController,
        onLogoutClicked = {
            mainViewModel.showDialog()
        }
    ) {
        Scaffold(
            topBar = {
                MainAppBar(
                    currentScreen,
                    onMenuClick = {
                        scope.launch { drawerState.open() }
                    },
                    navController = navController // Pass the navController here
                )
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = MainScreen.Home.name,
                modifier = Modifier.padding(paddingValues)
            ) {
                composable(route = MainScreen.Home.name) {
                    val username = LoginActivity.UserPrefs.getLoggedInUsername(context) ?: return@composable
                    // This will load the packages assigned to the logged-in user
                    Log.d("TTTT","$username")
                    packagesViewModel.loadAssignedPackages(username)

                    //TODO change this code to look like under

                    // Now you can use the assignedPackages StateFlow to collect and display the packages
                    val assignedPackages by packagesViewModel.assignedPackages.collectAsState()
                    AssignedPackagesScreen(assignedPackages)
                }
                composable(route = MainScreen.Profile.name) {
                    ProfileScreen(sharedViewModel)
                }
                composable(route = MainScreen.Products.name) {
                    ProductsScreen()
                }
                composable(route = MainScreen.Packages.name) {
                    PackagesScreen()
                }


            }
            if (state.showLogoutDialog) {
                ConfirmDialog(
                    title = "Logout",
                    text = "Are you sure you want to logout?",
                    onConfirm = {
                        mainViewModel.dismissDialog()
                        logoutUser()
                    },
                    onDismiss = {
                        mainViewModel.dismissDialog()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppBar(
    currentScreen: MainScreen,
    onMenuClick: () -> Unit,
    navController: NavHostController
) {
    TopAppBar(
        title = {
            Text(text = stringResource(id = currentScreen.title))
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu"
                )
            }
        },
        actions = {
            IconButton(onClick = { /* do something */ }) {
                Icon(
                    imageVector = Icons.Filled.QrCodeScanner,
                    contentDescription = "Add Package by Qr Code"
                )
            }
        }
    )
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun NavigationDrawer(
    drawerState: DrawerState,
    navController: NavHostController,
    onLogoutClicked: () -> Unit,
    content: @Composable () -> Unit
) {
    SmartWarehouseTheme {
        val items = listOf(
            NavigationItem(
                title = stringResource(id = R.string.home),
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home,
                route = MainScreen.Home
            ),
            NavigationItem(
                title = stringResource(id = R.string.profile),
                selectedIcon = Icons.Filled.Person,
                unselectedIcon = Icons.Outlined.Person,
                route = MainScreen.Profile
            ),
            NavigationItem(
                title = stringResource(id = R.string.products),
                selectedIcon = Icons.Filled.Warehouse,
                unselectedIcon = Icons.Outlined.Warehouse,
                route = MainScreen.Products
            ),
            NavigationItem(
                title = stringResource(id = R.string.packages),
                selectedIcon = Icons.Filled.LocalShipping,
                unselectedIcon = Icons.Outlined.LocalShipping,
                route = MainScreen.Packages
            ),
            NavigationItem(
                title = "Logout",
                selectedIcon = Icons.Filled.ExitToApp,
                unselectedIcon = Icons.Outlined.ExitToApp,
                route = MainScreen.Logout
            ),
        )

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {

            val scope = rememberCoroutineScope()
            var selectedItemIndex by rememberSaveable {
                mutableIntStateOf(0)
            }
            ModalNavigationDrawer(
                drawerContent = {
                    ModalDrawerSheet {
                        Spacer(modifier = Modifier.height(16.dp))
                        items.forEachIndexed { index, item ->
                            NavigationDrawerItem(
                                label = {
                                    Text(text = item.title)
                                },
                                selected = index == selectedItemIndex,
                                onClick = {
                                    selectedItemIndex = index
                                    scope.launch {
                                        drawerState.close()
                                    }
                                    if (item.title == "Logout") {
                                        onLogoutClicked()
                                    } else {
                                        navController.navigate(item.route.name)
                                        selectedItemIndex = index
                                        scope.launch {
                                            drawerState.close()
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (index == selectedItemIndex) {
                                            item.selectedIcon
                                        } else item.unselectedIcon,
                                        contentDescription = item.title
                                    )
                                },
                                badge = {
                                    item.badgeCount?.let {
                                        Text(text = item.badgeCount.toString())
                                    }
                                },
                                modifier = Modifier
                                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }
                    }
                },
                drawerState = drawerState
            ) {
                content()
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = text) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Yes")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("No")
            }
        }
    )
}
