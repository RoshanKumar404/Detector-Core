plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val debugApiBaseUrl = providers.gradleProperty("API_BASE_URL")
    .orElse("http://10.0.2.2:1000/api/")
    .get()

android {
    namespace = "com.example.detector"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.detector"
        minSdk = 31
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.01"


        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }
    splits {
        abi {
            isEnable = true
            reset()

            include(
                "armeabi-v7a",
                "arm64-v8a"
            )

            isUniversalApk = false
        }
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"$debugApiBaseUrl\""
            )
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"https://detectorbackend.onrender.com/api/\""
            )

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility =
            JavaVersion.VERSION_17

        targetCompatibility =
            JavaVersion.VERSION_17
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

    implementation("androidx.core:core-ktx:1.16.0")

    implementation(
        "androidx.lifecycle:lifecycle-runtime-ktx:2.9.1"
    )

    implementation(
        "androidx.activity:activity-compose:1.10.1"
    )

    implementation(
        platform(
            "androidx.compose:compose-bom:2025.06.00"
        )
    )

    implementation("androidx.compose.ui:ui")

    implementation(
        "androidx.compose.ui:ui-graphics"
    )

    implementation(
        "androidx.compose.ui:ui-tooling-preview"
    )

    implementation(
        "androidx.compose.material3:material3"
    )

    implementation("androidx.compose.material:material")

    // Retrofit

    implementation(
        "com.squareup.retrofit2:retrofit:2.11.0"
    )

    implementation(
        "com.squareup.retrofit2:converter-gson:2.11.0"
    )

    implementation(
        "com.squareup.okhttp3:logging-interceptor:4.12.0"
    )

    // Navigation

    implementation(
        "androidx.navigation:navigation-compose:2.9.0"
    )

    // Coil

    implementation(
        "io.coil-kt:coil-compose:2.7.0"
    )

    // CameraX

    implementation(
        "androidx.camera:camera-core:1.4.2"
    )

    implementation(
        "androidx.camera:camera-camera2:1.4.2"
    )

    implementation(
        "androidx.camera:camera-lifecycle:1.4.2"
    )

    implementation(
        "androidx.camera:camera-view:1.4.2"
    )

    implementation(
        "com.google.android.gms:play-services-location:21.3.0"
    )

    implementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3"
    )

    // OsmDroid for native OpenStreetMap
    implementation("org.osmdroid:osmdroid-android:6.1.20")

    // Testing

    testImplementation("junit:junit:4.13.2")

    androidTestImplementation(
        "androidx.junit:junit:1.2.1"
    )

    androidTestImplementation(
        "androidx.espresso:espresso-core:3.6.1"
    )

    androidTestImplementation(
        platform(
            "androidx.compose:compose-bom:2025.06.00"
        )
    )

    androidTestImplementation(
        "androidx.compose.ui:ui-test-junit4"
    )

    debugImplementation(
        "androidx.compose.ui:ui-tooling"
    )

    debugImplementation(
        "androidx.compose.ui:ui-test-manifest"
    )
}
