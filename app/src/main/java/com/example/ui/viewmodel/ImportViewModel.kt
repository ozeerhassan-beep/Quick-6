package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import com.example.data.CatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

data class ImportUiState(
    val collectionName: String = "products",
    val totalDocuments: Int = 0,
    val totalFieldsStored: Int = 0,
    val uploadProgress: Float = 0f,
    val isSyncing: Boolean = false,
    val isUploading: Boolean = false,
    val errorMessage: String? = null
)

class ImportViewModel(private val repository: CatalogRepository) : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    private val _navigationEvent = Channel<String>()
    val navigationEvent = _navigationEvent.receiveAsFlow()

    init {
        fetchDatabaseMetrics()
    }

    fun fetchDatabaseMetrics() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val collectionName = _uiState.value.collectionName
                val snapshot = db.collection(collectionName).get().await()

                val totalDocs = snapshot.size()
                var totalFields = 0
                snapshot.documents.forEach { doc ->
                    doc.data?.let { totalFields += it.keys.size }
                }

                _uiState.update { currentState ->
                    currentState.copy(
                        totalDocuments = totalDocs,
                        totalFieldsStored = totalFields,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.localizedMessage) }
            }
        }
    }

    fun triggerFullSync() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, errorMessage = null) }
            val result = repository.syncAllProductsFromFirestore()
            
            _uiState.update { currentState ->
                if (result.isSuccess) {
                    currentState.copy(isSyncing = false)
                } else {
                    currentState.copy(isSyncing = false, errorMessage = result.exceptionOrNull()?.message)
                }
            }
            fetchDatabaseMetrics()
        }
    }

    fun triggerUpsert() {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, errorMessage = null) }
            val result = repository.upsertProductsFromFirestore()
            
            _uiState.update { currentState ->
                if (result.isSuccess) {
                    _navigationEvent.send("products")
                    currentState.copy(isUploading = false)
                } else {
                    currentState.copy(isUploading = false, errorMessage = result.exceptionOrNull()?.message)
                }
            }
            fetchDatabaseMetrics()
        }
    }
}
