package com.mwysocki.smartwarehouse.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.mwysocki.smartwarehouse.activities.LoginActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PackagesViewModel : ViewModel() {
    private val _unassignedPackages = MutableStateFlow<List<Package>>(emptyList())
    val unassignedPackages: StateFlow<List<Package>> = _unassignedPackages.asStateFlow()

    private val productsMap = mutableMapOf<String, String>()

    // Map to store Product details along with their ordered quantity in a package
    private val productsInPackage = mutableMapOf<Product, Int>()

    // This map will now store the complete product information
    val productsInfoMap = mutableMapOf<String, Product>()

    init {
        fetchAllProducts()
        loadUnassignedPackages()
    }

    private val _assignedPackages = MutableStateFlow<List<Package>>(emptyList())
    val assignedPackages: StateFlow<List<Package>> = _assignedPackages.asStateFlow()


    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow("Unassigned")
    val filterType: StateFlow<String> = _filterType.asStateFlow()

    // Map to keep track of scanned products for each package
    private val _scannedProducts = mutableMapOf<String, List<String>>()
    val scannedProducts: Map<String, List<String>> = _scannedProducts

    fun markProductAsScanned(packageId: String, productId: String) {
        val currentScanned = _scannedProducts[packageId].orEmpty()
        if (productId !in currentScanned) {
            _scannedProducts[packageId] = currentScanned + productId
        }
    }



    fun deletePackage(packageId: String) {
        viewModelScope.launch {
            val packageRef = FirebaseFirestore.getInstance().collection("Packages").document(packageId)
            packageRef.delete()
                .addOnSuccessListener {
                    Log.d("PackagesViewModel", "Package successfully deleted: $packageId")
                    // Refresh the packages list after deletion
                    if (_filterType.value == "Unassigned") {
                        loadUnassignedPackages()
                    } else {
                        loadAllAssignedPackages()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("PackagesViewModel", "Error deleting package: $packageId", e)
                }
        }
    }
    fun setFilterType(type: String) {
        _filterType.value = type
        if (type == "Unassigned") {
            loadUnassignedPackages()
        } else {
            loadAllAssignedPackages()
        }
    }

    private fun loadAllAssignedPackages() {
        viewModelScope.launch {
            FirebaseFirestore.getInstance().collection("Packages")
                .whereNotEqualTo("assignedTo", "")
                .whereEqualTo("done", false)
                .get()
                .addOnSuccessListener { result ->
                    val packages = result.mapNotNull { document ->
                        document.toObject(Package::class.java)
                    }.also { fullList ->
                        Log.d("PackagesViewModel", "All assigned packages fetched: $fullList")
                    }.filter {
                        it.id.contains(_searchQuery.value, ignoreCase = true) ||
                                it.assignedTo.contains(_searchQuery.value, ignoreCase = true)
                    }
                    Log.d("PackagesViewModel", "Filtered assigned packages: $packages")
                    _assignedPackages.value = packages
                }
                .addOnFailureListener { e ->
                    Log.e("PackagesViewModel", "Error fetching assigned packages", e)
                }
        }
    }

    fun loadAssignedPackages(username: String) {
        viewModelScope.launch {
            FirebaseFirestore.getInstance().collection("Packages")
                .whereEqualTo("assignedTo", username)
                .whereEqualTo("done", false)
                .get()
                .addOnSuccessListener { result ->
                    val packages = result.mapNotNull { document ->
                        val pkg = document.toObject(Package::class.java)
                        pkg?.let {
                            updateProductsInPackage(it)
                        }
                        Log.d("PackagesViewModel", "Loaded package: $pkg")
                        pkg
                    }
                    _assignedPackages.value = packages
                    Log.d("PackagesViewModel", "Assigned packages updated: $packages")
                }
        }
    }

    // Function to update the productsInPackage map
    private fun updateProductsInPackage(pkg: Package) {
        pkg.products.forEach { (productId, orderedQuantity) ->
            FirebaseFirestore.getInstance().collection("Products").document(productId)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    val product = documentSnapshot.toObject(Product::class.java)
                    product?.let {
                        productsInPackage[it] = orderedQuantity
                    }
                }
                .addOnFailureListener {
                    Log.e("PackagesViewModel", "Error fetching product details", it)
                }
        }
    }


    private fun fetchAllProducts() {
        viewModelScope.launch {
            FirebaseFirestore.getInstance().collection("Products").get()
                .addOnSuccessListener { result ->
                    for (document in result) {
                        val product = document.toObject(Product::class.java)
                        productsInfoMap[product.id] = product
                    }
                }
        }
    }

    // Update this function to handle search filtering
    private fun loadUnassignedPackages() {
        viewModelScope.launch {
            FirebaseFirestore.getInstance().collection("Packages")
                .whereEqualTo("assignedTo", "")
                .whereEqualTo("done", false)
                .get()
                .addOnSuccessListener { result ->
                    val packages = result.mapNotNull { document ->
                        document.toObject(Package::class.java)
                    }.filter {
                        it.id.contains(_searchQuery.value, ignoreCase = true) ||
                                it.assignedTo.contains(_searchQuery.value, ignoreCase = true)
                    }
                    _unassignedPackages.value = packages
                }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        // Check the current filter type and load the appropriate set of packages
        if (_filterType.value == "Unassigned") {
            loadUnassignedPackages()
        } else {
            loadAllAssignedPackages()
        }
    }

    fun assignPackageToCurrentUser(context: Context, packageId: String) {
        val username = LoginActivity.UserPrefs.getLoggedInUsername(context) ?: "Unknown User"

        // Get the reference to the package document
        val packageRef = FirebaseFirestore.getInstance().collection("Packages").document(packageId)

        // Set the 'assignedTo' field of this package
        packageRef.update("assignedTo", username)
            .addOnSuccessListener {
                Log.d("PackagesViewModel", "Package $packageId assigned to $username")
                // Trigger the loadUnassignedPackages() to refresh the list
                loadUnassignedPackages()
            }
            .addOnFailureListener { e ->
                Log.e("PackagesViewModel", "Error assigning package", e)
            }
    }

    fun sendPackageToArchive(packageId: String, username: String) {
        viewModelScope.launch {
            val firestore = FirebaseFirestore.getInstance()
            val packageRef = firestore.collection("Packages").document(packageId)

            // Get the current package data
            packageRef.get().addOnSuccessListener { documentSnapshot ->
                val packageData = documentSnapshot.toObject(Package::class.java)
                packageData?.let { pkg ->
                    // Iterate over each product in the package
                    pkg.products.forEach { (productId, quantityOrdered) ->
                        // Reference to the product document
                        val productRef = firestore.collection("Products").document(productId)
                        // Update the product quantity
                        productRef.get().addOnSuccessListener { productSnapshot ->
                            val product = productSnapshot.toObject(Product::class.java)
                            product?.let {
                                val newQuantity = product.quantity - quantityOrdered
                                productRef.update("quantity", newQuantity)
                            }
                        }
                    }
                    // Set the 'done' field to true and move the package to the archive
                    val updatedPackageData = pkg.copy(isDone = true)
                    firestore.collection("PackagesArchive")
                        .document(packageId)
                        .set(updatedPackageData)
                        .addOnSuccessListener {
                            // Delete the original document from the Packages collection
                            packageRef.delete()
                            Log.d("PackagesViewModel", "Package $packageId sent to archive and original deleted.")
                            // Optionally, refresh the list of packages if needed
                            // loadUnassignedPackages()
                            loadAssignedPackages(username)
                        }.addOnFailureListener { e ->
                            Log.e("PackagesViewModel", "Error sending package to archive", e)
                        }
                }
            }.addOnFailureListener { e ->
                Log.e("PackagesViewModel", "Error retrieving package data", e)
            }
        }
    }

}