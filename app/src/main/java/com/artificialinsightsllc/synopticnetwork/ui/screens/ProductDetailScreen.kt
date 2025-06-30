package com.artificialinsightsllc.synopticnetwork.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.artificialinsightsllc.synopticnetwork.data.models.NwsProductDetailResponse
import com.artificialinsightsllc.synopticnetwork.ui.theme.SynopticNetworkTheme
import com.artificialinsightsllc.synopticnetwork.ui.viewmodels.ProductDetailViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Composable for displaying the detailed text content of a specific NWS weather product.
 *
 * @param navController The navigation controller.
 * @param productCode The 3-letter product code (e.g., "AFD", "CF6") passed as a navigation argument.
 * @param officeCode The 3-letter WFO identifier (e.g., "TBW") passed as a navigation argument.
 * @param productDetailViewModel The ViewModel for this screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    navController: NavHostController,
    productCode: String,
    officeCode: String, // MODIFIED: Changed parameter name
    productDetailViewModel: ProductDetailViewModel = viewModel()
) {
    val uiState by productDetailViewModel.uiState.collectAsState()

    // Fetch product detail when the screen is first composed or arguments change
    LaunchedEffect(productCode, officeCode) { // MODIFIED: Depend on officeCode
        productDetailViewModel.fetchProductDetail(productCode, officeCode) // MODIFIED: Pass officeCode
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.productDetail?.productName ?: "Weather Product", color = Color.White) },
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
                } else if (uiState.productDetail != null) {
                    ProductContent(product = uiState.productDetail!!)
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No product content available.",
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

@Composable
private fun ProductContent(product: NwsProductDetailResponse) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp)) // Semi-transparent background for content
            .padding(16.dp)
            .verticalScroll(rememberScrollState()), // Enable scrolling for long content
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        // Product Metadata
        Text(
            text = product.productName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        product.issuingOffice?.let {
            Text(
                text = "Issuing Office: ${it.removePrefix("K")}", // Remove leading 'K' if present
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
        product.issuanceTime?.let {
            Text(
                text = "Issued: ${formatProductTimestamp(it)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Product Text Content
        // Use Monospace font to preserve formatting (tabs, columns)
        Text(
            text = product.productText.replace("\t", "    "), // Replace tabs with spaces for consistent alignment
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = Color.White,
            lineHeight = 18.sp // Adjust line height for better readability of monospaced text
        )
    }
}

/**
 * Helper function to format ISO 8601 timestamps for product display.
 */
private fun formatProductTimestamp(isoTimestamp: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        val formatter = SimpleDateFormat("MMM dd,yyyy HH:mm z", Locale.US)
        formatter.format(parser.parse(isoTimestamp) ?: Date())
    } catch (e: Exception) {
        e.printStackTrace()
        isoTimestamp // Return original if parsing fails
    }
}


@Preview(showBackground = true)
@Composable
fun ProductDetailScreenPreview() {
    SynopticNetworkTheme {
        // Mock data for preview
        val mockProduct = NwsProductDetailResponse(
            id = "mock-id",
            wmoCollectiveId = "FXUS62",
            issuingOffice = "KTBW",
            issuanceTime = "2025-06-30T17:56:00+00:00",
            productCode = "AFD",
            productName = "Area Forecast Discussion",
            productText = "000\nFXUS62 KTBW 301756\nAFDTBW\n\nArea Forecast Discussion\nNational Weather Service Tampa Bay Ruskin FL\n156 PM EDT Mon Jun 30 2025\n\n.DISCUSSION...\nIssued at 154 PM EDT Mon Jun 30 2025\n\nWe continue to see a huge complex of rain sitting over the Gulf this \nafternoon. For the most part these showers have stayed off the coast \nhowever southwesterly flow will cause us to slowly see the showers \nstarting to push towards the coast in the afternoon and evening \nhours. We will also see our typical seabreeze thunderstorms start to \ndevelop over the next few hours inland. The southwesterly flow will \nkeep us under a wet pattern through mid week with highest rainfall \ntotals along the coast areas. \n\n.AVIATION...\n(18Z TAFS)\nIssued at 154 PM EDT Mon Jun 30 2025\n\nShowers and storms will be possible throughout the day starting \nalong the coast and spreading inland. Tried to highlight the best \nchance to see thunderstorms with VCTS in the late morning through \nthe afternoon. Storms will be clearing out by the early overnight \nhours. \n\n.MARINE...\nIssued at 154 PM EDT Mon Jun 30 2025\n\nA very moist southwesterly flow will remain in place \nthrough Wednesday with widespread showers expected across the Gulf \nwith speeds around 5 to 10 knots outside of storms. As we go into \nThursday and Friday a front will stall across North Florida. This \nwill continue to bring widespread showers to the Gulf with winds \nstaying out of the south and west around 5 to 10 knot. NHC is \ncurrently monitoring that areas for possible development along the \nfront Thursday and Friday but there is no clear picture if or when \nit would development and chances are currently at 20%. \n\n.FIRE WEATHER...\nIssued at 154 PM EDT Mon Jun 30 2025\n\nSummertime convection and humidity will keep fire danger at a \nminimum. \n\n.PRELIMINARY POINT TEMPS/POPS...\nTPA  88  75  88  78 /  70  40  60  40 \nFMY  91  74  91  76 /  70  40  70  30 \nGIF  88  73  91  75 /  60  30  70  30 \nSRQ  88  73  89  76 /  60  30  50  40 \nBKV  88  71  89  73 /  70  40  60  40 \nSPG  86  77  88  78 /  70  40  60  40 \n\nSea Breeze Thunderstorm Regime For Monday: 4\nSea Breeze Thunderstorm Regime For Tuesday: 7\n\nFor additional information on sea breeze regimes, go to: \n     https://www.weather.gov/tbw/ThunderstormClimatology\n\n.TBW WATCHES/WARNINGS/ADVISORIES...\nFL...None.\nGulf waters...None.\n\n$$\n\nDISCUSSION/AVIATION/MARINE/FIRE WEATHER...Shiveley\nDECISION SUPPORT...Close\nUPPER AIR/CLIMATE...Davis\n"
        )
        val mockViewModel = ProductDetailViewModel()
        LaunchedEffect(Unit) {
            mockViewModel._uiState.value = mockViewModel.uiState.value.copy(productDetail = mockProduct)
        }
        ProductDetailScreen(navController = rememberNavController(), productCode = "AFD", officeCode = "TBW", productDetailViewModel = mockViewModel)
    }
}

