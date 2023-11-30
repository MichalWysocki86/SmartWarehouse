package com.mwysocki.smartwarehouse.ui.screens

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mwysocki.smartwarehouse.activities.LoginActivity
import com.mwysocki.smartwarehouse.activities.QRScanActivity
import com.mwysocki.smartwarehouse.viewmodels.Package
import com.mwysocki.smartwarehouse.viewmodels.PackagesViewModel
import com.mwysocki.smartwarehouse.viewmodels.Product


@Composable
fun PackagesScreen(packagesViewModel: PackagesViewModel = viewModel()) {
    val unassignedPackages by packagesViewModel.unassignedPackages.collectAsState()
    val context = LocalContext.current // Retrieve the context
    var showDialog by remember { mutableStateOf(false) }
    var selectedPackageId by remember { mutableStateOf("") }

    LazyColumn {
        items(items = unassignedPackages, key = { it.id }) { pkg ->
            PackageItem(pkg = pkg) {
                // Set the selected package ID and show dialog
                selectedPackageId = pkg.id
                showDialog = true
            }
        }
    }

    // Show the dialog when a package is selected
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Assign Package") },
            text = { Text("Do you want to assign this package to yourself?") },
            confirmButton = {
                Button(onClick = {
                    packagesViewModel.assignPackageToCurrentUser(context, selectedPackageId)
                    showDialog = false
                }) {
                    Text("Assign to Me")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Go Back")
                }
            }
        )
    }
}

@Composable
fun PackageItem(pkg: Package, onPackageSelected: (Package) -> Unit) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable { onPackageSelected(pkg) },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = "Package ID: ${pkg.id}", style = MaterialTheme.typography.titleMedium)
            Text(text = "Created by: ${pkg.createdBy}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Creation date: ${pkg.creationDate}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun AssignedPackagesScreen(assignedPackages: List<Package>) {

    // Obtain PackagesViewModel instance
    val packagesViewModel: PackagesViewModel = viewModel()
    // Get the list of assigned packages from the ViewModel
    val assignedPackages by packagesViewModel.assignedPackages.collectAsState()
    var selectedPackage by remember { mutableStateOf<Package?>(null) }

    LazyColumn {
        items(items = assignedPackages, key = { it.id }) { pkg ->
            PackageItem(pkg = pkg) { selectedPkg ->
                selectedPackage = selectedPkg
            }
        }
    }

    selectedPackage?.let { pkg ->
        // Pass packagesViewModel to the dialog
        PackageDetailsDialog(pkg = pkg, packagesViewModel = packagesViewModel, onDismiss = { selectedPackage = null })
    }
}

@Composable
fun PackageDetailsDialog(pkg: Package, packagesViewModel: PackagesViewModel, onDismiss: () -> Unit) {
    // State to keep track of selected products
    val context = LocalContext.current // Retrieve the context
    val selectedProducts = remember { mutableStateListOf<String>() }
    val allProductsSelected = selectedProducts.size == pkg.products.size

    // Prepare a list of Pair<Product, Int> for each product in the package
    val productsWithQuantity = pkg.products.mapNotNull { (productId, quantity) ->
        packagesViewModel.productsInfoMap[productId]?.let { product ->
            product to quantity
        }
    }

    // Prepare launcher for QR scan activity
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scannedQRCode = result.data?.getStringExtra("SCANNED_QR")
            scannedQRCode?.let {
                // Compare the scanned QR code with product QR code and update selected products
                productsWithQuantity.forEach { (product, _) ->
                    if (product.id == scannedQRCode) {
                        Log.d("QRScanActivity", "Stuttu: $scannedQRCode")
                        selectedProducts.add(product.id)
                    }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Package Details") },
        text = {
            LazyColumn {
                items(productsWithQuantity, key = { it.first.id }) { (product, quantity) ->
                    ProductCard(
                        product = product,
                        quantity = quantity,
                        isSelected = selectedProducts.contains(product.id),
                        onProductClick = {
                            if (!selectedProducts.contains(product.id)) {
                                launcher.launch(Intent(context, QRScanActivity::class.java))
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            Row {
                if (allProductsSelected) {
                    Button(onClick = {
                        // Get the logged-in username, assuming 'getLoggedInUsername' returns a non-null username
                        val loggedInUsername = LoginActivity.UserPrefs.getLoggedInUsername(context) ?: return@Button
                        // Call 'sendPackageToArchive' with the packageId and logged-in username
                        packagesViewModel.sendPackageToArchive(pkg.id, loggedInUsername)
                        onDismiss() // Close the dialog
                    }) {
                        Text("Send Package")
                    }
                }

            }
        }
    )
}

@Composable
fun ProductCard(
    product: Product,
    quantity: Int,
    isSelected: Boolean,
    onProductClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth()
            .clickable(onClick = onProductClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color.Green else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = product.producer,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Qty: $quantity",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.width(8.dp))  // Space between quantity and checkmark
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White
                    )
                }
            }
        }
    }
}