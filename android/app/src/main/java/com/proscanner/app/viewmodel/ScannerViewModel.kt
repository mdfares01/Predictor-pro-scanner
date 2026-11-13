package com.proscanner.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proscanner.app.data.model.ScanResult
import com.proscanner.app.data.repository.ScannerRepository
import com.proscanner.app.ui.screens.ErrorType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScannerViewModel(
    private val repository: ScannerRepository = ScannerRepository.shared
) : ViewModel() {

    val recentScans: StateFlow<List<ScanResult>> = repository.recentScans
    val isBackendConnected: StateFlow<Boolean> = repository.isBackendConnected
    val isBackendConfigured: StateFlow<Boolean> = repository.isBackendConfigured
    val backendUrl: StateFlow<String> = repository.backendUrl
    val backendStatusDetail: StateFlow<String?> = repository.backendStatusDetail

    private val _isOverlayGranted = MutableStateFlow(false)
    val isOverlayGranted: StateFlow<Boolean> = _isOverlayGranted.asStateFlow()

    private val _isCaptureGranted = MutableStateFlow(false)
    val isCaptureGranted: StateFlow<Boolean> = _isCaptureGranted.asStateFlow()

    private val _currentLanguage = MutableStateFlow("en")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private val _activeError = MutableStateFlow<ErrorType?>(null)
    val activeError: StateFlow<ErrorType?> = _activeError.asStateFlow()

    init {
        refreshBackendStatus()
    }

    fun initPersistence(context: Context) {
        repository.initPersistence(context)
        refreshBackendStatus()
    }

    fun refreshBackendStatus() {
        viewModelScope.launch {
            repository.checkBackendHealth()
        }
    }

    fun updateBackendUrl(url: String) {
        repository.setBackendUrl(url)
        viewModelScope.launch {
            repository.checkBackendHealth()
        }
    }

    fun setOverlayGranted(granted: Boolean) {
        _isOverlayGranted.value = granted
    }

    fun setCaptureGranted(granted: Boolean) {
        _isCaptureGranted.value = granted
    }

    fun toggleLanguage() {
        _currentLanguage.value = if (_currentLanguage.value == "en") "bn" else "en"
    }

    fun showError(errorType: ErrorType) {
        _activeError.value = errorType
    }

    fun clearError() {
        _activeError.value = null
    }

    private val _targetDestination = MutableStateFlow<String?>(null)
    val targetDestination: StateFlow<String?> = _targetDestination.asStateFlow()

    fun setTargetDestination(dest: String?) {
        _targetDestination.value = dest
    }

    fun clearTargetDestination() {
        _targetDestination.value = null
    }
}
