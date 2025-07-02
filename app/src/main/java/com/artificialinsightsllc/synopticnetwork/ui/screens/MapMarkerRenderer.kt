package com.artificialinsightsllc.synopticnetwork.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as GraphicsColor // Alias to avoid ambiguity with Compose Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface // Corrected: Added missing import for Typeface
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import coil.ImageLoader
import coil.request.ImageRequest
import com.artificialinsightsllc.synopticnetwork.R
import com.artificialinsightsllc.synopticnetwork.data.models.ForecastIcon
import com.artificialinsightsllc.synopticnetwork.data.models.StormCell
import com.artificialinsightsllc.synopticnetwork.data.models.getReportTypesWithEmojis
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Helper class responsible for creating and caching custom marker icons for the map.
 * Each icon combines a base weather pin with an emoji representing the report type,
 * and is rotated to show the direction the photo was taken.
 * Icons are cached to improve performance for repeated requests.
 *
 * @param context The Android Context, used for loading drawables.
 */
class MarkerIconFactory(private val context: Context) {
    private val iconCache = mutableMapOf<String, BitmapDescriptor>()
    private val imageLoader = ImageLoader.Builder(context).build()
    // Use MutableStateFlow to hold the loaded Bitmap and the URL it came from
    private val _stormAttributeBaseBitmapWithUrl = MutableStateFlow<Pair<Bitmap, String>?>(null)
    val stormAttributeBaseBitmapFlow = _stormAttributeBaseBitmapWithUrl.asStateFlow()

    // Define the size of individual icons within the sprite sheet
    private val ICON_WIDTH = 32
    private val ICON_HEIGHT = 32
    // Assuming a 4x4 grid for the sprite sheet based on common placefile icon sheets
    private val ICONS_PER_ROW = 4

    // NEW: Define the desired display size for the storm cell icons on the map
    private val STORM_ICON_DISPLAY_SIZE = 96 // pixels, for a 96x96 display size

    /**
     * Loads the base storm attribute icon from the given URL and caches it.
     * This is a suspend function that should be called from a coroutine scope.
     *
     * @param url The URL of the storm attribute icon. Can be null if no URL is available.
     */
    suspend fun loadStormAttributeIcon(url: String?) {
        // Only load if URL is not null AND (not already loaded OR a different URL is requested)
        if (url != null && _stormAttributeBaseBitmapWithUrl.value?.second != url) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .addHeader("User-Agent", "SynopticNetwork (rdspromo@gmail.com)")
                    .allowHardware(false)
                    .listener(
                        onSuccess = { _, result ->
                            val drawable = result.drawable
                            if (drawable != null) {
                                val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
                                val canvas = Canvas(bitmap)
                                drawable.setBounds(0, 0, canvas.width, drawable.intrinsicHeight) // Corrected: Used drawable.intrinsicHeight
                                drawable.draw(canvas)
                                // Store the URL along with the bitmap in the MutableStateFlow
                                _stormAttributeBaseBitmapWithUrl.value = Pair(bitmap, url)
                                Log.d("MarkerIconFactory", "Storm attribute icon loaded successfully from $url.")
                            } else {
                                Log.e("MarkerIconFactory", "Failed to load storm attribute icon: Drawable is null for $url.")
                                _stormAttributeBaseBitmapWithUrl.value = null
                            }
                        },
                        onError = { request, result ->
                            Log.e("MarkerIconFactory", "Error loading storm attribute icon from ${request.data}: ${result.throwable?.message}", result.throwable)
                            _stormAttributeBaseBitmapWithUrl.value = null
                        }
                    )
                    .build()
                imageLoader.execute(request)
            } catch (e: Exception) {
                Log.e("MarkerIconFactory", "Error initiating image load for $url: ${e.message}", e)
                _stormAttributeBaseBitmapWithUrl.value = null
            }
        } else if (url == null) {
            // If the URL is explicitly null, clear the cached bitmap
            _stormAttributeBaseBitmapWithUrl.value = null
        }
    }


    /**
     * Creates a custom marker icon.
     * The icon is a combination of a base weather pin with an emoji representing the report type,
     * and is rotated to show the direction the photo was taken.
     * Icons are cached to improve performance for repeated requests.
     *
     * @param report The MapReport containing details for icon creation (report type, direction).
     * @return A BitmapDescriptor ready to be used as a marker icon, or null if creation fails.
     */
    fun createMarkerIcon(report: com.artificialinsightsllc.synopticnetwork.data.models.MapReport): BitmapDescriptor? {
        // Create a unique cache key based on report type and direction for efficient caching.
        // We only care about the integer part of the direction for caching, as small float
        // differences shouldn't generate new bitmaps.
        val cacheKey = "report_${report.reportType}_${report.direction.toInt()}"

        // Check if the icon is already in the cache
        if (iconCache.containsKey(cacheKey)) {
            return iconCache[cacheKey]
        }

        // 1. Load the base weather pin drawable and convert it to a mutable bitmap.
        // The size (120x120) is chosen to provide enough space for the emoji.
        val baseBitmap = ContextCompat.getDrawable(context, R.drawable.weatherpin)?.let {
            val bmp = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            it.setBounds(0, 0, canvas.width, it.intrinsicHeight)
            it.draw(canvas)
            bmp
        } ?: return null // Return null if the base drawable cannot be loaded

        // 2. Rotate the base bitmap according to the report's direction.
        val rotatedBitmap = baseBitmap.rotate(report.direction)

        // 3. Get the emoji corresponding to the report type.
        // Falls back to a question mark emoji if the type is not found.
        val emoji = getReportTypesWithEmojis().find { it.first == report.reportType }?.second ?: "❓"

        // 4. Create a new mutable bitmap to draw the emoji on the rotated pin.
        val finalBitmap = rotatedBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(finalBitmap)
        val paint = Paint().apply {
            textSize = 60f // Set emoji size
            color = GraphicsColor.BLACK // Use GraphicsColor.BLACK
            textAlign = Paint.Align.CENTER // Center the text horizontally
        }

        // Calculate the center of the canvas
        val centerX = canvas.width / 2f
        val centerY = canvas.height / 2f

        // Calculate the opposite angle in radians
        val oppositeAngleInDegrees = (report.direction + 180) % 360
        val oppositeAngleInRadians = Math.toRadians(oppositeAngleInDegrees.toDouble()).toFloat()

        // Calculate the x and y offsets based on the opposite direction and 20dp distance
        // In Android Canvas: positive X is right, positive Y is down.
        // dx = distance * sin(angle)
        // dy = -distance * cos(angle)
        val offsetDistance = 20f // 20 dp equivalent in pixels for drawing
        val offsetX = (offsetDistance * Math.sin(oppositeAngleInRadians.toDouble())).toFloat()
        val offsetY = (-offsetDistance * Math.cos(oppositeAngleInRadians.toDouble())).toFloat()

        // Calculate the final emoji drawing position, adjusting for text baseline
        val finalEmojiX = centerX + offsetX
        // The `paint.descent() + paint.ascent()) / 2f` centers the text vertically around the given Y.
        // So, we add the calculated offsetY to the centerY, and then apply the text baseline correction.
        val finalEmojiY = centerY + offsetY - ((paint.descent() + paint.ascent()) / 2f)

        canvas.drawText(emoji, finalEmojiX, finalEmojiY, paint)

        // 5. Convert the final bitmap into a BitmapDescriptor for Google Maps.
        val descriptor = BitmapDescriptorFactory.fromBitmap(finalBitmap)

        // Cache the newly created descriptor
        iconCache[cacheKey] = descriptor
        return descriptor
    }

    /**
     * Creates a custom marker icon for a group of reports, displaying the count.
     *
     * @param count The number of reports in the group.
     * @return A BitmapDescriptor for the group marker.
     */
    fun createGroupMarkerIcon(count: Int): BitmapDescriptor? {
        val cacheKey = "group_$count"
        if (iconCache.containsKey(cacheKey)) {
            return iconCache[cacheKey]
        }

        // Define bitmap size for group marker (larger than individual for visibility)
        val size = 150 // pixels
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw a solid circle background
        val circlePaint = Paint().apply {
            color = GraphicsColor.BLUE // Group marker color
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, circlePaint)

        // Draw the count text
        val textPaint = Paint().apply {
            color = GraphicsColor.WHITE
            textSize = 70f // Adjust text size
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD // Corrected: Used Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        // Center the text vertically
        val xPos = canvas.width / 2f
        val yPos = (canvas.height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(count.toString(), xPos, yPos, textPaint)

        val descriptor = BitmapDescriptorFactory.fromBitmap(bitmap)
        iconCache[cacheKey] = descriptor
        return descriptor
    }

    /**
     * Creates a small blue circle icon for the center of a spread group.
     */
    fun createSpreadCenterIcon(): BitmapDescriptor? {
        val cacheKey = "spread_center"
        if (iconCache.containsKey(cacheKey)) {
            return iconCache[cacheKey]
        }

        val size = 40 // pixels (smaller size for the center marker)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint().apply {
            color = GraphicsColor.BLUE // Changed color to blue
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        val descriptor = BitmapDescriptorFactory.fromBitmap(bitmap)
        iconCache[cacheKey] = descriptor
        return descriptor
    }

    /**
     * Creates a custom marker icon for a storm cell, potentially with TVS/MESO indicators.
     * The base icon is loaded from the placefile's IconFile URL.
     *
     * @param stormCell The StormCell data.
     * @return A BitmapDescriptor for the storm cell marker.
     */
    @Composable
    fun createStormCellIcon(stormCell: StormCell): BitmapDescriptor? {
        // Observe the loaded stormAttributeBaseBitmap
        val currentBaseBitmapWithUrl by stormAttributeBaseBitmapFlow.collectAsState()
        val fullSpriteSheetBitmap = currentBaseBitmapWithUrl?.first // Get the Bitmap from the Pair

        // Corrected: Cache key now includes the URL hash and iconIndex to ensure new icon if base image or index changes
        val cacheKey = "storm_cell_${stormCell.hasTVS}_${stormCell.hasMeso}_${stormCell.iconIndex}_${fullSpriteSheetBitmap?.hashCode()}"
        if (iconCache.containsKey(cacheKey)) {
            return iconCache[cacheKey]
        }

        if (fullSpriteSheetBitmap == null) {
            Log.w("MarkerIconFactory", "Full sprite sheet bitmap is null, cannot create storm cell icon.")
            return null
        }

        // Calculate the source rectangle for cropping the individual icon from the sprite sheet
        val iconX = (stormCell.iconIndex % ICONS_PER_ROW) * ICON_WIDTH
        val iconY = (stormCell.iconIndex / ICONS_PER_ROW) * ICON_HEIGHT

        // Ensure the calculated region is within the bounds of the sprite sheet
        if (iconX + ICON_WIDTH > fullSpriteSheetBitmap.width || iconY + ICON_HEIGHT > fullSpriteSheetBitmap.height) {
            Log.e("MarkerIconFactory", "Icon index ${stormCell.iconIndex} is out of bounds for sprite sheet dimensions.")
            return null
        }

        // 1. Crop the individual icon from the full sprite sheet
        val croppedIconBitmap = Bitmap.createBitmap(
            fullSpriteSheetBitmap,
            iconX,
            iconY,
            ICON_WIDTH,
            ICON_HEIGHT
        )

        // 2. Scale the cropped icon to the desired display size
        val scaledIconBitmap = Bitmap.createScaledBitmap(
            croppedIconBitmap,
            STORM_ICON_DISPLAY_SIZE,
            STORM_ICON_DISPLAY_SIZE,
            true // Filter for smooth scaling
        ).copy(Bitmap.Config.ARGB_8888, true) // Ensure it's mutable for pixel manipulation

        // 3. Make black background transparent on the scaled bitmap
        for (x in 0 until scaledIconBitmap.width) {
            for (y in 0 until scaledIconBitmap.height) {
                val pixel = scaledIconBitmap.getPixel(x, y)
                // Check if the pixel is black or very close to black (e.g., for anti-aliasing)
                // You might need to fine-tune this threshold (e.g., check individual R, G, B values)
                if (GraphicsColor.red(pixel) < 20 && GraphicsColor.green(pixel) < 20 && GraphicsColor.blue(pixel) < 20) {
                    scaledIconBitmap.setPixel(x, y, GraphicsColor.TRANSPARENT)
                }
            }
        }

        val canvas = Canvas(scaledIconBitmap) // Draw on the scaled bitmap

        // 4. Draw TVS indicator if present (e.g., a red circle/dot)
        if (stormCell.hasTVS) {
            val paint = Paint().apply {
                color = GraphicsColor.RED
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            // Draw a small red circle at the top-right corner of the SCALED icon
            canvas.drawCircle(scaledIconBitmap.width * 0.8f, scaledIconBitmap.height * 0.2f, 10f, paint) // Increased size for visibility
        }

        // 5. Draw MESO indicator if present (e.g., a yellow circle/dot)
        if (stormCell.hasMeso) {
            val paint = Paint().apply {
                color = GraphicsColor.YELLOW
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            // Draw a small yellow circle at the top-left corner of the SCALED icon
            canvas.drawCircle(scaledIconBitmap.width * 0.2f, scaledIconBitmap.height * 0.2f, 10f, paint) // Increased size for visibility
        }

        val descriptor = BitmapDescriptorFactory.fromBitmap(scaledIconBitmap)
        iconCache[cacheKey] = descriptor
        return descriptor
    }

    /**
     * Creates a custom marker icon for a forecast position, with rotation.
     * This will be a small arrow or dot.
     *
     * @param forecastIcon The ForecastIcon data.
     * @return A BitmapDescriptor for the forecast icon.
     */
    @Composable
    fun createForecastIcon(forecastIcon: ForecastIcon): BitmapDescriptor? {
        val cacheKey = "forecast_icon_${forecastIcon.rotation.toInt()}"
        if (iconCache.containsKey(cacheKey)) {
            return iconCache[cacheKey]
        }

        val size = 60 // pixels for forecast icon (slightly larger for visibility)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw a small circle as the base
        val circlePaint = Paint().apply {
            color = GraphicsColor.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 3f, circlePaint)

        // Draw a small arrow indicating direction
        val arrowPaint = Paint().apply {
            color = GraphicsColor.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 5f
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        // Save canvas state before rotation
        canvas.save()
        // Rotate the canvas around its center
        canvas.rotate(forecastIcon.rotation, size / 2f, size / 2f)

        // Draw a simple arrow pointing "up" (which becomes the rotated direction)
        val arrowLength = size / 4f
        val centerX = size / 2f
        val centerY = size / 2f
        canvas.drawLine(centerX, centerY + arrowLength, centerX, centerY - arrowLength, arrowPaint)
        // Draw arrow head (simple V shape)
        canvas.drawLine(centerX, centerY - arrowLength, centerX - arrowLength / 3, centerY - arrowLength / 2, arrowPaint)
        canvas.drawLine(centerX, centerY - arrowLength, centerX + arrowLength / 3, centerY - arrowLength / 2, arrowPaint)

        // Restore canvas state
        canvas.restore()

        val descriptor = BitmapDescriptorFactory.fromBitmap(bitmap)
        iconCache[cacheKey] = descriptor
        return descriptor
    }


    /**
     * Rotates a given Bitmap by a specified number of degrees.
     *
     * @param degrees The rotation angle in degrees.
     * @return A new Bitmap rotated by the specified degrees.
     */
    private fun Bitmap.rotate(degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }
}
