import java.util.Properties

// Machine-only settings (never committed): SDK path and the release signing key.
val local = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "io.github.iamanthonycruz.aqui"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.iamanthonycruz.aqui"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

    }

    // Release key: set RELEASE_STORE_FILE, RELEASE_STORE_PASSWORD, RELEASE_KEY_ALIAS and
    // RELEASE_KEY_PASSWORD in local.properties. Without them, release builds use the debug key
    // (fine for trying it out, but such an APK can't update one signed with the real key).
    val releaseStore = local.getProperty("RELEASE_STORE_FILE")?.let { file(it) }?.takeIf { it.exists() }
    signingConfigs {
        if (releaseStore != null) create("release") {
            storeFile = releaseStore
            storePassword = local.getProperty("RELEASE_STORE_PASSWORD")
            keyAlias = local.getProperty("RELEASE_KEY_ALIAS")
            keyPassword = local.getProperty("RELEASE_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            // R8 drops unused code (most of material-icons-extended) and unused resources.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName(if (releaseStore != null) "release" else "debug")
            // Phones are ARM: leave out x86 native code (emulator-only; debug builds keep it).
            ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
        }
    }

    packaging {
        // Compress native libraries in the APK: a much smaller download for a sideloaded app,
        // at the cost of unpacking them once on install.
        jniLibs { useLegacyPackaging = true }
    }

    lint {
        // AGP 8.7's lint crashes on Android Studio's JDK 25 ("IllegalArgumentException: 25.0.4"),
        // which blocks every release build. Turn back on after upgrading AGP.
        checkReleaseBuilds = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.activity:activity-compose:1.9.3")

    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    // Rounded icons for the forms (timer, chat, map, contacts...). Unused ones are stripped from release builds.
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    val room = "2.6.1"
    implementation("androidx.room:room-runtime:$room")
    implementation("androidx.room:room-ktx:$room")
    ksp("androidx.room:room-compiler:$room")

    testImplementation("junit:junit:4.13.2")
}
