package com.mwysocki.smartwarehouse.viewmodels

import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
//class AddPackageViewModel : ViewModel() {
//    private val _productsState = MutableStateFlow(ProductsState())
//    val productsState: StateFlow<ProductsState> = _productsState.asStateFlow()
//
//    private val _searchQuery = MutableStateFlow("")
//    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
//
//    init {
//        fetchProducts()
//    }
//
//    fun setSearchQuery(query: String) {
//        _searchQuery.value = query
//        // Implement the logic to filter products based on the search query
//    }
//
//    private fun fetchProducts() {
//        // Implement the logic to fetch products
//    }
//}
//
