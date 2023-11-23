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

class PackagesViewModel : ViewModel() {
    private val _unassignedPackages = MutableStateFlow<List<Package>>(emptyList())
    val unassignedPackages: StateFlow<List<Package>> = _unassignedPackages.asStateFlow()

    private val productsMap = mutableMapOf<String, String>()

    init {
        fetchAllProducts()
        loadUnassignedPackages()
    }

    private fun fetchAllProducts() {
        viewModelScope.launch {
            FirebaseFirestore.getInstance().collection("Products").get().addOnSuccessListener { result ->
                for (document in result) {
                    val product = document.toObject(Product::class.java)
                    productsMap[product.id] = product.name
                }
            }
        }
    }

    private fun loadUnassignedPackages() {
        viewModelScope.launch {
            FirebaseFirestore.getInstance().collection("Packages")
                .whereEqualTo("assignedTo", "")
                .get()
                .addOnSuccessListener { result ->
                    val packages = result.mapNotNull { document ->
                        val pkg = document.toObject(Package::class.java)
                        pkg?.products = pkg.products.mapKeys { entry ->
                            productsMap[entry.key] ?: "Unknown Product"
                        }
                        pkg
                    }
                    _unassignedPackages.value = packages
                }
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
}