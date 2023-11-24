package com.mwysocki.smartwarehouse.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mwysocki.smartwarehouse.viewmodels.Package
import com.mwysocki.smartwarehouse.viewmodels.PackagesViewModel


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
    var selectedPackage by remember { mutableStateOf<Package?>(null) }

    LazyColumn {
        items(items = assignedPackages, key = { it.id }) { pkg ->
            PackageItem(pkg = pkg) { selectedPkg ->
                selectedPackage = selectedPkg
            }
        }
    }

    selectedPackage?.let { pkg ->
        PackageDetailsDialog(pkg = pkg, onDismiss = { selectedPackage = null })
    }
}
@Composable
fun PackageDetailsDialog(pkg: Package, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Package Details") },
        text = {
            LazyColumn {
                items(pkg.products.toList(), key = { it.first }) { (productName, quantity) ->
                    ProductCard(productName = productName, quantity = quantity)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
@Composable
fun ProductCard(productName: String, quantity: Int) {
    Card(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = productName, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.weight(1f))
            Text(text = "Quantity: $quantity", style = MaterialTheme.typography.bodySmall)
        }
    }
}