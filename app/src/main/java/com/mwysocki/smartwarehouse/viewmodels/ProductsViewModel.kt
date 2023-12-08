package com.mwysocki.smartwarehouse.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Base64
import android.util.Log
import com.mwysocki.smartwarehouse.activities.LoginActivity
import java.io.ByteArrayOutputStream
import java.util.*

data class Product(
    val id: String = "",
    val name: String = "",
    var quantity: Int = 0,
    val description: String = "",
    val producer: String = "", // Add this line
    val qrCode: String = "",
    val addedBy: String = ""
)

data class ProductsState(
    val allProducts: List<Product> = listOf(),
    val filteredProducts: List<Product> = listOf(),
    val errorMessage: String? = null
)

data class Package(
    val id: String = "",
    val creationDate: Date? = null,
    val createdBy: String = "",
    val assignedTo: String = "",
    val isDone: Boolean = false,
    var products: Map<String, Int> = emptyMap(), // Make sure to provide a default empty map
    // Ensure that the productIds list is derived from the products map to avoid deserialization issues
    val productIds: List<String> = products.keys.toList()
)


class ProductsViewModel : ViewModel() {
    private val _productsState = MutableStateFlow(ProductsState())
    val productsState: StateFlow<ProductsState> = _productsState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showAddProductDialog = MutableStateFlow(false)
    val showAddProductDialog: StateFlow<Boolean> = _showAddProductDialog.asStateFlow()

    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProduct.asStateFlow()

    private val _showProductDetailDialog = MutableStateFlow(false)
    val showProductDetailDialog: StateFlow<Boolean> = _showProductDetailDialog.asStateFlow()

    private val _filterType = MutableStateFlow("Name")
    val filterType: StateFlow<String> = _filterType.asStateFlow()

    // Function to create and save a package to Firestore
    fun createAndSavePackage(context: Context, selectedProducts: Map<String, Int>) {
        viewModelScope.launch {
            try {
                // Extract product IDs for querying
                val productIds = selectedProducts.keys.toList()
                val createdBy = LoginActivity.UserPrefs.getLoggedInUsername(context) ?: "Unknown User"

                // Create a new package with product IDs and the creator's username
                val newPackage = Package(
                    id = UUID.randomUUID().toString(),
                    creationDate = Date(),
                    createdBy = createdBy,
                    assignedTo = "",
                    isDone = false,
                    products = selectedProducts,
                    productIds = productIds
                )

                // Save the package to Firestore
                FirebaseFirestore.getInstance().collection("Packages").document(newPackage.id)
                    .set(newPackage)
                    .addOnSuccessListener {
                        // Handle success
                        Log.d("Firestore", "Package created successfully by $createdBy.")
                    }
                    .addOnFailureListener { e ->
                        // Handle failure
                        Log.e("Firestore", "Error creating package", e)
                    }
            } catch (e: Exception) {
                // Handle any exceptions
                Log.e("Firestore", "Exception in creating package", e)
            }
        }
    }



    fun selectProduct(product: Product) {
        _selectedProduct.value = product
        _showProductDetailDialog.value = true
    }

    fun hideProductDetailDialog() {
        _showProductDetailDialog.value = false
    }

    init {
        fetchProducts()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        filterProducts()
    }

    fun fetchProducts() {
        viewModelScope.launch {
            try {
                val productList = mutableListOf<Product>()
                FirebaseFirestore.getInstance().collection("Products")
                    .orderBy("name")
                    .get()
                    .addOnSuccessListener { result ->
                        for (document in result) {
                            val product = document.toObject(Product::class.java)
                            productList.add(product)
                        }
                        _productsState.value = ProductsState(allProducts = productList)
                        filterProducts()
                    }
                    .addOnFailureListener { exception ->
                        _productsState.value =
                            ProductsState(errorMessage = exception.localizedMessage)
                    }
            } catch (e: Exception) {
                _productsState.value = ProductsState(errorMessage = e.localizedMessage)
            }
        }
    }

    fun setFilterType(type: String) {
        _filterType.value = type
        filterProducts()
    }

    private fun filterProducts() {
        val filteredList = _productsState.value.allProducts.filter {
            when (_filterType.value) {
                "Name" -> it.name.contains(_searchQuery.value, ignoreCase = true)
                "Producer" -> it.producer.contains(_searchQuery.value, ignoreCase = true)
                "ID" -> it.id.contains(_searchQuery.value, ignoreCase = true)
                else -> false
            }
        }
        _productsState.value = _productsState.value.copy(filteredProducts = filteredList)
    }

    fun showAddProductDialog() {
        _showAddProductDialog.value = true
    }

    fun hideAddProductDialog() {
        _showAddProductDialog.value = false
    }

    private fun generateQRCode(content: String): String {
        val qrCodeWriter = QRCodeWriter()
        val bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 512, 512)
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
        for (x in 0 until 512) {
            for (y in 0 until 512) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    fun addProductToFirestore(productName: String, productDescription: String, productQuantity: Int, producerName: String, context: Context) {
        viewModelScope.launch {
            try {
                val loggedInUsername = LoginActivity.UserPrefs.getLoggedInUsername(context) ?: return@launch
                val newProductRef = FirebaseFirestore.getInstance().collection("Products").document()
                val uniqueProductId = newProductRef.id
                val qrCodeData = generateQRCode(uniqueProductId)

                val newProduct = Product(
                    id = uniqueProductId,
                    name = productName,
                    quantity = productQuantity,
                    description = productDescription,
                    producer = producerName,
                    qrCode = qrCodeData,
                    addedBy = loggedInUsername
                )

                Log.d("ProductsViewModel", "Adding new product: $productName")

                newProductRef.set(newProduct)
                    .addOnSuccessListener {
                        Log.d("ProductsViewModel", "Product added successfully: $productName")
                        hideAddProductDialog() // Close the dialog after successful addition
                        fetchProducts() // Refresh the products list
                    }
                    .addOnFailureListener { exception ->
                        Log.e("ProductsViewModel", "Error adding product: ${exception.localizedMessage}")
                        _productsState.value = ProductsState(errorMessage = exception.localizedMessage)
                    }
            } catch (e: Exception) {
                Log.e("ProductsViewModel", "Exception in adding product: ${e.localizedMessage}")
                _productsState.value = ProductsState(errorMessage = e.localizedMessage)
            }
        }
    }

    fun deleteProductFromFirestore(product: Product) {
        viewModelScope.launch {
            try {
                FirebaseFirestore.getInstance().collection("Products")
                    .document(product.id)
                    .delete()
                    .addOnSuccessListener {
                        fetchProducts() // Refresh the products list after deletion
                        hideProductDetailDialog()
                    }
                    .addOnFailureListener { exception ->
                        _productsState.value = ProductsState(errorMessage = exception.localizedMessage)
                    }
            } catch (e: Exception) {
                _productsState.value = ProductsState(errorMessage = e.localizedMessage)
            }
        }
    }

    fun updateProductInFirestore(updatedProduct: Product) {
        viewModelScope.launch {
            val productRef = FirebaseFirestore.getInstance().collection("Products").document(updatedProduct.id)
            productRef.set(updatedProduct)
                .addOnSuccessListener {
                    fetchProducts() // Refresh the product list
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error updating product", e)
                }
        }
    }
}

