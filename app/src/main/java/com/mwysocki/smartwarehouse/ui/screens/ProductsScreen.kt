package com.mwysocki.smartwarehouse.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
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

@Composable
fun ProductsScreen(productsViewModel: ProductsViewModel = viewModel()) {
    val productsState by productsViewModel.productsState.collectAsState()
    val searchQuery by productsViewModel.searchQuery.collectAsState()
    val showDialog by productsViewModel.showAddProductDialog.collectAsState()

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
                    ProductItem(product)
                }
            }
        }

        // Displaying the Add Product dialog when showDialog is true
        if (showDialog) {
            ProductAddDialog(
                onAddProduct = productsViewModel::addProductToFirestore,
                onDismiss = productsViewModel::hideAddProductDialog
            )
        }
    }
}
@Composable
fun ProductItem(product: Product) {
    Card(modifier = Modifier.padding(8.dp)) {
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
@Preview(showBackground = true)
@Composable
fun PreviewProductsScreen() {
    ProductsScreen()
}