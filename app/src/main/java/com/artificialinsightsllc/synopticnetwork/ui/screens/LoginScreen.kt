package com.artificialinsightsllc.synopticnetwork.ui.screens

import android.Manifest
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.artificialinsightsllc.synopticnetwork.R
import com.artificialinsightsllc.synopticnetwork.navigation.Screen
import com.artificialinsightsllc.synopticnetwork.ui.theme.SynopticNetworkTheme
import com.artificialinsightsllc.synopticnetwork.ui.theme.Transparent_Black
import com.artificialinsightsllc.synopticnetwork.ui.viewmodels.LoginState
import com.artificialinsightsllc.synopticnetwork.ui.viewmodels.LoginViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

// Define the color for the legal text links
private val LegalGold = Color(0xFFFFA726)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LoginScreen(
    navController: NavHostController,
    loginViewModel: LoginViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var startAnimation by remember { mutableStateOf(false) }

    val loginState by loginViewModel.loginState
    val focusManager = LocalFocusManager.current

    // This effect now handles both successful login and navigating if already logged in.
    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) {
            navController.navigate(Screen.Main.route) {
                // Clear the back stack to prevent going back to the login screen
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
        }
    }

    if (loginState is LoginState.Error) {
        ErrorAlertDialog(
            title = "Login Failed",
            message = (loginState as LoginState.Error).message,
            onDismiss = { loginViewModel.resetState() }
        )
    }

    // This handles the one-time permission request flow.
    PermissionHandler(
        onPermissionFlowFinished = {
            loginViewModel.onPermissionFlowFinished()
        }
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = "Background Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // If the initial check finds the user is already logged in, show a loading
        // indicator instead of the login form to prevent a UI flash.
        if (loginState is LoginState.Success) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Only show the login form if the user is not already logged in.
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .alpha(animateFloatAsState(if (startAnimation) 1f else 0f, tween(1000, 200)).value)
                        .offset(y = animateFloatAsState(if (startAnimation) 0f else 50f, tween(1000, 200)).value.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Transparent_Black)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_splash_logo),
                            contentDescription = "App Logo",
                            modifier = Modifier.size(80.dp)
                        )
                        Text(
                            text = "The Synoptic Network",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        CustomTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = "Email",
                            icon = Icons.Default.Email,
                            enabled = loginState != LoginState.Loading
                        )
                        CustomTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = "Password",
                            icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            onIconClick = { passwordVisible = !passwordVisible },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardType = KeyboardType.Password,
                            enabled = loginState != LoginState.Loading
                        )

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                loginViewModel.onLoginClicked(email, password)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = loginState != LoginState.Loading,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (loginState == LoginState.Loading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Login", modifier = Modifier.padding(vertical = 8.dp), fontSize = 16.sp)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Sign Up",
                                color = Color.White,
                                modifier = Modifier.clickable { navController.navigate(Screen.SignUp.route) }
                            )
                            Text(
                                text = "Forgot Password?",
                                color = Color.White,
                                modifier = Modifier.clickable { navController.navigate(Screen.ForgotPassword.route) }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Terms of Service",
                                color = LegalGold,
                                fontSize = 12.sp,
                                modifier = Modifier.clickable { /* TODO: Navigate to Terms */ }
                            )
                            Text(
                                text = "Privacy Policy",
                                color = LegalGold,
                                fontSize = 12.sp,
                                modifier = Modifier.clickable { /* TODO: Navigate to Privacy Policy */ }
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "© 2025 Synoptic Networks | Version 1.0.0 ",
                color = Color.White,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Not to be used for life saving decisions",
                color = Color.White,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionHandler(
    onPermissionFlowFinished: () -> Unit
) {
    // Build the list of permissions dynamically.
    val permissions = remember {
        mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION
        ).apply {
            // Only add the notifications permission on Android 13 (TIRAMISU) and above.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val permissionState = rememberMultiplePermissionsState(permissions = permissions)
    var showRationaleDialog by remember { mutableStateOf(false) }

    // This effect runs whenever the permission state changes.
    LaunchedEffect(permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted) {
            // Permissions are granted, so the flow is finished.
            onPermissionFlowFinished()
        } else {
            // Permissions are not granted, so we need to show the dialog.
            showRationaleDialog = true
        }
    }

    if (showRationaleDialog) {
        PermissionExplainerDialog(
            onConfirm = {
                showRationaleDialog = false
                permissionState.launchMultiplePermissionRequest()
            }
        )
    }
}

@Composable
private fun PermissionExplainerDialog(onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* Do nothing, force a choice */ },
        icon = { Icon(Icons.Default.Info, contentDescription = null) },
        title = { Text("Permissions Required") },
        text = {
            Text(
                "To provide the best experience, The Synoptic Network needs access to your:" +
                        "\n\n\u2022 Camera: To take and upload weather photos." +
                        "\n\u2022 Location: To automatically tag your reports with your current location." +
                        "\n\u2022 Notifications: To send you important, localized weather alerts."
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("OK")
            }
        }
    )
}

@Composable
private fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    onIconClick: (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        label = { Text(label) },
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
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
        ),
        shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp, topEnd = 50.dp, bottomEnd = 50.dp),
        trailingIcon = {
            Box(
                modifier = Modifier
                    .size(53.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
                    .clickable(enabled = onIconClick != null && enabled) { onIconClick?.invoke() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White
                )
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    SynopticNetworkTheme {
        LoginScreen(rememberNavController())
    }
}
