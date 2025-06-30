package com.artificialinsightsllc.synopticnetwork.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artificialinsightsllc.synopticnetwork.data.models.NwsProductListResponse // Import the new wrapper
import com.artificialinsightsllc.synopticnetwork.data.models.NwsProductListItem // Import the actual item
import com.artificialinsightsllc.synopticnetwork.data.services.NwsApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Data class to hold the UI state for the ProductMenuScreen.
 */
data class ProductMenuUiState(
    val isLoading: Boolean = false,
    val products: List<NwsProductListItem> = emptyList(), // MODIFIED: Now a list of NwsProductListItem
    val errorMessage: String? = null,
    val userWfo: String? = null // To store the user's WFO for fetching products
)

/**
 * ViewModel for the ProductMenuScreen, responsible for fetching and managing
 * the list of available NWS weather products.
 */
class ProductMenuViewModel(
    private val nwsApiService: NwsApiService = NwsApiService()
) : ViewModel() {

    // MODIFIED: Changed visibility from private to internal
    internal val _uiState = MutableStateFlow(ProductMenuUiState())
    val uiState = _uiState.asStateFlow()

    /**
     * Fetches the list of available weather products for the given WFO.
     * This function should be called once the user's WFO is known.
     */
    fun fetchProducts(wfo: String) {
        // Only fetch if WFO is valid and not already loading/loaded for this WFO
        if (wfo.isBlank() || _uiState.value.isLoading || _uiState.value.userWfo == wfo) {
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, userWfo = wfo) }

        viewModelScope.launch {
            // MODIFIED: Call getAvailableProductTypes which now returns NwsProductListResponse
            val response = nwsApiService.getAvailableProductTypes(wfo)
            val products = response.graph // Extract the list from the 'graph' property

            if (products.isNotEmpty()) {
                _uiState.update { it.copy(isLoading = false, products = products) }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "No weather products found for your area, or a network error occurred.") }
            }
        }
    }
}
