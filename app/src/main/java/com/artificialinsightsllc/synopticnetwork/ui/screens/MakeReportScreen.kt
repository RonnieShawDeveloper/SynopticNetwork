package com.artificialinsightsllc.synopticnetwork.ui.screens

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.artificialinsightsllc.synopticnetwork.data.models.getReportTypesWithEmojis
import com.artificialinsightsllc.synopticnetwork.navigation.Screen
import com.artificialinsightsllc.synopticnetwork.ui.theme.SynopticNetworkTheme
import com.artificialinsightsllc.synopticnetwork.ui.viewmodels.MakeReportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MakeReportScreen(
    navController: NavHostController,
    makeReportViewModel: MakeReportViewModel = viewModel()
) {
    val uiState by makeReportViewModel.uiState.collectAsState()

    // Handle submission success
    LaunchedEffect(uiState.submissionSuccess) {
        if (uiState.submissionSuccess) {
            // On success, pop back to the main screen
            navController.popBackStack()
            makeReportViewModel.resetSubmissionState()
        }
    }

    // Handle submission errors
    if (uiState.submissionError != null) {
        ErrorAlertDialog(
            title = "Submission Failed",
            message = uiState.submissionError!!,
            onDismiss = { makeReportViewModel.resetSubmissionState() }
        )
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Report") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply Scaffold's default padding (e.g., for TopAppBar)
                .navigationBarsPadding() // Adjust for system navigation bars (bottom)
                .imePadding() // Adjust for keyboard (Input Method Editor)
                .verticalScroll(rememberScrollState()) // Enable scrolling for content
                .padding(horizontal = 16.dp, vertical = 16.dp), // Apply symmetrical padding inside the scrollable area
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Image Placeholder
            ImagePlaceholder(
                imageUri = uiState.croppedImageUri,
                onClick = { navController.navigate(Screen.Camera.route) }
            )

            // Form Fields
            val formEnabled = uiState.croppedImageUri != null
            ReportForm(
                uiState = uiState,
                viewModel = makeReportViewModel,
                enabled = formEnabled
            )

            // Submit Button
            Button(
                onClick = { makeReportViewModel.submitReport() },
                modifier = Modifier.fillMaxWidth(),
                enabled = formEnabled && !uiState.isSubmitting
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Submit Report")
                }
            }
        }
    }
}

@Composable
fun ImagePlaceholder(imageUri: Uri?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (imageUri != null) {
            Image(
                painter = rememberAsyncImagePainter(imageUri),
                contentDescription = "Report Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Take Photo",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportForm(
    uiState: com.artificialinsightsllc.synopticnetwork.ui.viewmodels.MakeReportState,
    viewModel: MakeReportViewModel,
    enabled: Boolean
) {
    // Get the full list of report types from the same source as the MainScreen legend.
    // We only need the names (first element of the pair).
    val reportTypes = getReportTypesWithEmojis().map { it.first }
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Report Type Dropdown
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
        ) {
            OutlinedTextField(
                value = uiState.reportType,
                onValueChange = { },
                readOnly = true,
                label = { Text("Report Type") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                enabled = enabled
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                reportTypes.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type) },
                        onClick = {
                            viewModel.onReportTypeChange(type)
                            expanded = false
                        }
                    )
                }
            }
        }

        // Send to NWS Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Send to National Weather Service?", fontWeight = FontWeight.Medium)
            Switch(
                checked = uiState.sendToNws,
                onCheckedChange = { viewModel.onSendToNwsChange(it) },
                enabled = enabled
            )
        }

        // Comments
        OutlinedTextField(
            value = uiState.comments,
            onValueChange = { viewModel.onCommentsChange(it) },
            label = { Text("Comments") },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            maxLines = 4
        )

        // Phone Number with description
        Column {
            Text(
                text = "This number is optional and is kept private. It is used only if NWS employee's need to contact you about your report to get further details.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
            OutlinedTextField(
                value = uiState.phoneNumber,
                onValueChange = { viewModel.onPhoneNumberChange(it) },
                label = { Text("Phone Number (Not Public)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun MakeReportScreenPreview() {
    SynopticNetworkTheme {
        MakeReportScreen(navController = rememberNavController())
    }
}
