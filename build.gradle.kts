// Top-level build file where you can add configuration options common to all sub-projects/modules.

// Keep the metadata reader aligned with the Kotlin compiler used by the project.
// This prevents Hilt/Room KAPT from rejecting Kotlin metadata 2.3.0 as unsupported.
val kotlinVersion = libs.versions.kotlin.get()
subprojects {
    configurations.configureEach {
        resolutionStrategy.force("org.jetbrains.kotlin:kotlin-metadata-jvm:$kotlinVersion")
        // Pin the stdlib to the newest Kotlin-2.3-metadata build readable by the project's
        // compiler; some third-party POMs (e.g. markdown renderers) pin newer stdlib versions
        // whose 2.4 metadata the Kotlin 2.2 compiler cannot read.
        resolutionStrategy.force("org.jetbrains.kotlin:kotlin-stdlib:2.3.21")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.gms) apply false
}