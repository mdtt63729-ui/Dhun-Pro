plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Apply Hilt explicitly so AGP 9 + KAPT registers Hilt's generated component metadata.
    alias(libs.plugins.hilt)
    id("org.jetbrains.kotlin.kapt")
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.compose.compiler)

    alias(libs.plugins.gms) apply false
    alias(libs.plugins.crashlytics) apply false
}

val hasGoogleServices = file("google-services.json").exists()
val gitHash = execute("git", "rev-parse", "HEAD").take(7)
val gitCount = execute("git", "rev-list", "--count", "HEAD").toInt()
val version = "3.0.$gitCount"

android {
    namespace = "dev.brahmkshatriya.echo"
    compileSdk = 37

    packaging {
        resources {
            excludes += setOf(
                "META-INF/CONTRIBUTORS.md",
                "META-INF/CONTRIBUTORS",
                "META-INF/LICENSE.md",
                "META-INF/LICENSE",
                "META-INF/NOTICE.md",
                "META-INF/NOTICE",
                "META-INF/DEPENDENCIES",
            )
        }
    }

    defaultConfig {
        applicationId = "dev.brahmkshatriya.echo"
        minSdk = 24
        targetSdk = 36
        versionCode = gitCount
        versionName = "v${version}_$gitHash($gitCount)"

        // Last.fm API credentials (scrobbling). Override via gradle properties if needed.
        buildConfigField("String", "LASTFM_API_KEY", "\"${project.findProperty("LASTFM_API_KEY") ?: ""}\"")
        buildConfigField("String", "LASTFM_SECRET", "\"${project.findProperty("LASTFM_SECRET") ?: ""}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
            )
        }
        create("nightly") {
            initWith(getByName("release"))
            applicationIdSuffix = ".nightly"
            resValue("string", "app_name", "Echo Nightly")
        }
        create("stable") {
            initWith(getByName("release"))
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
        resValues = true
    }

    androidResources {
        @Suppress("UnstableApiUsage")
        generateLocaleConfig = true
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    // Backdrop 1.0.1 is distributed with Java 21 bytecode (class version 65).
    // Keep Android/JVM output compatible with Java 17 while using JDK 21 to run KAPT.
    jvmToolchain(21)
}

kapt {
    correctErrorTypes = true
}

dependencies {
    implementation(project(":common"))
    implementation(libs.kotlin.reflect)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.bundles.androidx)
    implementation(libs.material)
    implementation(libs.bundles.paging)
    implementation(libs.filekache)
    implementation(libs.bundles.room)
    kapt(libs.room.compiler)
    implementation(libs.bundles.koin)
    implementation(libs.bundles.media3)
    implementation(libs.bundles.coil)
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.kyant.backdrop)

    // Dhun: additional dependencies
    implementation(libs.datastore.preferences)
    implementation(libs.coil.compose)
    implementation(libs.window.core)
    implementation(libs.compose.material3.adaptive)
    implementation(libs.jsoup)
    implementation(libs.kuromoji.ipadic)
    implementation(libs.reorderable)
    implementation(libs.squigglyslider)
    implementation(libs.cloudy)
    implementation(libs.markdown.m3)
    implementation(libs.compose.markdown)
    implementation(libs.translator)
    implementation(libs.m3color)
    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.media3.datasource.okhttp)

    implementation(libs.pikolo)
    implementation(libs.fadingedgelayout)
    implementation(libs.fastscroll)
    implementation(libs.kenburnsview)
    implementation(libs.nestedscrollwebview)
    implementation(libs.acsbendi.webview)
    implementation(libs.commons.lang3)
    implementation(libs.bundles.lyrics.network)
    // NewPipeExtractor is published by TeamNewPipe on JitPack using the v-prefixed tag.
    implementation(libs.newpipe.extractor)

    if (!hasGoogleServices) return@dependencies
    implementation(libs.bundles.firebase)
}

if (hasGoogleServices) {
    apply(plugin = libs.plugins.gms.get().pluginId)
    apply(plugin = libs.plugins.crashlytics.get().pluginId)
}

fun execute(vararg command: String): String = providers.exec {
    commandLine(*command)
}.standardOutput.asText.get().trim()