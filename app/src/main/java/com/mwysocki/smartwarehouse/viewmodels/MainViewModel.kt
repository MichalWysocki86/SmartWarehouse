package com.mwysocki.smartwarehouse.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel: ViewModel() {
    private val _uiState = MutableStateFlow(MainAppState())
    val uiState: StateFlow<MainAppState> = _uiState.asStateFlow()

    fun showDialog() {
        _uiState.value = _uiState.value.copy(showLogoutDialog = true)
    }

    fun dismissDialog() {
        _uiState.value = _uiState.value.copy(showLogoutDialog = false)
    }

}
data class MainAppState(
    val showLogoutDialog: Boolean = false
)