import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // Apply the Compose Compiler plugin here as required
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
    // Add the Kotlinx Serialization plugin
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
}

// Load the local.properties file to access the API key securely
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.artificialinsightsllc.synopticnetwork"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.artificialinsightsllc.synopticnetwork"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Get the key and trim any quotes it might have.
        val apiKey = localProperties.getProperty("GOOGLE_MAPS_API_KEY", "").trim('"')

        // Create a string resource for the AndroidManifest.xml
        resValue("string", "google_maps_key", apiKey)

        // Create a BuildConfig field to make the key available in the app's code.
        // The key is wrapped in quotes to be treated as a String literal.
        buildConfigField("String", "GOOGLE_MAPS_API_KEY", "\"$apiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        // Updated to a modern Java version to resolve deprecation warnings
        sourceCompatibility = JavaVersion.VERSION_18
        targetCompatibility = JavaVersion.VERSION_18
    }
    kotlinOptions {
        // Aligned the Kotlin JVM target with the updated Java version
        jvmTarget = "18"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")


    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.1")
    implementation("androidx.activity:activity-compose:1.9.0")


    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")


    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-config")


    implementation("io.coil-kt:coil-compose:2.6.0")


    val cameraxVersion = "1.4.2"
    implementation("androidx.camera:camera-core:${cameraxVersion}")
    implementation("androidx.camera:camera-camera2:${cameraxVersion}")
    implementation("androidx.camera:camera-lifecycle:${cameraxVersion}")
    implementation("androidx.camera:camera-view:${cameraxVersion}")


    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.maps.android:maps-compose:4.4.1")
    implementation("com.google.maps.android:android-maps-utils:3.8.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.maps.android:maps-compose-utils:4.4.1")

    // CORRECTED: Use the GitHub-based coordinates for JitPack
    implementation("com.github.kungfoo:geohash-java:1.4.0")

    implementation("com.github.yalantis:ucrop:2.2.8")

    // Retrofit for networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // OkHttp Logging Interceptor for debugging network calls
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    // Kotlinx Serialization Converter for Retrofit
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")
    // Kotlinx Serialization library
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")


    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
