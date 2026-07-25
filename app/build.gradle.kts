plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    // React Native Gradle plugin (manages RN AAR dependency & repositories)
    id("com.facebook.react") version "0.73.6"
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

    // React Native bridge — the com.facebook.react plugin adds this as an
    // implementation dependency (bundled in the APK). For the services flavor
    // this is harmless: the bridge classes exist but are never initialized.
    // The plugin also handles Maven repo resolution from node_modules.

    // JSON
    implementation("com.google.code.gson:gson:2.10.1")
}
