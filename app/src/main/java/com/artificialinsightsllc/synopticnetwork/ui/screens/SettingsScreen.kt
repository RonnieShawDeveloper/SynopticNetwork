package com.artificialinsightsllc.synopticnetwork.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.artificialinsightsllc.synopticnetwork.R
import com.artificialinsightsllc.synopticnetwork.data.models.ExperienceLevel
import com.artificialinsightsllc.synopticnetwork.data.models.MemberType
import com.artificialinsightsllc.synopticnetwork.ui.theme.Accent_Success
import com.artificialinsightsllc.synopticnetwork.ui.theme.SynopticNetworkTheme
import com.artificialinsightsllc.synopticnetwork.ui.theme.Transparent_Black
import com.artificialinsightsllc.synopticnetwork.ui.viewmodels.ScreenNameState
import com.artificialinsightsllc.synopticnetwork.ui.viewmodels.SettingsViewModel
import androidx.compose.material.icons.filled.Radio // Import the new icon

/**
 * The Settings screen where users can view and update their profile information.
 *
 * @param navController The navigation controller.
 * @param settingsViewModel The ViewModel for this screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val uiState by settingsViewModel.uiState.collectAsState()

    // Dialogs for confirmations and errors
    if (uiState.showLogoutConfirmation) {
        ConfirmAlertDialog(
            title = "Logout Confirmation",
            message = "Are you sure you want to log out?",
            onConfirm = { settingsViewModel.performLogout() },
            onDismiss = { settingsViewModel.dismissLogoutConfirmation() }
        )
    }

    if (uiState.showPasswordResetConfirmation) {
        AlertDialog(
            onDismissRequest = { settingsViewModel.dismissPasswordResetConfirmation() },
            title = { Text("Password Reset Email Sent") },
            text = { Text("A password reset email has been sent to your registered email address. Please follow the instructions in the email.") },
            confirmButton = {
                Button(onClick = { settingsViewModel.dismissPasswordResetConfirmation() }) {
                    Text("OK")
                }
            }
        )
    }

    if (uiState.errorMessage != null) {
        ErrorAlertDialog(
            title = "Error",
            message = uiState.errorMessage!!,
            onDismiss = { settingsViewModel.dismissErrorMessage() }
        )
    }

    if (uiState.showSuccessMessage != null) {
        AlertDialog(
            onDismissRequest = { settingsViewModel.dismissSuccessMessage() },
            title = { Text("Success") },
            text = { Text(uiState.showSuccessMessage!!) },
            confirmButton = {
                Button(onClick = { settingsViewModel.dismissSuccessMessage() }) {
                    Text("OK")
                }
            }
        )
    }

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
                .navigationBarsPadding()
                .imePadding(), // Adjust for keyboard
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopAppBar(
                title = { Text("Settings", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .weight(1f)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Transparent_Black)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "User Profile",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Non-editable fields
                    InfoField(label = "Email", value = uiState.currentUser?.email ?: "", icon = Icons.Default.Email)
                    InfoField(label = "First Name", value = uiState.currentUser?.firstName ?: "", icon = Icons.Default.Person)
                    InfoField(label = "Last Name", value = uiState.currentUser?.lastName ?: "", icon = Icons.Default.Person)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Editable fields
                    CustomTextField(
                        value = uiState.screenName,
                        onValueChange = { settingsViewModel.onScreenNameChanged(it) },
                        label = "Screen Name",
                        icon = Icons.Default.Person,
                        enabled = !uiState.isSaving
                    )
                    ScreenNameAvailabilityText(state = uiState.screenNameAvailability)

                    CustomTextField(
                        value = uiState.zipCode,
                        onValueChange = { settingsViewModel.onZipCodeChanged(it) },
                        label = "Zip Code",
                        icon = Icons.Default.Place,
                        keyboardType = KeyboardType.Number,
                        enabled = !uiState.isSaving
                    )

                    CustomTextField(
                        value = uiState.nwsSpotterId,
                        onValueChange = { settingsViewModel.onNwsSpotterIdChanged(it) },
                        label = "NWS Spotter ID (Optional)",
                        icon = Icons.Default.Info,
                        enabled = !uiState.isSaving
                    )

                    CustomTextField(
                        value = uiState.hamRadioCallSign,
                        onValueChange = { settingsViewModel.onHamRadioCallSignChanged(it) },
                        label = "Ham Radio Call Sign (Optional)",
                        icon = Icons.Default.Radio, // Changed icon to Radio
                        enabled = !uiState.isSaving
                    )

                    CustomDropdown(
                        label = "Experience Level",
                        selectedValue = uiState.experienceLevel,
                        options = ExperienceLevel.values().map { it.name },
                        onValueSelected = { settingsViewModel.onExperienceLevelChanged(it) },
                        enabled = !uiState.isSaving
                    )

                    CustomDropdown(
                        label = "Member Type",
                        selectedValue = uiState.memberType,
                        options = MemberType.values().map { it.name.replace("_", " ") },
                        onValueSelected = { settingsViewModel.onMemberTypeChanged(it.replace(" ", "_")) },
                        enabled = !uiState.isSaving
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action Buttons
                    Button(
                        onClick = { settingsViewModel.sendPasswordReset() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSaving && uiState.currentUser?.email != null,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Key, contentDescription = "Change Password", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Change Password", modifier = Modifier.padding(vertical = 8.dp), fontSize = 16.sp)
                    }

                    Button(
                        onClick = { settingsViewModel.saveProfile() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSaving, // Disable while saving
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Save Changes", modifier = Modifier.padding(vertical = 8.dp), fontSize = 16.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = { settingsViewModel.confirmLogout() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSaving
                    ) {
                        Text(
                            text = "Logout",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Reusable Composable for displaying non-editable user information.
 */
@Composable
private fun InfoField(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp, topEnd = 50.dp, bottomEnd = 50.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)) // Lighter transparent background
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
            Text(text = value, style = MaterialTheme.typography.bodyLarge, color = Color.White)
        }
    }
}


/**
 * Reusable Composable for text input fields with custom styling.
 */
@Composable
private fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.White,
            unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
            focusedLabelColor = Color.White,
            unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            disabledTextColor = Color.White.copy(alpha = 0.7f),
            disabledBorderColor = Color.White.copy(alpha = 0.5f),
            disabledLabelColor = Color.White.copy(alpha = 0.5f),
            // The disabled leading icon color is now handled by the trailingIcon logic if it's there
        ),
        shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp, topEnd = 50.dp, bottomEnd = 50.dp),
        trailingIcon = { // Moved from leadingIcon to trailingIcon
            Box(
                modifier = Modifier
                    .size(53.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (enabled) Color.White else Color.White.copy(alpha = 0.5f) // Adjust tint based on enabled state
                )
            }
        }
    )
}

/**
 * Reusable Composable for dropdown selection fields with custom styling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomDropdown(
    label: String,
    selectedValue: String,
    options: List<String>,
    onValueSelected: (String) -> Unit,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded && enabled } // Only expand if enabled
    ) {
        OutlinedTextField(
            value = selectedValue.replace("_", " "), // Display user-friendly string
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(), // Designates this as the anchor for the dropdown
            enabled = enabled,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                disabledTextColor = Color.White.copy(alpha = 0.7f),
                disabledBorderColor = Color.White.copy(alpha = 0.5f),
                disabledLabelColor = Color.White.copy(alpha = 0.5f),
                disabledTrailingIconColor = Color.White.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.secondary) // Background for dropdown items
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Reusable Composable for displaying screen name availability status.
 */
@Composable
private fun ScreenNameAvailabilityText(state: ScreenNameState) {
    val (text, color) = when (state) {
        ScreenNameState.CHECKING -> "Checking..." to Color.White
        ScreenNameState.AVAILABLE -> "Available!" to Accent_Success
        ScreenNameState.TAKEN -> "Taken" to MaterialTheme.colorScheme.error
        ScreenNameState.IDLE -> "" to Color.Transparent
    }
    AnimatedVisibility(visible = text.isNotEmpty()) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 4.dp)
        )
    }
}

/**
 * Reusable Composable for confirmation dialogs.
 */
@Composable
fun ConfirmAlertDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SynopticNetworkTheme {
        SettingsScreen(rememberNavController())
    }
}
