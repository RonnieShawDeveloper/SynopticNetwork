package com.artificialinsightsllc.synopticnetwork.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artificialinsightsllc.synopticnetwork.data.models.NwsProductDetailResponse
import com.artificialinsightsllc.synopticnetwork.data.services.NwsApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Data class to hold the UI state for the ProductDetailScreen.
 */
data class ProductDetailUiState(
    val isLoading: Boolean = false,
    val productDetail: NwsProductDetailResponse? = null,
    val errorMessage: String? = null
)

/**
 * ViewModel for the ProductDetailScreen, responsible for fetching and managing
 * the detailed content of a specific NWS weather product.
 */
class ProductDetailViewModel(
    private val nwsApiService: NwsApiService = NwsApiService()
) : ViewModel() {

    // MODIFIED: Changed visibility from private to internal
    internal val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState = _uiState.asStateFlow()

    /**
     * Fetches the detailed content for a specific weather product.
     * This function should be called with the product code and WFO from navigation arguments.
     *
     * @param productCode The 3-letter product code (e.g., "AFD", "CF6").
     * @param wfo The 3-letter WFO identifier (e.g., "TBW").
     */
    fun fetchProductDetail(productCode: String, wfo: String) {
        // Prevent re-fetching if already loading or if data is already present for the same product/WFO
        if (_uiState.value.isLoading || (_uiState.value.productDetail?.productCode == productCode && _uiState.value.productDetail?.issuingOffice?.removePrefix("K") == wfo)) {
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, productDetail = null) }

        viewModelScope.launch {
            val detail = nwsApiService.getLatestProduct(productCode, wfo)
            if (detail != null) {
                _uiState.update { it.copy(isLoading = false, productDetail = detail) }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Product details not found or a network error occurred.") }
            }
        }
    }
}
