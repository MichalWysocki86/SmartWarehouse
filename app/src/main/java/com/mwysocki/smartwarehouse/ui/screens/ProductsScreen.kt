package com.mwysocki.smartwarehouse.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mwysocki.smartwarehouse.viewmodels.Product
import com.mwysocki.smartwarehouse.viewmodels.ProductsViewModel
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import java.io.ByteArrayInputStream
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import com.mwysocki.smartwarehouse.viewmodels.ProductsState

val LightBlue = Color(0xFFADD8E6)  // Define LightBlue color
@Composable
fun ProductsScreen(productsViewModel: ProductsViewModel = viewModel()) {
    val productsState by productsViewModel.productsState.collectAsState()
    val searchQuery by productsViewModel.searchQuery.collectAsState()
    val showDialog by productsViewModel.showAddProductDialog.collectAsState()

    val selectedProduct by productsViewModel.selectedProduct.collectAsState()
    val showProductDetailDialog by productsViewModel.showProductDetailDialog.collectAsState()

    var showQuantityDialog by remember { mutableStateOf(false) }
    var currentSelectedProduct by remember { mutableStateOf<Product?>(null) }
    var currentQuantity by remember { mutableStateOf(0) }

    var createPackageMode by remember { mutableStateOf(false) }
    val selectedProductQuantities = remember { mutableMapOf<String, Int>() }
    var showPackageSummaryDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            BottomAppBar {
                if (createPackageMode) {
                    Button(
                        onClick = { showPackageSummaryDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Create Package")
                    }

                    Button(
                        onClick = { createPackageMode = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back")
                    }
                } else {
                    Button(
                        onClick = { createPackageMode = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Create Package")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { productsViewModel.setSearchQuery(it) },
                    label = { Text("Search") },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { productsViewModel.showAddProductDialog() },
                    modifier = Modifier.defaultMinSize(minWidth = 56.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Product")
                }
            }

            Text(
                text = "Total products: ${productsState.allProducts.size}",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            if (productsState.errorMessage != null) {
                Text(
                    text = "Error: ${productsState.errorMessage}",
                    color = Color.Red,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            } else {
                LazyColumn {
                    items(productsState.filteredProducts) { product ->
                        ProductItem(
                            product = product,
                            isSelected = selectedProductQuantities.containsKey(product.id),
                            showCheckbox = createPackageMode,
                            onProductClick = {
                                if (createPackageMode) {
                                    if (selectedProductQuantities.containsKey(product.id)) {
                                        selectedProductQuantities.remove(product.id)
                                    } else {
                                        currentSelectedProduct = product
                                        showQuantityDialog = true
                                    }
                                } else {
                                    productsViewModel.selectProduct(product)
                                }
                            }
                        )
                    }
                }
            }

            // ... existing QuantityDialog code ...
            if (showQuantityDialog && currentSelectedProduct != null) {
                QuantityDialog(
                    product = currentSelectedProduct,
                    onQuantityConfirm = { quantity ->
                        selectedProductQuantities[currentSelectedProduct!!.id] = quantity
                        showQuantityDialog = false
                    },
                    onDismiss = { showQuantityDialog = false }
                )
            }

            if (showDialog) {
                ProductAddDialog(
                    onAddProduct = { productName, productDescription, productQuantity, producerName ->
                        productsViewModel.addProductToFirestore(
                            productName,
                            productDescription,
                            productQuantity,
                            producerName,
                            context
                        )
                    },
                    onDismiss = productsViewModel::hideAddProductDialog
                )
            }

            if (showPackageSummaryDialog) {
                PackageSummaryDialog(
                    selectedProducts = selectedProductQuantities,
                    productsState = productsState,
                    onConfirm = {
                        productsViewModel.createAndSavePackage(context, selectedProductQuantities)
                        createPackageMode = false
                        selectedProductQuantities.clear()
                        showPackageSummaryDialog = false
                    },
                    onDismiss = {
                        showPackageSummaryDialog = false
                    }
                )
            }

            selectedProduct?.let { product ->
                if (showProductDetailDialog) {
                    ProductDetailDialog(
                        product = product,
                        onDismiss = productsViewModel::hideProductDetailDialog,
                        onDelete = { productsViewModel.deleteProductFromFirestore(it) },
                        onModify = { updatedProduct ->
                            // Code to handle the product update
                            productsViewModel.updateProductInFirestore(updatedProduct)
                            productsViewModel.hideProductDetailDialog()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PackageSummaryDialog(
    selectedProducts: Map<String, Int>,
    productsState: ProductsState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Package Summary") },
        text = {
            Column {
                Text("Selected Products:")
                selectedProducts.forEach { (productId, quantity) ->
                    val productName = productsState.allProducts.find { it.id == productId }?.name ?: "Unknown"
                    Text("$productName: $quantity")
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Confirm")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Back")
            }
        }
    )
}
@Composable
fun ProductItem(
    product: Product,
    isSelected: Boolean,
    showCheckbox: Boolean,
    onProductClick: (Product) -> Unit
) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .background(if (isSelected) LightBlue else MaterialTheme.colorScheme.surface)
            .clickable(enabled = !showCheckbox) { onProductClick(product) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = product.producer,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.weight(1f)) // This will push the quantity and checkbox to the end of the row
            Text(text = "Qty: ${product.quantity}", style = MaterialTheme.typography.bodySmall)
            if (showCheckbox) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onProductClick(product) }
                )
            }
        }
    }
}
@Composable
fun ProductAddDialog(
    onAddProduct: (String, String, Int, String) -> Unit, // Add String for producer
    onDismiss: () -> Unit
) {
    var productName by remember { mutableStateOf("") }
    var productDescription by remember { mutableStateOf("") }
    var productQuantity by remember { mutableStateOf(0) }
    var producerName by remember { mutableStateOf("") } // Add this line

    AlertDialog(
        title = { Text("Add New Product") },
        text = {
            Column {
                TextField(
                    value = productName,
                    onValueChange = { productName = it },
                    label = { Text("Product Name") }
                )
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = productDescription,
                    onValueChange = { productDescription = it },
                    label = { Text("Product Description") }
                )
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = producerName,
                    onValueChange = { producerName = it },
                    label = { Text("Producer Name") } // Add this TextField
                )
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = productQuantity.toString(),
                    onValueChange = { newValue ->
                        // Only update if the new value can be converted to an Int or is blank
                        productQuantity = newValue.toIntOrNull() ?: 0
                    },
                    label = { Text("Product Quantity") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number) // Set the keyboard type to number
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onAddProduct(productName, productDescription, productQuantity, producerName) // Pass producerName
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        onDismissRequest = onDismiss
    )
}

@Composable
fun ProductDetailDialog(
    product: Product,
    onDismiss: () -> Unit,
    onModify: (Product) -> Unit,
    onDelete: (Product) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog) {
        ProductEditDialog(
            product = product,
            onDismiss = { showEditDialog = false },
            onConfirm = { updatedProduct ->
                onModify(updatedProduct)
                showEditDialog = false
            },
            onDelete = {
                onDelete(product)
                showEditDialog = false
            }
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Product Details") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                Text(text = "ID: ${product.id}")
                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Descr: ${product.description}")
                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Producer: ${product.producer}")
                Spacer(modifier = Modifier.height(16.dp))

                QRCodeImage(qrCodeBase64 = product.qrCode)
                Spacer(modifier = Modifier.height(16.dp))  // Additional spacing before the buttons

                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(onClick = { showEditDialog = true }) {
                        Text("Modify")
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun ProductEditDialog(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: (Product) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(product.name) }
    var description by remember { mutableStateOf(product.description) }
    var quantity by remember { mutableStateOf(product.quantity.toString()) }
    var producer by remember { mutableStateOf(product.producer) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Product") },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") }
                )
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") }
                )
                TextField(
                    value = producer,
                    onValueChange = { producer = it },
                    label = { Text("Producer") }
                )
                TextField(
                    value = quantity,
                    onValueChange = { quantity= it },
                    label = { Text("Quantity") }
                )
                // Repeat for description, quantity, and producer
                Spacer(modifier = Modifier.height(16.dp))

                // Save Changes and Cancel Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Button(onClick = {
                        val updatedProduct = product.copy(
                            name = name,
                            description = description,
                            quantity = quantity.toIntOrNull() ?: product.quantity,
                            producer = producer
                        )
                        onConfirm(updatedProduct)
                    }) {
                        Text("Save Changes")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onDismiss) {
                        Text("Cancel")
                    }

                }

                Spacer(modifier = Modifier.height(16.dp))

                // Delete Button
                Button(
                    onClick = onDelete,
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(contentColorFor(backgroundColor = Color.Red))
                ) {
                    Text("Delete")
                }
            }
        },
        confirmButton = {}, // Empty as buttons are included in text
        dismissButton = {}  // Empty as buttons are included in text
    )
}


@Composable
fun QuantityDialog(product: Product?, onQuantityConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    if (product != null) {
        var quantity by remember { mutableStateOf(0) }
        val maxQuantity = product.quantity

        AlertDialog(
            onDismissRequest = { onDismiss() },
            title = { Text("Select Quantity") },
            text = {
                Column {
                    Text("Product: ${product.name}")
                    Text("Available: $maxQuantity")
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = quantity.toString(),
                        onValueChange = { newValue ->
                            val newQuantity = newValue.toIntOrNull() ?: 0
                            quantity = newQuantity.coerceIn(0, maxQuantity)
                        },
                        label = { Text("Quantity") },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        isError = quantity == 0
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { if (quantity > 0) onQuantityConfirm(quantity) },
                    enabled = quantity > 0
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                Button(onClick = { onDismiss() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalCoilApi::class)
@Composable
fun QRCodeImage(qrCodeBase64: String) {
    val byteStream = ByteArrayInputStream(Base64.decode(qrCodeBase64, Base64.DEFAULT))
    val imageBitmap = BitmapFactory.decodeStream(byteStream)
    val painter = rememberImagePainter(data = imageBitmap)

    Image(
        painter = painter,
        contentDescription = "QR Code",
        modifier = Modifier.size(200.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewProductsScreen() {
    ProductsScreen()
}