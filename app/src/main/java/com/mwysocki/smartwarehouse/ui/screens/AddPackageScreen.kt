package com.mwysocki.smartwarehouse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

import com.mwysocki.smartwarehouse.viewmodels.Product
import com.mwysocki.smartwarehouse.viewmodels.ProductsViewModel

//@Composable
//fun AddPackageScreen(addPackageViewModel: AddPackageViewModel = viewModel()) {
//    val productsState by addPackageViewModel.productsState.collectAsState()
//    val searchQuery by addPackageViewModel.searchQuery.collectAsState()
//
//    Column(modifier = Modifier.fillMaxSize()) {
//        OutlinedTextField(
//            value = searchQuery,
//            onValueChange = { addPackageViewModel.setSearchQuery(it) },
//            label = { Text("Search Products") },
//            modifier = Modifier.fillMaxWidth().padding(16.dp)
//        )
//
//        LazyColumn {
//            items(productsState.filteredProducts) { product ->
//                ProductItem(product)
//            }
//        }
//    }
//}
//
//@Composable
//fun ProductItem(product: Product) {
//    Card(
//        modifier = Modifier
//            .padding(8.dp)
//            .fillMaxWidth()
//            .clickable {
//                // Handle product selection logic
//            }
//    ) {
//        Row(
//            modifier = Modifier.padding(16.dp),
//            horizontalArrangement = Arrangement.SpaceBetween
//        ) {
//            Text(text = product.name)
//            Text(text = "Qty: ${product.quantity}")
//        }
//    }
//}