package com.mmdparsadev.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmdparsadev.data.repository.ConversionRepository
import com.mmdparsadev.engine.plugins.ConversionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val repository: ConversionRepository) : ViewModel() {
    val currentTab = MutableStateFlow(0)
    val showSharedTextDialog = MutableStateFlow<String?>(null)
    val showFileConversionSheet = MutableStateFlow(false)
    val textInput = MutableStateFlow("")
    val textOutput = MutableStateFlow("")
    val selectedFileUri = MutableStateFlow<Uri?>(null)
    val selectedFileName = MutableStateFlow<String?>(null)
    val selectedFileType = MutableStateFlow<String?>(null)
    val selectedFileSize = MutableStateFlow<String?>(null)
    val suggestedFormats = MutableStateFlow<List<String>>(emptyList())
    val chosenOutputFormat = MutableStateFlow<String?>(null)
    val isConverting = MutableStateFlow(false)
    val conversionProgress = MutableStateFlow(0f)
    val activeConversionResult = MutableStateFlow<ConversionResult?>(null)

    val history = repository.history

    fun onTextChange(text: String) {
        textInput.value = text
        // Add text analysis logic here if needed
    }

    fun selectFile(context: Context, uri: Uri?) {
        selectedFileUri.value = uri
        // Add file analysis logic here
    }

    fun processIntent(context: Context, intent: Intent) {
        // Implementation
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun syncRatesNow(context: Context, onResult: () -> Unit) {
        viewModelScope.launch {
            com.mmdparsadev.engine.plugins.CurrencyConverterPlugin().syncRate(repository.preferences)
            onResult()
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteHistory(id)
        }
    }
}
