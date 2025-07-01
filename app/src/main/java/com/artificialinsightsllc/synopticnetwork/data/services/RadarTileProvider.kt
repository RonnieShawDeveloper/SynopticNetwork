package com.artificialinsightsllc.synopticnetwork.data.services

import android.graphics.BitmapFactory
import android.util.Log
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.log
import kotlin.math.atan
import kotlin.math.exp

/**
 * A custom TileProvider to fetch and display WMS radar data on a Google Map.
 * This provider constructs WMS GetMap requests for each tile based on the Google Maps
 * tile coordinates (x, y, zoom) and the specified WFO.
 *
 * @param officeCode The 3-letter WFO identifier (e.g., "TBW") for the radar station.
 * @param httpClient An OkHttpClient instance for making network requests.
 * @param latestRadarTimestamp The latest available ISO 8601 timestamp for the radar data,
 * fetched from the GetCapabilities response.
 */
class RadarTileProvider(
    private val officeCode: String,
    private val httpClient: OkHttpClient,
    private val latestRadarTimestamp: String? // NEW: Accept latest timestamp
) : TileProvider {

    // Constants for WMS GetMap request
    private val TILE_SIZE = 256 // Google Maps tile size in pixels
    private val WMS_LAYER_NAME = "${officeCode.lowercase()}_sr_bref" // Using Super Resolution Base Reflectivity
    private val WMS_STYLE = "radar_reflectivity" // Style name from GetCapabilities
    private val WMS_CRS = "EPSG:3857" // Web Mercator, compatible with Google Maps
    private val WMS_VERSION = "1.3.0"
    private val WMS_FORMAT = "image/png"
    private val WMS_TRANSPARENT = "TRUE"
    private val WMS_SERVICE = "WMS"
    private val WMS_REQUEST = "GetMap"

    // Base URL for the NCEP GeoServer WMS service, dynamically constructed with WFO
    private val BASE_WMS_URL = "https://opengeo.ncep.noaa.gov/geoserver/${officeCode.lowercase()}/ows?"

    // User-Agent header required by NWS
    private val USER_AGENT = "SynopticNetwork (rdspromo@gmail.com)"

    /**
     * Returns the tile image for the given tile coordinates and zoom level.
     * This method is called by the Google Map SDK for each visible tile.
     *
     * @param x The x-coordinate of the tile.
     * @param y The y-coordinate of the tile.
     * @param zoom The zoom level of the tile.
     * @return A Tile object containing the image bytes, or TileProvider.NO_TILE if the tile cannot be loaded.
     */
    override fun getTile(x: Int, y: Int, zoom: Int): Tile {
        // Calculate the bounding box for the current Google Maps tile in EPSG:3857 (Web Mercator)
        val bbox = tile2boundingBox(x, y, zoom)

        // Construct the WMS GetMap URL
        val wmsUrl = buildWmsUrl(bbox)

        // NEW: Log the full WMS URL for debugging
        Log.d("RadarTileProvider", "Fetching radar tile URL: $wmsUrl")

        // Fetch the image bytes
        val imageBytes = fetchImageBytes(wmsUrl)

        return if (imageBytes != null) {
            Tile(TILE_SIZE, TILE_SIZE, imageBytes)
        } else {
            TileProvider.NO_TILE // Return NO_TILE if fetching fails
        }
    }

    /**
     * Builds the complete WMS GetMap URL using the provided bounding box.
     */
    private fun buildWmsUrl(bbox: BoundingBox): String {
        // Build the base URL
        var url = "${BASE_WMS_URL}" +
                "service=$WMS_SERVICE&version=$WMS_VERSION&request=$WMS_REQUEST&" +
                "layers=$WMS_LAYER_NAME&styles=$WMS_STYLE&crs=$WMS_CRS&" +
                "bbox=${bbox.minX},${bbox.minY},${bbox.maxX},${bbox.maxY}&" +
                "width=$TILE_SIZE&height=$TILE_SIZE&format=$WMS_FORMAT&transparent=$WMS_TRANSPARENT"

        // Conditionally add the 'time' parameter if a valid timestamp is provided
        if (!latestRadarTimestamp.isNullOrBlank()) {
            url += "&time=$latestRadarTimestamp"
        }
        return url
    }

    /**
     * Fetches image bytes from the given URL using OkHttpClient.
     */
    private fun fetchImageBytes(url: String): ByteArray? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT) // NWS requires a User-Agent
            .build()

        return try {
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.bytes()
            } else {
                Log.e("RadarTileProvider", "Failed to fetch radar tile: ${response.code} - ${response.message}")
                null
            }
        } catch (e: IOException) {
            Log.e("RadarTileProvider", "Network error fetching radar tile: ${e.message}", e)
            null
        }
    }

    /**
     * Calculates the Web Mercator bounding box (EPSG:3857) for a given Google Maps tile.
     * This is a standard conversion for Google Maps tile coordinates.
     *
     * @param x The tile's x-coordinate.
     * @param y The tile's y-coordinate.
     * @param zoom The tile's zoom level.
     * @return A BoundingBox object in Web Mercator coordinates.
     */
    private fun tile2boundingBox(x: Int, y: Int, zoom: Int): BoundingBox {
        val n = 2.0.pow(zoom.toDouble())
        val lon1 = x / n * 360.0 - 180.0
        val lon2 = (x + 1) / n * 360.0 - 180.0
        val lat1 = atan(0.5 * (exp(Math.PI * (1 - 2 * y / n)) - exp(-Math.PI * (1 - 2 * y / n)))) * 180.0 / Math.PI
        val lat2 = atan(0.5 * (exp(Math.PI * (1 - 2 * (y + 1) / n)) - exp(-Math.PI * (1 - 2 * (y + 1) / n)))) * 180.0 / Math.PI

        // Convert WGS84 (lat/lon) to Web Mercator (EPSG:3857)
        val earthRadius = 6378137.0 // meters
        val minX = lon1 * earthRadius * Math.PI / 180.0
        val maxX = lon2 * earthRadius * Math.PI / 180.0
        // Corrected: Use Math.E as the base for the natural logarithm
        val minY = log(
            Math.tan((90 + lat2) * Math.PI / 360.0),
            base = Math.E
        ) * earthRadius
        val maxY = log(
            Math.tan((90 + lat1) * Math.PI / 360.0),
            base = Math.E
        ) * earthRadius

        return BoundingBox(minX, minY, maxX, maxY)
    }

    /**
     * This function is no longer directly used in buildWmsUrl, but kept for reference if needed elsewhere.
     */
    private fun getCurrentIsoTime(): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(System.currentTimeMillis())
    }

    /**
     * Data class to hold bounding box coordinates in Web Mercator.
     */
    data class BoundingBox(val minX: Double, val minY: Double, val maxX: Double, val maxY: Double)
}
