package com.artificialinsightsllc.synopticnetwork.data.models

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

/**
 * A data class that wraps a [MapReport] and implements the [ClusterItem] interface.
 * This allows our custom weather report markers to be used with the Google Maps Android Utility
 * Library's marker clustering feature. It only holds minimal data ([MapReport]) for performance.
 * Full report details are fetched only when the marker/cluster is interacted with.
 *
 * @param report The [MapReport] instance this cluster item represents.
 */
data class ReportClusterItem(val report: MapReport) : ClusterItem {

    // The position of the marker on the map, required by ClusterItem.
    // We convert the GeoPoint from our MapReport model to a LatLng.
    override fun getPosition(): LatLng =
        report.location?.let { LatLng(it.latitude, it.longitude) } ?: LatLng(0.0, 0.0)

    // The title for the marker, often displayed when the marker is tapped.
    // For our case, we can use the report type.
    override fun getTitle(): String? = report.reportType

    // The snippet for the marker, providing a brief summary.
    // Since MapReport does not contain 'comments', we use 'reportType' for the snippet.
    override fun getSnippet(): String? = report.reportType // Using reportType as snippet for lightweight data

    // Required by ClusterItem, but not typically used for display in simple clustering.
    // You could return a unique ID from the report if needed for advanced click handling.
    override fun getZIndex(): Float? = null
}
