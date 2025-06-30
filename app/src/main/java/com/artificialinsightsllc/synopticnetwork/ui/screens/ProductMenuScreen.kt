package com.artificialinsightsllc.synopticnetwork.ui.screens

import android.util.Log // Import Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.artificialinsightsllc.synopticnetwork.R
import com.artificialinsightsllc.synopticnetwork.data.models.NwsProductListResponse
import com.artificialinsightsllc.synopticnetwork.data.models.NwsProductListItem // NEW: Import NwsProductListItem
import com.artificialinsightsllc.synopticnetwork.navigation.Screen
import com.artificialinsightsllc.synopticnetwork.ui.theme.SynopticNetworkTheme
import com.artificialinsightsllc.synopticnetwork.ui.theme.Transparent_Black
import com.artificialinsightsllc.synopticnetwork.ui.viewmodels.MainViewModel // To get user's WFO
import com.artificialinsightsllc.synopticnetwork.ui.viewmodels.ProductMenuUiState
import com.artificialinsightsllc.synopticnetwork.ui.viewmodels.ProductMenuViewModel
import kotlinx.coroutines.flow.update

/**
 * Composable for the Product Menu screen, displaying a list of available weather products.
 *
 * @param navController The navigation controller.
 * @param wfo The WFO code passed as a navigation argument.
 * @param productMenuViewModel The ViewModel for this screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductMenuScreen(
    navController: NavHostController,
    wfo: String, // MODIFIED: Now accepts WFO as a direct parameter
    productMenuViewModel: ProductMenuViewModel = viewModel()
) {
    val uiState by productMenuViewModel.uiState.collectAsState()

    // Fetch products when the screen is first composed and WFO is available
    LaunchedEffect(wfo) { // MODIFIED: Depend on the passed WFO
        Log.d("ProductMenuScreen", "LaunchedEffect triggered with WFO: $wfo")
        if (wfo.isNotBlank()) {
            productMenuViewModel.fetchProducts(wfo) // Use the passed WFO directly
        } else {
            Log.w("ProductMenuScreen", "WFO is blank, not fetching products.")
            // Optionally, set an error message in the ViewModel if WFO is missing
            productMenuViewModel._uiState.update { it.copy(errorMessage = "WFO information is missing. Cannot fetch products.") }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weather Products", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.background),
                contentDescription = "Background Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.errorMessage != null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = uiState.errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else if (uiState.products.isNotEmpty()) {
                    Log.d("ProductMenuScreen", "Displaying ${uiState.products.size} products.")
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(uiState.products) { product -> // 'product' here is NwsProductListItem
                            ProductCard(product = product) {
                                // Navigate to the detail screen with productCode and WFO
                                // Use the WFO passed to this screen
                                navController.navigate(Screen.ProductDetail.createRoute(product.productCode, wfo))
                            }
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) } // Spacer at the bottom
                    }
                } else {
                    Log.d("ProductMenuScreen", "No products available in uiState.products.")
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No weather products available for your location.",
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Composable for a single product card in the menu.
 * MODIFIED: Now accepts NwsProductListItem.
 */
@Composable
fun ProductCard(product: NwsProductListItem, onClick: () -> Unit) { // MODIFIED: Parameter type
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Transparent_Black)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start // Align content to start
        ) {
            // Filled circle with Emoji
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary), // Use secondary color for the circle
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getProductEmoji(product.productCode),
                    fontSize = 24.sp,
                    color = Color.White // Emoji color
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            // Product Name
            Text(
                text = product.productName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White, // Text color
                modifier = Modifier.weight(1f) // Allow text to take available space
            )
        }
    }
}

/**
 * Helper function to map product codes to relevant emojis.
 */
fun getProductEmoji(productCode: String): String {
    return when (productCode.uppercase()) {
        "AFD" -> "💬" // Area Forecast Discussion
        "CF6" -> "📊" // WFO Monthly/Daily Climate Data
        "CLI" -> "📅" // Climatological Report (Daily)
        "ZFP" -> "🗺️" // Zone Forecast Product
        "RWS" -> "🌬️" // Rawinsonde Data
        "SPS" -> "⚠️" // Special Weather Statement
        "NPW" -> "📰" // Public Information Statement
        "HWO" -> "🌀" // Hazardous Weather Outlook
        "MWS" -> "🌊" // Marine Weather Statement
        "FWF" -> "🔥" // Fire Weather Forecast
        "LSR" -> "🚨" // Local Storm Report
        "FFW" -> "💧" // Flash Flood Watch/Warning
        "TOR" -> "🌪️" // Tornado Warning
        "SVR" -> "⛈️" // Severe Thunderstorm Warning
        "SMW" -> "⛵" // Special Marine Warning
        "FLW" -> "🌊" // Flood Warning
        "ESF" -> "🚨" // Emergency Support Function
        "WSW" -> "❄️" // Winter Storm Warning
        "NOW" -> "⚡" // Short Term Forecast
        else -> "📄" // Generic document emoji for others
    }
}


@Preview(showBackground = true)
@Composable
fun ProductMenuScreenPreview() {
    SynopticNetworkTheme {
        // For preview, we need to mock the ViewModel state
        val mockViewModel = ProductMenuViewModel()
        LaunchedEffect(Unit) {
            // Manually set some mock products for preview using NwsProductListItem
            mockViewModel._uiState.value = ProductMenuUiState(
                products = listOf(
                    NwsProductListItem("AFD", "Area Forecast Discussion"),
                    NwsProductListItem("CF6", "WFO Monthly/Daily Climate Data"),
                    NwsProductListItem("ZFP", "Zone Forecast Product"),
                    NwsProductListItem("SPS", "Special Weather Statement"),
                    NwsProductListItem("OTHER", "Some Other Product")
                ),
                userWfo = "TBW"
            )
        }
        // Pass a mock WFO for the preview
        ProductMenuScreen(navController = rememberNavController(), wfo = "TBW", productMenuViewModel = mockViewModel)
    }
}
