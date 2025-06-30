package com.artificialinsightsllc.synopticnetwork.ui.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.artificialinsightsllc.synopticnetwork.data.models.Report
import com.artificialinsightsllc.synopticnetwork.data.services.AuthService
import com.artificialinsightsllc.synopticnetwork.data.services.NwsApiService
import com.artificialinsightsllc.synopticnetwork.data.services.ReportService
import com.artificialinsightsllc.synopticnetwork.data.services.StorageService
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

// Add the GeoHash import here
import ch.hsr.geohash.GeoHash

/**
 * Data class to hold all the state for the report creation screen.
 */
data class MakeReportState(
    // The URI of the final, cropped image displayed on the screen.
    val croppedImageUri: Uri? = null,
    // The captured location (lat/lon) of the report.
    val capturedLocation: LatLng? = null,
    // The captured direction the camera was facing.
    val capturedDirection: Float = 0f,
    // The captured timestamp.
    val capturedTimestamp: Long = 0L,
    // Live data for the camera overlay
    val liveLocation: LatLng? = null,
    val liveDirection: Float = 0f,
    // Form field states
    val reportType: String = "",
    val sendToNws: Boolean = false,
    val comments: String = "",
    val phoneNumber: String = "",
    // A flag to indicate if we are currently processing/uploading.
    val isSubmitting: Boolean = false,
    val submissionSuccess: Boolean = false,
    val submissionError: String? = null
)

// We now use AndroidViewModel to get access to the application context for sensors.
class MakeReportViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    private val _uiState = MutableStateFlow(MakeReportState())
    val uiState = _uiState.asStateFlow()

    // Services
    private val authService = AuthService()
    private val storageService = StorageService()
    private val reportService = ReportService()
    private val nwsApiService = NwsApiService()

    // Sensor and Location properties
    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVectorSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    // Throttling for sensor updates
    private var lastSensorUpdateTime: Long = 0

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { location ->
                _uiState.update { it.copy(liveLocation = LatLng(location.latitude, location.longitude)) }
            }
        }
    }

    // Called when the user returns from uCrop with a cropped image.
    fun onImageCropped(uri: Uri) {
        _uiState.update { it.copy(croppedImageUri = uri) }
    }

    // Called at the moment the photo is taken in the camera view.
    fun onPhotoTaken() {
        _uiState.update {
            it.copy(
                capturedLocation = it.liveLocation,
                capturedDirection = it.liveDirection,
                capturedTimestamp = System.currentTimeMillis()
            )
        }
    }

    // This function takes the original cropped bitmap and imprints the captured data onto it.
    fun imprintDataOnBitmap(context: Context, sourceBitmap: Bitmap): Uri? {
        val state = _uiState.value
        val newBitmap = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(newBitmap)
        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 40f // Adjust text size as needed
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(5f, 2f, 2f, Color.BLACK) // Add a shadow for better visibility
        }

        val sdf = SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US)
        val dateString = sdf.format(Date(state.capturedTimestamp))

        val latLonString = "Lat/Lon: ${"%.4f".format(state.capturedLocation?.latitude)}/${"%.4f".format(state.capturedLocation?.longitude)}"
        val directionString = "Direction: ${state.capturedDirection.toInt()}°"

        // Position the text on the canvas
        val x = 20f
        var y = 50f
        canvas.drawText("The Synoptic Network", x, y, paint)
        y += paint.fontSpacing
        canvas.drawText(dateString, x, y, paint)
        y += paint.fontSpacing
        canvas.drawText(latLonString, x, y, paint)
        y += paint.fontSpacing
        canvas.drawText(directionString, x, y, paint)

        return saveBitmapToTempFile(context, newBitmap)
    }

    // Saves the modified bitmap to a temporary file and returns its URI.
    private fun saveBitmapToTempFile(context: Context, bitmap: Bitmap): Uri? {
        return try {
            val outputDir = context.cacheDir
            val outputFile = File.createTempFile("imprinted_", ".jpg", outputDir)
            val fos = FileOutputStream(outputFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            fos.flush()
            fos.close()
            Uri.fromFile(outputFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * The main function to orchestrate the entire report submission process.
     */
    fun submitReport() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }

            val state = _uiState.value
            val userId = authService.getCurrentUserId()
            val location = state.capturedLocation

            // --- Validation ---
            if (userId == null) {
                _uiState.update { it.copy(isSubmitting = false, submissionError = "User not authenticated.") }
                return@launch
            }
            if (state.croppedImageUri == null) {
                _uiState.update { it.copy(isSubmitting = false, submissionError = "Image is missing.") }
                return@launch
            }
            if (location == null) {
                _uiState.update { it.copy(isSubmitting = false, submissionError = "Location is missing.") }
                return@launch
            }

            // 1. Get WFO and Zone from location
            val pointData = nwsApiService.getNwsPointData(location.latitude, location.longitude)
            val wfo = pointData?.properties?.gridId
            val zone = pointData?.properties?.forecastZone?.substringAfterLast("/")
            if (wfo == null || zone == null) {
                _uiState.update { it.copy(isSubmitting = false, submissionError = "Could not determine NWS forecast zone for this location.") }
                return@launch
            }

            // 2. Upload image to Storage
            val imageUrl = storageService.uploadReportImage(userId, state.croppedImageUri)
            if (imageUrl == null) {
                _uiState.update { it.copy(isSubmitting = false, submissionError = "Failed to upload image.") }
                return@launch
            }

            // Calculate Geohash with 7-digit precision
            val geohash = GeoHash.withBitPrecision(location.latitude, location.longitude, 35).toBase32()
            // NEW: Calculate Geohash with 3-character precision
            val geohash3Char = GeoHash.withBitPrecision(location.latitude, location.longitude, 15).toBase32()


            // 3. Create Report object
            val report = Report(
                userId = userId,
                location = GeoPoint(location.latitude, location.longitude),
                imageUrl = imageUrl,
                direction = state.capturedDirection,
                reportType = state.reportType,
                comments = state.comments.takeIf { it.isNotBlank() },
                sendToNws = state.sendToNws,
                phoneNumber = state.phoneNumber.takeIf { it.isNotBlank() },
                wfo = wfo,
                zone = zone,
                geohash = geohash, // Assign the 7-character geohash
                geohash3Char = geohash3Char // NEW: Assign the 3-character geohash
            )

            // 4. Save report to Firestore
            val success = reportService.createReport(report)
            if (success) {
                _uiState.update { it.copy(isSubmitting = false, submissionSuccess = true) }
            } else {
                _uiState.update { it.copy(isSubmitting = false, submissionError = "Failed to save report to database.") }
            }
        }
    }

    // Update functions for form fields
    fun onReportTypeChange(type: String) {
        _uiState.update { it.copy(reportType = type) }
    }
    fun onSendToNwsChange(send: Boolean) {
        _uiState.update { it.copy(sendToNws = send) }
    }
    fun onCommentsChange(text: String) {
        _uiState.update { it.copy(comments = text) }
    }
    fun onPhoneNumberChange(number: String) {
        _uiState.update { it.copy(phoneNumber = number) }
    }

    fun resetSubmissionState() {
        _uiState.update { it.copy(submissionSuccess = false, submissionError = null) }
    }

    // --- Sensor and Location Lifecycle ---

    @SuppressLint("MissingPermission")
    fun startSensorUpdates() {
        rotationVectorSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    fun stopSensorUpdates() {
        sensorManager.unregisterListener(this)
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            val currentTime = System.currentTimeMillis()
            if ((currentTime - lastSensorUpdateTime) < 500) {
                return // Throttle updates
            }
            lastSensorUpdateTime = currentTime

            // Get the rotation matrix from the sensor event
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

            // Remap the coordinate system to a "world" coordinate system where
            // the Y-axis points north and the Z-axis points straight up.
            // This is crucial for getting a stable azimuth when the phone is tilted vertically.
            val remappedRotationMatrix = FloatArray(9)
            SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, remappedRotationMatrix)

            // Get the orientation from the remapped matrix
            val orientationAngles = FloatArray(3)
            SensorManager.getOrientation(remappedRotationMatrix, orientationAngles)

            // The azimuth is the first value in the array.
            val azimuthInRadians = orientationAngles[0]
            val azimuthInDegrees = Math.toDegrees(azimuthInRadians.toDouble()).toFloat()

            // Normalize to 0-360 degrees
            val degrees = (azimuthInDegrees + 360) % 360

            _uiState.update { it.copy(liveDirection = degrees) }
        }
    }


    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this use case
    }

    // Clear sensor listeners when the ViewModel is cleared
    override fun onCleared() {
        super.onCleared()
        stopSensorUpdates()
    }
}
