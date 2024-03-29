package com.mwysocki.smartwarehouse.ui.screens

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    val context = LocalContext.current
    val currentPackages = when (packagesViewModel.filterType.collectAsState().value) {
        "Unassigned" -> packagesViewModel.unassignedPackages.collectAsState().value
        else -> packagesViewModel.assignedPackages.collectAsState().value
    }
    val searchQuery by packagesViewModel.searchQuery.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var selectedPackageId by remember { mutableStateOf("") }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { packagesViewModel.setSearchQuery(it) },
                label = { Text("Search Packages") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            FilterDropdownMenu(packagesViewModel = packagesViewModel)
        }
        LazyColumn {
            items(items = currentPackages, key = { it.id }) { pkg ->
                PackageItem(pkg = pkg) {
                    selectedPackageId = pkg.id
                    showDialog = true
                }
            }
        }
    }
    if (showDialog) {
        PackageActionDialog(
            packageId = selectedPackageId,
            onAssignToMe = {
                packagesViewModel.assignPackageToCurrentUser(context, selectedPackageId)
                showDialog = false
            },
            onDelete = {
                packagesViewModel.deletePackage(selectedPackageId)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
fun PackageActionDialog(
    packageId: String,
    onAssignToMe: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isManager = LoginActivity.UserPrefs.getLoggedInUserIsManager(context)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Package Actions") },
        text = { Text("Select an action for package ID: $packageId") },
        confirmButton = {
            Column(
                modifier = Modifier.padding(all = 8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onAssignToMe,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp)
                    ) {
                        Text("Assign to Me")
                    }
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Go Back")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (isManager) {
                            onDelete()
                        } else {
                            Toast.makeText(context, "Only managers can delete packages.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete Package", color = Color.White)
                }
            }
        }
    )
}
@Composable
fun FilterDropdownMenu(packagesViewModel: PackagesViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val filterOptions = listOf("Unassigned", "Assigned")

    Box(modifier = Modifier.padding(end = 8.dp)) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                )
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Filter Options",
                tint = Color.White // White arrow icon
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            filterOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        packagesViewModel.setFilterType(option)
                        expanded = false
                    }
                )
            }
        }
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
            Text(text = "Assigned to: ${pkg.assignedTo}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun AssignedPackagesScreen(assignedPackages: List<Package>, packagesViewModel: PackagesViewModel) {
    var selectedPackage by remember { mutableStateOf<Package?>(null) }

    LazyColumn {
        items(items = assignedPackages, key = { it.id }) { pkg ->
            PackageItem(pkg = pkg) { selectedPkg ->
                selectedPackage = selectedPkg
            }
        }
    }
    selectedPackage?.let { pkg ->
        PackageDetailsDialog(
            pkg = pkg,
            packagesViewModel = packagesViewModel,
            onDismiss = { selectedPackage = null }
        )
    }
}

@Composable
fun PackageDetailsDialog(pkg: Package, packagesViewModel: PackagesViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val initialScannedProducts = packagesViewModel.scannedProducts[pkg.id].orEmpty()
    val selectedProducts = remember { mutableStateListOf<String>().apply { addAll(initialScannedProducts) } }
    var currentScanningProduct by remember { mutableStateOf<Product?>(null) }
    val allProductsSelected = selectedProducts.size == pkg.products.size
    val productsWithQuantity = pkg.products.mapNotNull { (productId, quantity) ->
        packagesViewModel.productsInfoMap[productId]?.let { product ->
            product to quantity
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scannedQRCode = result.data?.getStringExtra("SCANNED_QR")
            scannedQRCode?.let { code ->
                if (currentScanningProduct?.id == scannedQRCode) {
                    selectedProducts.add(scannedQRCode)
                    packagesViewModel.markProductAsScanned(pkg.id, scannedQRCode)
                    Toast.makeText(context, "Product scanned successfully.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "This is not the correct product to scan next.", Toast.LENGTH_LONG).show()
                }
                currentScanningProduct = null // Reset the current scanning product
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
                                currentScanningProduct = product
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
                        val loggedInUsername = LoginActivity.UserPrefs.getLoggedInUsername(context) ?: return@Button
                        packagesViewModel.sendPackageToArchive(pkg.id, loggedInUsername)
                        onDismiss()
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
                    Spacer(modifier = Modifier.width(8.dp))
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