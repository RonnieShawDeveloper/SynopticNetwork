package com.artificialinsightsllc.synopticnetwork.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.artificialinsightsllc.synopticnetwork.ui.viewmodels.MakeReportViewModel
import com.yalantis.ucrop.UCrop
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor

/**
 * A full-screen camera composable with a live data overlay.
 *
 * @param navController The navigation controller.
 * @param viewModel The ViewModel shared with the MakeReportScreen.
 */
@Composable
fun CameraScreen(
    navController: NavHostController,
    viewModel: MakeReportViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    // Create and remember a LifecycleCameraController
    val cameraController = remember { LifecycleCameraController(context) }

    // Start and stop sensor updates as the screen enters or leaves the composition
    DisposableEffect(Unit) {
        viewModel.startSensorUpdates()
        onDispose {
            viewModel.stopSensorUpdates()
        }
    }

    // Launcher for uCrop, which starts after the photo is taken
    val uCropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let {
                // Now, take the cropped image, imprint the data, and update the ViewModel
                val bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(it))
                val finalUri = viewModel.imprintDataOnBitmap(context, bitmap)
                finalUri?.let { uri ->
                    viewModel.onImageCropped(uri)
                    navController.popBackStack() // Go back to the report form
                }
            }
        } else {
            // Handle crop cancellation or failure
            navController.popBackStack()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = {
                PreviewView(it).apply {
                    this.controller = cameraController
                    cameraController.bindToLifecycle(lifecycleOwner)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // UI Controls (Overlay and Button) with padding for system bars
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // Live Data Overlay
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                OverlayText(text = "Synoptic Network")
                OverlayText(text = SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US).format(Date()))
                OverlayText(text = "Lat/Lon: ${"%.4f".format(uiState.liveLocation?.latitude)} / ${"%.4f".format(uiState.liveLocation?.longitude)}")
                OverlayText(text = "Direction: ${uiState.liveDirection.toInt()}°")
            }

            // Bottom Controls (Shutter and Warning)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Shutter Button
                IconButton(
                    onClick = {
                        // Capture the data at this exact moment
                        viewModel.onPhotoTaken()

                        // Take the picture
                        val executor = ContextCompat.getMainExecutor(context)
                        takePicture(cameraController, executor, context) { uri ->
                            // Picture saved, now start uCrop
                            val destinationUri = Uri.fromFile(File(context.cacheDir, "cropped_${System.currentTimeMillis()}.jpg"))
                            val uCropOptions = UCrop.Options().apply {
                                setCompressionQuality(80)
                                withAspectRatio(1f, 1f)
                                setHideBottomControls(false)
                                setFreeStyleCropEnabled(false)
                                setToolbarTitle("Crop Photo")
                            }
                            val uCropIntent = UCrop.of(uri, destinationUri)
                                .withOptions(uCropOptions)
                                .getIntent(context)
                            uCropLauncher.launch(uCropIntent)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = "Take Photo",
                        tint = Color.White,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                            .padding(16.dp)
                    )
                }

                // Warning Message
                Text(
                    text = "DO NOT TAKE PHOTOS IN A CAR OR BUILDING!\nINTERFERENCE WILL CAUSE COMPASS TO MALFUNCTION",
                    color = Color.White,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color.Red.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// Helper function to take a picture and handle the result
private fun takePicture(
    cameraController: LifecycleCameraController,
    executor: Executor,
    context: Context,
    onImageSaved: (Uri) -> Unit
) {
    val file = File.createTempFile("temp_photo_", ".jpg", context.cacheDir)
    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

    cameraController.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                outputFileResults.savedUri?.let { onImageSaved(it) }
            }

            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
            }
        }
    )
}

// Helper composable for the overlay text to keep styling consistent
@Composable
private fun OverlayText(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(4.dp)
    )
}
