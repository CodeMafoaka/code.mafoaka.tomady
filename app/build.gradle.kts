plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.tomady.nutrition"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.tomady.nutrition"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("int", "SERVICE_API_PORT", project.findProperty("serviceApiPort")?.toString() ?: "7777")
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("services") {
            dimension = "distribution"
            applicationIdSuffix = ".services"
            versionNameSuffix = "-services"
        }
        create("demo") {
            dimension = "distribution"
            applicationIdSuffix = ".demo"
            versionNameSuffix = "-demo"
        }
        create("full") {
            dimension = "distribution"
            versionNameSuffix = "-full"
        }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    // Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Lifecycle / ViewModel
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // JSON
    implementation("com.google.code.gson:gson:2.10.1")

    // AppCompat (for DemoActivity)
    implementation("androidx.appcompat:appcompat:1.6.1")

    // NanoHTTPd — embedded HTTP server for local-network REST API
    implementation("org.nanohttpd:nanohttpd:2.3.1")

    // MediaPipe LLM Inference — Gemma 4 on-device
    implementation("com.google.mediapipe:tasks-genai:0.10.14")

    // React Native bridge (provided by host app at runtime)
    compileOnly("com.facebook.react:react-android:0.73.0")
}
