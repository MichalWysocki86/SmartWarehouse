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
import androidx.compose.ui.platform.LocalContext
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import java.io.ByteArrayInputStream

@Composable
fun ProductsScreen(productsViewModel: ProductsViewModel = viewModel()) {
    val productsState by productsViewModel.productsState.collectAsState()
    val searchQuery by productsViewModel.searchQuery.collectAsState()
    val showDialog by productsViewModel.showAddProductDialog.collectAsState()

    val selectedProduct by productsViewModel.selectedProduct.collectAsState()
    val showProductDetailDialog by productsViewModel.showProductDetailDialog.collectAsState()

    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize()) {

        // Create a Row for the Search bar and Add button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically // This ensures all items inside the Row are vertically centered
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    productsViewModel.setSearchQuery(it)
                },
                label = { Text("Search") },
                modifier = Modifier.weight(1f)  // Assign weight to make it take up available space
            )

            Spacer(modifier = Modifier.width(8.dp))  // Spacer for some space between TextField and Button

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
                .padding(16.dp),
        )

        if (productsState.errorMessage != null) {
            Text(
                text = "Error: ${productsState.errorMessage}",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                color = Color.Red,
            )
        } else {
            LazyColumn {
                items(productsState.filteredProducts) { product ->
                    ProductItem(product, productsViewModel::selectProduct)
                }
            }
        }

        // Displaying the Add Product dialog when showDialog is true
        if (showDialog) {
            ProductAddDialog(
                onAddProduct = { productName, productDescription ->
                    productsViewModel.addProductToFirestore(productName, productDescription, context )
                },
                onDismiss = productsViewModel::hideAddProductDialog
            )
        }

        selectedProduct?.let { product ->
            if (showProductDetailDialog) {
                ProductDetailDialog(
                    product = product,
                    onDismiss = productsViewModel::hideProductDetailDialog
                )
            }
        }
    }
}
@Composable
fun ProductItem(product: Product, onProductClick: (Product) -> Unit) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .clickable { onProductClick(product) } // Add this
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = product.name)
            Text(text = product.description)
        }
    }
}
@Composable
fun ProductAddDialog(
    onAddProduct: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var productName by remember { mutableStateOf("") }
    var productDescription by remember { mutableStateOf("") }


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
            }
        },
        confirmButton = {
            Button(onClick = {
                onAddProduct(productName, productDescription)
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
    productsViewModel: ProductsViewModel = viewModel() //tutaj zmienić troche bo idk czy dobrze
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Product Details") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally, // Added this to center horizontally
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                Text(text = "ID: ${product.id}")
                Spacer(modifier = Modifier.height(16.dp))
                QRCodeImage(qrCodeBase64 = product.qrCode)

                Spacer(modifier = Modifier.height(16.dp))  // Additional spacing before the buttons

                Row(
                    horizontalArrangement = Arrangement.Center,  // Changed from End to Center
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            productsViewModel.deleteProductFromFirestore(product)
                        },
                        colors = ButtonDefaults.buttonColors(contentColorFor(backgroundColor = Color.Red))
                    ) {
                        Text("Delete")
                    }

                    Spacer(modifier = Modifier.width(16.dp))  // Increased the space for symmetry

                    Button(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        },
        confirmButton = {}
    )
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
        modifier = Modifier.size(200.dp) // Change this to adjust the QR code size on the screen
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewProductsScreen() {
    ProductsScreen()
}