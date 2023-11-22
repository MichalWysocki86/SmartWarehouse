package com.mwysocki.smartwarehouse.activities

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
import androidx.compose.foundation.layout.ColumnScope
import com.mwysocki.smartwarehouse.activities.LoginActivity
import com.mwysocki.smartwarehouse.viewmodels.ProductsViewModel
import java.io.ByteArrayOutputStream
import java.lang.Package
import java.util.*
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.Date

