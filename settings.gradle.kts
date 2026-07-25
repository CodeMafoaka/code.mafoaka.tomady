pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    // Include the React Native Gradle plugin from node_modules (RN 0.73+)
    val rnGradlePluginDir = file("demo/node_modules/@react-native/gradle-plugin")
    if (rnGradlePluginDir.exists()) {
        includeBuild(rnGradlePluginDir.path)
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        // Local React Native Android AAR published inside the NPM package
        maven { url = uri("$rootDir/demo/node_modules/react-native/android") }
        // Android JSC binaries needed by React Native
        maven { url = uri("$rootDir/demo/node_modules/jsc-android/dist") }
    }
}

rootProject.name = "tomady-nutrition"
include(":app")
