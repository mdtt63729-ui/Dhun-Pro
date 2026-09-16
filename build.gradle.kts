buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Explicit classpath keeps Hilt's Gradle bytecode transformation active on AGP 9.x.
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.60.1")
    }
}

// Top-level build file where you can add configuration options common to all sub-projects/modules.

// Keep the metadata reader aligned with the Kotlin compiler used by the project.
// This prevents Hilt/Room KAPT from rejecting Kotlin metadata 2.3.0 as unsupported.
val kotlinVersion = libs.versions.kotlin.get()
subprojects {
    configurations.configureEach {
        resolutionStrategy.force("org.jetbrains.kotlin:kotlin-metadata-jvm:$kotlinVersion")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.room.plugin) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.gms) apply false
}