package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.ApkItem
import com.example.network.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface StoreUiState {
    object Loading : StoreUiState
    data class Success(val apks: List<ApkItem>) : StoreUiState
    data class Error(val message: String) : StoreUiState
}

class StoreViewModel : ViewModel() {

    private val apiService = ApiService.create()

    private val _uiState = MutableStateFlow<StoreUiState>(StoreUiState.Loading)
    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Tracking active download IDs mapping downloadId -> apkName
    private val _activeDownloads = MutableStateFlow<Map<Long, String>>(emptyMap())
    val activeDownloads = _activeDownloads.asStateFlow()

    init {
        fetchApks()
    }

    fun fetchApks() {
        viewModelScope.launch {
            _uiState.value = StoreUiState.Loading
            try {
                val list = apiService.getApkList()
                _uiState.value = StoreUiState.Success(list)
            } catch (e: Throwable) {
                e.printStackTrace()
                _uiState.value = StoreUiState.Error(
                    e.localizedMessage ?: "Failed to load apps. Ensure you are connected to the Internet."
                )
            }
        }
    }

    // Combine Flow elements reactively to produce filtered APKs on the fly
    val filteredApks: StateFlow<List<ApkItem>> = combine(
        _uiState,
        _searchQuery,
        _selectedCategory
    ) { state, query, category ->
        if (state is StoreUiState.Success) {
            state.apks.filter { item ->
                val matchesQuery = item.name.contains(query, ignoreCase = true) ||
                        item.developer.contains(query, ignoreCase = true) ||
                        item.description.contains(query, ignoreCase = true)
                
                val matchesCategory = if (category == "All") {
                    true
                } else {
                    item.category.trim().equals(category.trim(), ignoreCase = true)
                }
                
                matchesQuery && matchesCategory
            }
        } else {
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Convenience flow for unique active categories parsed directly from loaded APKs
    val categories: StateFlow<List<String>> = combine(_uiState) { states ->
        val list = mutableListOf("All")
        val state = states[0]
        if (state is StoreUiState.Success) {
            val fetchedCategories = state.apks.map { it.category.trim() }.distinct()
            list.addAll(fetchedCategories)
        } else {
            // Default categories fallback
            list.addAll(listOf("VPN", "Tools", "Games", "MOD", "Social"))
        }
        list.distinct()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All", "VPN", "Tools", "Games", "MOD", "Social"))

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun registerDownload(downloadId: Long, apkName: String) {
        val current = _activeDownloads.value.toMutableMap()
        current[downloadId] = apkName
        _activeDownloads.value = current
    }

    fun removeDownload(downloadId: Long) {
        val current = _activeDownloads.value.toMutableMap()
        current.remove(downloadId)
        _activeDownloads.value = current
    }

    /**
     * Helper to generate a reliable and normalized filename for downloads.
     */
    fun getApkFilename(item: ApkItem): String {
        val sanitizedName = item.name.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val sanitizedVersion = item.version.replace("[^a-zA-Z0-9]".toRegex(), "_")
        return "${sanitizedName}_v${sanitizedVersion}.apk"
    }
}
