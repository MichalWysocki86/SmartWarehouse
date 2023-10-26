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
    val filteredProducts: List<Product> = listOf(),
    val errorMessage: String? = null
)

class ProductsViewModel : ViewModel() {
    private val _productsState = MutableStateFlow(ProductsState())
    val productsState: StateFlow<ProductsState> = _productsState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showAddProductDialog = MutableStateFlow(false)
    val showAddProductDialog: StateFlow<Boolean> = _showAddProductDialog.asStateFlow()
    init {
        fetchProducts()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        filterProducts()
    }

    private fun fetchProducts() {
        viewModelScope.launch {
            try {
                val productList = mutableListOf<Product>()
                FirebaseFirestore.getInstance().collection("Products").get()
                    .addOnSuccessListener { result ->
                        for (document in result) {
                            val product = document.toObject(Product::class.java)
                            productList.add(product)
                        }
                        _productsState.value = ProductsState(allProducts = productList)
                        filterProducts()
                    }
                    .addOnFailureListener { exception ->
                        _productsState.value = ProductsState(errorMessage = exception.localizedMessage)
                    }
            } catch (e: Exception) {
                _productsState.value = ProductsState(errorMessage = e.localizedMessage)
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
    fun showAddProductDialog() {
        _showAddProductDialog.value = true
    }

    fun hideAddProductDialog() {
        _showAddProductDialog.value = false
    }

    fun addProductToFirestore(productName: String, productDescription: String) {
        val newProduct = mapOf(
            "name" to productName,
            "description" to productDescription
            // "qrCode" can be added later as needed
        )

        FirebaseFirestore.getInstance().collection("Products").add(newProduct)
            .addOnSuccessListener {
                fetchProducts() // Refresh the list after adding a new product
                hideAddProductDialog()
            }
            .addOnFailureListener { exception ->
                _productsState.value = ProductsState(errorMessage = exception.localizedMessage)
            }
    }
}