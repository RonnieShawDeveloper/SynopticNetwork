Synoptic Network Project Summary
The "Synoptic Network" is an Android application designed to facilitate real-time weather reporting and access to National Weather Service (NWS) data. The app allows users to submit weather reports with location, direction, comments, and photos, and it also displays active NWS alerts and weather products.

Core Functionality
User Authentication and Profiles: The application supports user registration, login, and password reset functionalities using Firebase Authentication. Users can manage their profiles, including screen name, zip code, NWS spotter ID, and ham radio call sign, which are stored in Firestore.

Weather Report Submission: Users can create and submit detailed weather reports, including an image captured via the device's camera, geographic location (latitude/longitude), compass direction, report type (e.g., Tornado, Hail, Flooding), and optional comments and phone number. Reports can optionally be sent to the NWS.

Interactive Map Display: The main screen features a Google Map that displays user-submitted weather reports as markers. Markers are dynamically grouped or spread based on zoom level to manage visual clutter.

NWS Alert Integration: The app fetches and displays active NWS severe weather alerts as polygons on the map, with varying colors based on severity. It highlights local alerts and provides details such as expiration time.

NWS Weather Products: Users can browse and view detailed text content of various NWS weather products (e.g., Area Forecast Discussions, Hazardous Weather Outlooks) specific to their Weather Forecast Office (WFO).

Radar Overlays: The application supports displaying reflectivity and velocity radar overlays on the map, dynamically fetching data from NCEP GeoServer.

Key Technologies Used
Android Development: Kotlin, Jetpack Compose for UI, Android SDK 34.

Mapping: Google Maps Android SDK, Google Maps Compose library for map integration, and Google Maps Android Utility Library for marker clustering.

Backend & Data Storage: Firebase (Authentication, Firestore for user profiles and reports, Storage for images, Remote Config for dynamic API keys).

Networking: Retrofit and OkHttp for consuming RESTful APIs, with Kotlinx Serialization for JSON parsing.

NWS Data Integration: Utilizes the National Weather Service API for fetching point data, active alerts, and weather products. Google Geocoding API is used for zip code to coordinates conversion.

Location & Sensors: Fused Location Provider for accurate location and device sensors (rotation vector) for compass direction.

Image Handling: CameraX for camera functionality and uCrop for image cropping.

Spatial Indexing: GeoHash-Java library for efficient spatial querying of reports.

The application's architecture follows the Model-View-ViewModel (MVVM) pattern, using Jetpack Compose for declarative UI. It prioritizes a light-theme only design with a custom color palette inspired by NOAA/NWS branding.