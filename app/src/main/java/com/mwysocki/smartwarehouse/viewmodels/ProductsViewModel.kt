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
import androidx.compose.foundation.layout.ColumnScope
import com.mwysocki.smartwarehouse.activities.LoginActivity
import java.io.ByteArrayOutputStream

data class Product(
    val id: String = "",
    val name: String = "",
    val quantity: Int = 0,
    val description: String = "",
    val qrCode: String = "",
    val addedBy: String = ""
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

    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProduct.asStateFlow()

    private val _showProductDetailDialog = MutableStateFlow(false)
    val showProductDetailDialog: StateFlow<Boolean> = _showProductDetailDialog.asStateFlow()

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
                        _productsState.value =
                            ProductsState(errorMessage = exception.localizedMessage)
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

    fun addProductToFirestore(productName: String, productDescription: String, productQuantity: Int, context: Context) {
        viewModelScope.launch {
            try {
                val loggedInUsername = LoginActivity.UserPrefs.getLoggedInUsername(context) ?: return@launch
                // Get a new document reference with an auto-generated ID
                val newProductRef =
                    FirebaseFirestore.getInstance().collection("Products").document()

                // Retrieve the unique product ID
                val uniqueProductId = newProductRef.id

                // Generate QR code with product's name and ID
                val qrCodeData = generateQRCode("$productName:$uniqueProductId")

                // Create the product with the unique ID, name, description, and QR code data
                val newProduct = Product(
                    id = uniqueProductId,
                    name = productName,
                    quantity = productQuantity,
                    description = productDescription,
                    qrCode = qrCodeData,
                    addedBy = loggedInUsername
                )

                // Save the product to Firestore
                newProductRef.set(newProduct)
                    .addOnSuccessListener {
                        hideAddProductDialog()
                        fetchProducts()
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

    fun deleteProductFromFirestore(product: Product) {
        viewModelScope.launch {
            try {
                FirebaseFirestore.getInstance().collection("Products")
                    .document(product.id)
                    .delete()
                    .addOnSuccessListener {
                        hideProductDetailDialog()
                        fetchProducts()
                    }
                    .addOnFailureListener { exception ->
                        _productsState.value = ProductsState(errorMessage = exception.localizedMessage)
                    }
            } catch (e: Exception) {
                _productsState.value = ProductsState(errorMessage = e.localizedMessage)
            }
        }
    }
}