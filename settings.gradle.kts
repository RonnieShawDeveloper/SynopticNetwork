// ronnieshawdeveloper/synopticnetwork/SynopticNetwork-88da7de6be3d676fbdf74a8471c15bf12098783e/settings.gradle.kts
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Ensure the JitPack repository is included here for libraries like GeoHash
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Synoptic Network"
include(":app")

