
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.dwellping"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.dwellping.open"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

    }

    buildTypes {
        release {
            // R8 drops unused code (most of material-icons-extended) and unused resources.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // TODO(#18): a real release key before the first public APK.
            signingConfig = signingConfigs.getByName("debug")
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
