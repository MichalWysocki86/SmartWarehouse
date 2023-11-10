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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import java.io.ByteArrayInputStream
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme

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
    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            BottomAppBar {
                if (createPackageMode) {
                    Button(
                        onClick = {
                            productsViewModel.createAndSavePackage(
                                createdBy = "creatorUsername",  // Replace with the actual creator's username
                                selectedProducts = selectedProductQuantities
                            )
                            createPackageMode = false
                            selectedProductQuantities.clear()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Create Package")
                    }
                    Button(
                        onClick = { /* TODO: Handle "List" action */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("List")
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
                    onAddProduct = { productName, productDescription, productQuantity ->
                        productsViewModel.addProductToFirestore(
                            productName,
                            productDescription,
                            productQuantity,
                            context
                        )
                    },
                    onDismiss = productsViewModel::hideAddProductDialog
                )
            }

            selectedProduct?.let { product ->
                if (showProductDetailDialog) {
                    ProductDetailDialog(
                        product = product,
                        onDismiss = productsViewModel::hideProductDetailDialog,
                        onDelete = { productsViewModel.deleteProductFromFirestore(it) }
                    )
                }
            }
        }
    }
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
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = product.name)
                Text(text = "Qty: ${product.quantity}")
            }
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
    onAddProduct: (String, String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var productName by remember { mutableStateOf("") }
    var productDescription by remember { mutableStateOf("") }
    var productQuantity by remember { mutableStateOf(0) }


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
                onAddProduct(productName, productDescription, productQuantity)
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
    onDelete: (Product) -> Unit
) {
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
                Text(text = ": ${product.description}")
                Spacer(modifier = Modifier.height(16.dp))
                QRCodeImage(qrCodeBase64 = product.qrCode)

                Spacer(modifier = Modifier.height(16.dp))  // Additional spacing before the buttons

                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            onDelete(product)  // Call the provided delete handler
                        },
                        colors = ButtonDefaults.buttonColors(contentColorFor(backgroundColor = Color.Red))
                    ) {
                        Text("Delete")
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
fun QuantityDialog(product: Product?, onQuantityConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    if (product != null) {
        var quantity by remember { mutableStateOf(0) }

        AlertDialog(
            onDismissRequest = { onDismiss() },
            title = { Text("Select Quantity") },
            text = {
                Column {
                    Text("Product: ${product.name}")
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = quantity.toString(),
                        onValueChange = { newValue ->
                            quantity = newValue.toIntOrNull() ?: 0
                        },
                        label = { Text("Quantity") },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(onClick = { onQuantityConfirm(quantity) }) {
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