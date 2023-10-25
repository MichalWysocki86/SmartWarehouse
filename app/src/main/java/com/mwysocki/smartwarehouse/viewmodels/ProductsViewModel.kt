package com.mwysocki.smartwarehouse.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class Product(
    val name: String = "",
    val description: String = "",
    val qrCode: String = ""
)

data class ProductsState(
    val allProducts: List<Product> = listOf(),
    val filteredProducts: List<Product> = listOf()
)

class ProductsViewModel : ViewModel() {
    private val _productsState = MutableStateFlow(ProductsState())
    val productsState: StateFlow<ProductsState> = _productsState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        fetchProducts()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        filterProducts()
    }

    private fun fetchProducts() {
        viewModelScope.launch {
            val productList = mutableListOf<Product>()
            FirebaseFirestore.getInstance().collection("products").get().addOnSuccessListener { result ->
                for (document in result) {
                    val product = document.toObject(Product::class.java)
                    productList.add(product)
                }
                _productsState.value = ProductsState(allProducts = productList)
                filterProducts()
            }
        }
    }

    private fun filterProducts() {
        val filteredList = _productsState.value.allProducts.filter {
            it.name.contains(_searchQuery.value, ignoreCase = true) ||
                    it.description.contains(_searchQuery.value, ignoreCase = true)
        }
        _productsState.value = _productsState.value.copy(filteredProducts = filteredList)
    }
}