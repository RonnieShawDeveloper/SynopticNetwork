// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // These aliases point to your project's version catalog (e.g., libs.versions.toml)
    // This is the correct way to apply plugins in your setup.
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false // This correctly applies the Compose Compiler plugin

    // The Google Services plugin is also declared here.
    id("com.google.gms.google-services") version "4.4.2" apply false
}
