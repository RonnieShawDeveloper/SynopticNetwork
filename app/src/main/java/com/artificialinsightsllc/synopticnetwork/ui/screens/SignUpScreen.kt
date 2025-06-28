package com.artificialinsightsllc.synopticnetwork.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.artificialinsightsllc.synopticnetwork.R
import com.artificialinsightsllc.synopticnetwork.data.models.ExperienceLevel
import com.artificialinsightsllc.synopticnetwork.data.models.MemberType
import com.artificialinsightsllc.synopticnetwork.data.models.User
import com.artificialinsightsllc.synopticnetwork.navigation.Screen
import com.artificialinsightsllc.synopticnetwork.ui.theme.Accent_Success
import com.artificialinsightsllc.synopticnetwork.ui.theme.SynopticNetworkTheme
import com.artificialinsightsllc.synopticnetwork.ui.theme.Transparent_Black
import com.artificialinsightsllc.synopticnetwork.ui.viewmodels.ScreenNameState
import com.artificialinsightsllc.synopticnetwork.ui.viewmodels.SignUpState
import com.artificialinsightsllc.synopticnetwork.ui.viewmodels.SignUpViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    navController: NavHostController,
    signUpViewModel: SignUpViewModel = viewModel()
) {
    // State for all the input fields
    var email by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var middleName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var screenName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var nwsSpotterId by remember { mutableStateOf("") }
    var hamRadioCallSign by remember { mutableStateOf("") }
    var zipCode by remember { mutableStateOf("") }
    var experienceLevel by remember { mutableStateOf(ExperienceLevel.ENTHUSIAST) }
    var memberType by remember { mutableStateOf(MemberType.STANDARD) }

    // Observe state from the ViewModel
    val signUpState by signUpViewModel.signUpState
    val screenNameState by signUpViewModel.screenNameState

    val focusManager = LocalFocusManager.current

    // Handle navigation and error dialogs based on state changes
    LaunchedEffect(signUpState) {
        when (signUpState) {
            is SignUpState.Success -> {
                navController.navigate(Screen.Main.route) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            }
            else -> { /* Handle other states if needed */ }
        }
    }

    if (signUpState is SignUpState.Error) {
        ErrorAlertDialog(
            title = "Registration Failed",
            message = (signUpState as SignUpState.Error).message,
            onDismiss = { signUpViewModel.resetState() }
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
                // This modifier ensures the content is pushed up above the system navigation bar.
                .navigationBarsPadding()
                // This modifier adjusts the layout when the keyboard appears, making it scrollable.
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopAppBar(
                title = { Text("Create Account", color = Color.White) },
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
                    RegistrationTextField(value = email, onValueChange = { email = it }, label = "Email", keyboardType = KeyboardType.Email)
                    Column {
                        RegistrationTextField(
                            value = screenName,
                            onValueChange = {
                                screenName = it
                                signUpViewModel.onScreenNameChanged(it)
                            },
                            label = "Screen Name (Unique)"
                        )
                        ScreenNameAvailabilityText(state = screenNameState)
                    }
                    RegistrationTextField(value = firstName, onValueChange = { firstName = it }, label = "First Name")
                    RegistrationTextField(value = middleName, onValueChange = { middleName = it }, label = "Middle Name (Optional)")
                    RegistrationTextField(value = lastName, onValueChange = { lastName = it }, label = "Last Name")

                    PasswordRequirements()

                    RegistrationTextField(value = password, onValueChange = { password = it }, label = "Password", visualTransformation = PasswordVisualTransformation(), keyboardType = KeyboardType.Password)
                    RegistrationTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = "Confirm Password", visualTransformation = PasswordVisualTransformation(), keyboardType = KeyboardType.Password)
                    RegistrationTextField(value = zipCode, onValueChange = { zipCode = it }, label = "Zip Code", keyboardType = KeyboardType.Number)
                    RegistrationTextField(value = nwsSpotterId, onValueChange = { nwsSpotterId = it }, label = "NWS Spotter ID (Optional)")
                    RegistrationTextField(value = hamRadioCallSign, onValueChange = { hamRadioCallSign = it }, label = "Ham Radio Call Sign (Optional)")
                    RegistrationDropdown(label = "Experience Level", selectedValue = experienceLevel.name, options = ExperienceLevel.values().map { it.name }, onValueSelected = { experienceLevel = ExperienceLevel.valueOf(it) })
                    RegistrationDropdown(label = "Member Type", selectedValue = memberType.name, options = MemberType.values().map { it.name.replace("_", " ") }, onValueSelected = { memberType = MemberType.valueOf(it.replace(" ", "_")) })

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            val user = User(email = email, screenName = screenName, firstName = firstName, middleName = middleName.takeIf { it.isNotBlank() }, lastName = lastName, zipCode = zipCode, nwsSpotterId = nwsSpotterId.takeIf { it.isNotBlank() }, hamRadioCallSign = hamRadioCallSign.takeIf { it.isNotBlank() }, experienceLevel = experienceLevel.name, memberType = memberType.name)
                            signUpViewModel.onSignUpClicked(user, password)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = signUpState != SignUpState.Loading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (signUpState == SignUpState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Sign Up", modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PasswordRequirements() {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(start = 16.dp, end = 16.dp, bottom = 4.dp)) {
        Text(
            "Password must contain:",
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        RequirementText("At least 6 characters")
        RequirementText("1 uppercase & 1 lowercase letter")
        RequirementText("1 number (0-9)")
        RequirementText("1 special character (e.g., !@#$)")
    }
}

@Composable
private fun RequirementText(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Accent_Success,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

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

@Composable
fun ErrorAlertDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
private fun RegistrationTextField(value: String, onValueChange: (String) -> Unit, label: String, visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None, keyboardType: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth(), label = { Text(label) }, visualTransformation = visualTransformation, keyboardOptions = KeyboardOptions(keyboardType = keyboardType), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.White, unfocusedBorderColor = Color.White.copy(alpha = 0.7f), focusedLabelColor = Color.White, unfocusedLabelColor = Color.White.copy(alpha = 0.7f), cursorColor = MaterialTheme.colorScheme.primary, focusedTextColor = Color.White, unfocusedTextColor = Color.White), shape = RoundedCornerShape(12.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegistrationDropdown(label: String, selectedValue: String, options: List<String>, onValueSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(value = selectedValue.replace("_", " "), onValueChange = {}, readOnly = true, label = { Text(label) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, modifier = Modifier
            .fillMaxWidth()
            .menuAnchor(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.White, unfocusedBorderColor = Color.White.copy(alpha = 0.7f), focusedLabelColor = Color.White, unfocusedLabelColor = Color.White.copy(alpha = 0.7f), focusedTextColor = Color.White, unfocusedTextColor = Color.White), shape = RoundedCornerShape(12.dp))
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(MaterialTheme.colorScheme.secondary)) {
            options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onValueSelected(option); expanded = false }) }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignUpScreenPreview() {
    SynopticNetworkTheme {
        SignUpScreen(rememberNavController())
    }
}
